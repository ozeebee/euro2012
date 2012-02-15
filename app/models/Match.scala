package models

import play.api.db.DB
import play.api.Play.current

import anorm._
import anorm.SqlParser._

import models.Phase.Phase
import models.MatchOutcome.MatchOutcome

case class Match(
	id: Pk[Long],
	teamA: Option[String],
	teamAformula: Option[String],
	teamB: Option[String],
	teamBformula: Option[String],
	kickoff: java.util.Date,
	phase: Phase, // example: round1, quarter finals, semi final, etc
	// stadium ?
	result: Option[Result]
) {
	val played: Boolean = result.isDefined
	val isFormula: Boolean = teamAformula.isDefined

	/**
	 * @return the group for which this match is played if this is a group match
	 */
	def group(): Option[String] = {
		teamA flatMap { teamId =>
			val team = Team.getTeam(teamId)
			team.map(_.group)
		}
	}
	
	/**
	 * @return either the team id or the formula if the team id is not yet known (computed)
	 */
	def teamAorFormula: String = {
		teamA.getOrElse(teamAformula.get)
	}
	def teamBorFormula: String = {
		teamB.getOrElse(teamBformula.get)
	}
	
	/**
	 * @return the match kickoff day without time information
	 */
	def kickoffDay: java.util.Date = {
		val cal = java.util.Calendar.getInstance()
		cal.setTime(kickoff)
		cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
		cal.set(java.util.Calendar.MINUTE, 0)
		cal.set(java.util.Calendar.SECOND, 0)
		cal.set(java.util.Calendar.MILLISECOND, 0)
		cal.getTime()
	}
	
	/**
	 * @return the winner (team id) or None it's a draw OR if no result yet
	 */
	def winner: Option[String] = {
		if (result.isEmpty)
			Option(null)
		else
			result.get.winner
	}
	
	/**
	 * @return true if team A is the winner, false if the result is a draw of if the match has not yet been played
	 */
	def isTeamAWinner: Boolean = {
		result.map(_.outcome == MatchOutcome.TEAM_A).getOrElse(false)
	}
	
	/**
	 * @return true if team B is the winner, false if the result is a draw of if the match has not yet been played
	 */
	def isTeamBWinner: Boolean = {
		result.map(_.outcome == MatchOutcome.TEAM_B).getOrElse(false)
	}

	/**
	 * @return true wether this match concerns both supplied teams (in any order) 
	 */
	def concernTeams(teamX: String, teamY: String): Boolean = {
		(teamA == teamX && teamB == teamY) || (teamA == teamY && teamB == teamX)
	}
}

case class Result(
	outcome: MatchOutcome,
	winner: Option[String],
	scoreA: Int,
	scoreB: Int
)

object Match {
	
	// Parser
	val simple = {
		get[Pk[Long]]("match.id") ~
		get[Option[String]]("match.teamA") ~
		get[Option[String]]("match.teamAformula") ~
		get[Option[String]]("match.teamB") ~
		get[Option[String]]("match.teamBformula") ~
		get[java.util.Date]("match.kickoff") ~
		get[String]("match.phase") ~
		get[Option[String]]("match.result") ~
		get[Option[Int]]("match.scoreA") ~
		get[Option[Int]]("match.scoreB") map {
			case id~teamA~teamAformula~teamB~teamBformula~kickoff~phase~result~scoreA~scoreB => {
				result.map { result => // Match with Result
					assert(scoreA.isDefined && scoreB.isDefined, "if there is a result, scores must exist")
					Match(id, teamA, teamAformula, teamB, teamBformula, kickoff, Phase.withName(phase), result match {
						case "DRAW" => Some(Result(MatchOutcome.DRAW, None, scoreA.get, scoreB.get)) 
						case _ => {
							if (result == teamA.get) Some(Result(MatchOutcome.TEAM_A, Some(result), scoreA.get, scoreB.get))
							else if (result == teamB.get) Some(Result(MatchOutcome.TEAM_B, Some(result), scoreA.get, scoreB.get))
							else throw new IllegalArgumentException("result for matchId "+id+" does not match concerned team !")
						}
					}) 
				}.getOrElse { // Match without Result
					Match(id, teamA, teamAformula, teamB, teamBformula, kickoff, Phase.withName(phase), None)
				}
			}
		}
	}
	
