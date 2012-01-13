package models

import play.api.db.DB
import play.api.Play.current

import anorm._
import anorm.SqlParser._

import models.Phase.Phase

case class Match(
	id: Pk[Long],
	teamA: Option[String],
	teamAformula: Option[String],
	teamB: Option[String],
	teamBformula: Option[String],
	kickoff: java.util.Date,
	phase: Phase, // example: round1, quarter finals, semi final, etc
	// stadium
	// result
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
	 * @return true wether this match concerns both supplied teams (in any order) 
	 */
	def concernTeams(teamX: String, teamY: String): Boolean = {
		(teamA == teamX && teamB == teamY) || (teamA == teamY && teamB == teamX)
	}
}

case class Result(
	result: String, // either 'DRAW', teamAId or TeamBId
	scoreA: Int,
	scoreB: Int
) {
	/**
	 * @return the winner (team id) or None it's a draw
	 */
	def winner: Option[String] = {
		if (result == "DRAW")
			Option(null)
		else
			Some(result)
	}
}

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
				if (result.isEmpty) // Match without Result
					Match(id, teamA, teamAformula, teamB, teamBformula, kickoff, Phase.withName(phase), Option(null))
				else { // Match with Result
					assert(scoreA.isDefined && scoreB.isDefined, "if there is a result, scores must exist")
					Match(id, teamA, teamAformula, teamB, teamBformula, kickoff, Phase.withName(phase), Some(Result(result.get, scoreA.get, scoreB.get)))
				}
			}
		}
	}
	
	def findAll(): Seq[Match] = {
		DB.withConnection { implicit connection =>
			SQL("select * from match").as(Match.simple *)
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
// XXX : NO TIMESTAMP SUPPORT !!!
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
			zmatch.copy(result = Some(Result(result, scoreA, scoreB)))
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
		// this generate a collection of tuples(2) that can be used to crate a map
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
				val standing = Standing(zmatch.teamA.get, 0, 0, 0, 0, 0, 0, 0)
				map(zmatch.teamA.get) = standing
				standing
			}
			val standingB = map.get(zmatch.teamB.get).getOrElse {
				val standing = Standing(zmatch.teamB.get, 0, 0, 0, 0, 0, 0, 0)
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
	
	/**
	 * function used to sort standings to give team positions
	 * @return true if first standing is before second one
	 */
//	def positionComp(standingA: Standing, standingB: Standing): Boolean = {
//		if (standingA.points > standingB.points) {
//			true
//		} else if (standingA.points < standingB.points) {
//			false
//		} 
//		else {
//			// same number of points, check goal difference be
//		}
//	}
	
	def update(zmatch: Match) {
		if (zmatch.played) {
			played += 1
			
			val result = zmatch.result.get
			
			if (result.winner.isEmpty) {
				points += 1
				draws += 1
			}
			else if (result.result == team) {
				points += 3
				wins += 3
			}
			else {
				losses += 1
			}
			
			if (team == zmatch.teamA) {
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