	def findAll(): Seq[Match] = {
		DB.withConnection { implicit connection =>
			SQL("select * from match order by kickoff").as(Match.simple *)
		}
	}
	
	def findById(id: Long): Option[Match] = {
		DB.withConnection { implicit connection =>
			SQL("select * from match where id={id}").on('id -> id).as(Match.simple.singleOpt)
		}
	}
	
	def findByGroup(group: String): Seq[Match] = {
		DB.withConnection { implicit connection =>
			SQL("""
					select * from match 
					where teamA is not null and teamB is not null 
					  and teamA in (select id from team where "group" = {group}) 
					  and teamB in (select id from team where "group" = {group}) 
					order by kickoff, id
			""").on('group -> group)
				.as(Match.simple *)
		}
	}
	
	def findPlayed(): Seq[Match] = {
		DB.withConnection { implicit connection =>
			SQL("select * from match where scoreA is not null").as(Match.simple *)
			// assuming scoreA and scoreB are both set
		}
	}
	
	def create(zmatch: Match): Match = {
		DB.withConnection { implicit connection =>
			// generated identifier
			val id: Long =  SQL("select next value for match_seq").as(scalar[Long].single)
			
			SQL("""
				insert into match (id, teamA, teamB, kickoff, phase) 
				values ({id}, {teamA}, {teamB}, {kickoff}, {phase})
			""").on(
				'id -> id,
				'teamA -> zmatch.teamA,
				'teamB -> zmatch.teamB,
// XXX : NO TIMESTAMP SUPPORT !!! ==> this should have been fixed in play2.0, to be re-tested
				'kickoff -> new java.sql.Timestamp(zmatch.kickoff.getTime()),
				'phase -> zmatch.phase.toString()
			).executeUpdate()
			
			zmatch.copy(id = Id(id))
		}
	}
	
	def updateResult(matchId: Long, scoreA: Int, scoreB: Int): Option[Match] = {
		findById(matchId).map { zmatch =>
			assert(zmatch.teamA.isDefined && zmatch.teamB.isDefined, "at this step the teams should be known (computed) !")
			val result = (scoreA, scoreB) match {
				case (x, y) if (x > y) => zmatch.teamA.get // teamA wins
				case (x, y) if (x < y) => zmatch.teamB.get // teamB wins
				case _ => "DRAW"
			}
			
			DB.withConnection { implicit connection =>
				SQL("""
					update match 
					set result = {result}, scoreA = {scoreA}, scoreB = {scoreB}
					where id = {id}
				""").on(
					'id -> matchId,
					'result -> result,
					'scoreA -> scoreA,
					'scoreB -> scoreB
				).executeUpdate()
			}
			
			// set result in match
			val newResult = result match {
				case "DRAW" => Some(Result(MatchOutcome.DRAW, None, scoreA, scoreB))
				case teamId if (teamId == zmatch.teamA.get) => Some(Result(MatchOutcome.TEAM_A, Some(teamId), scoreA, scoreB))
				case teamId if (teamId == zmatch.teamB.get) => Some(Result(MatchOutcome.TEAM_B, Some(teamId), scoreA, scoreB))
			}
			zmatch.copy(result = newResult)
		}
	}
	
	def clearResult(matchId: Long) = {
		DB.withConnection { implicit connection =>
			SQL("""
				update match 
				set result = null, scoreA = null, scoreB = null
				where id = {id}
			""").on('id -> matchId).executeUpdate()
		}
	}
	
	/**
	 * compute standings for all groups
	 */
	def computeStandings(matches: Seq[Match]): Map[String, Seq[Standing]] = {
		// this generate a collection of tuples(2) that can be used to create a map
		val tuples = for (group <- Team.getGroups()) yield group -> computeStandings(group, matches)
		Map(tuples.toSeq: _*)
	}
	
	/**
	 * compute standings for given group
	 */
	def computeStandings(group: String, matches: Seq[Match]): Seq[Standing] = {
		// first, get all matches in group
		val matchesInGroup: Seq[Match] = matches filter (zmatch => zmatch.group().isDefined && zmatch.group().get == group)
println("matches in group " + group + " = " + matchesInGroup)
		// compute standings
		//   we start with an empty map being filled in with team standings as we process matches one by one
		val standingsMap = matchesInGroup.foldLeft(collection.mutable.Map[String, Standing]()) { (map: collection.mutable.Map[String, Standing], zmatch: Match) =>
			// get current standing or create it if it doesnt exist
			val standingA = map.get(zmatch.teamA.get).getOrElse { 
				val standing = Standing.Empty(zmatch.teamA.get)
				map(zmatch.teamA.get) = standing
				standing
			}
			val standingB = map.get(zmatch.teamB.get).getOrElse {
				val standing = Standing.Empty(zmatch.teamB.get)
				map(zmatch.teamB.get) = standing
				standing
			}
			// update standing with match result
			standingA.update(zmatch)
			standingB.update(zmatch)
			
			map
		}
		
println("standingsMap = " + standingsMap)

		val standings = standingsMap.values.toSeq
		// compute positions
		standings.sortWith { (standingA: Standing, standingB: Standing) => 
			if (standingA.points > standingB.points) {
				true
			} else if (standingA.points < standingB.points) {
				false
			} 
			else {
				// same number of points, check goal difference in matches between teams
				val goalDiffA = getGoalDiffForTeamAgainst(standingA.team, standingB.team, matchesInGroup)
				// if it's positive, it means teamA has won thus teamA is before teamB
				if (goalDiffA > 0)
					true
				else if (goalDiffA < 0)
					false
				else {
					// goalDiff is zero, check global goal diff in matches between teams
					if (standingA.goalDiff > standingB.goalDiff)
						true
					else if (standingA.goalDiff < standingB.goalDiff)
						false
					else {
						// same goalDiff, check goals scored
						if (standingA.goalsScored > standingB.goalsScored)
							true
						else if (standingA.goalsScored < standingB.goalsScored)
							false
						else {
							// same goalsScored, check what now ?? UEFA coefficients ... 
							println("!!! cannot determine winner ")
							// XXX: ok teamA wins then ...
							true
						}
					}
				}
			}
		}
	}
	
	lazy val randomScoreRanges = Array(
			0  until 30, // score 0
			30 until 60, // score 1
			60 until 80, // score 2
			80 until 90, // score 3
			90 until 95, // score 4
			95 until 98, // score 5
			98 until 100 // score 6
		) 
	
	def generateRandomScore(): Int = {
		val randInt = util.Random.nextInt(randomScoreRanges.last.end)
println("  randInt = " + randInt)
		val index = randomScoreRanges.indexWhere(_.contains(randInt))
		// index is the score
		index
	}
	
	def generateRandomResult(): (Int, Int) = {
		(generateRandomScore(), generateRandomScore())
	}
	
	// inner function
	/**
	 * Compute goal difference for given team against otherTeam
	 */
	private def getGoalDiffForTeamAgainst(teamId: String, otherTeamId:String, matches: Seq[Match]): Int = {
		matches.foldLeft(0) { (goalDiff: Int, zmatch: Match) =>
			if (zmatch.played && zmatch.concernTeams(teamId, otherTeamId)) {
				if (zmatch.teamA == teamId)
					goalDiff + zmatch.result.get.scoreA - zmatch.result.get.scoreB
				else
					goalDiff + zmatch.result.get.scoreB - zmatch.result.get.scoreA
			}
			else
				goalDiff
		}
	}
}

// Team standings
case class Standing (
	team: String,
	var played: Int,
	var points: Int,
	var wins: Int,
	var draws: Int,
	var losses: Int,
	var goalsScored: Int,
	var goalsAgainst: Int
) {
	
	def goalDiff: Int = (goalsScored - goalsAgainst)
	
	def update(zmatch: Match) {
		zmatch.result.foreach { result =>
			played += 1

			result.winner match {
				case None => points += 1; draws += 1;
				case Some(teamId) if (team == teamId) => points += 3; wins +=1;
				case _ => losses += 1
			}
			
			if (team == zmatch.teamA.get) {
				goalsScored += result.scoreA
				goalsAgainst += result.scoreB
			}
			else {
				goalsScored += result.scoreB
				goalsAgainst += result.scoreA
			}
		}
	}
}

object Standing {
	def Empty(team: String) = new Standing(team, 0, 0, 0, 0, 0, 0, 0)	
}
