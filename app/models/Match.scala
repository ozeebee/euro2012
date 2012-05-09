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
	
	val isLive: Boolean = Param.getLiveMatch().exists(_ == id.get)

	/**
	 * @return the group for which this match is played if this is a group match
	 */
	def group(): Option[String] = {
		if (isGroupStageMatch()) {
			teamA flatMap { teamId =>
				val team = Team.getTeam(teamId)
				team.map(_.group)
			}
		}
		else
			None
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
	 * NOTE: this does *NOT* take into account possible penalties scores 
	 */
	def winner: Option[String] = result.map(_.winner).getOrElse(None)
	
	/**
	 * @return the winning team taking into account penalties (if any)
	 */
	def finalWinner: Option[String] = {
		result match {
			case Some(r) => {
				r.winner
					.map(Some(_)) // return either the winner (as an Option[String])
					.getOrElse { // or the winner after penalties (in case of draw)
						r.penaltiesScore match {
							case Some((penScoreA: Int, penScoreB: Int)) if (penScoreA > penScoreB) => teamA
							case Some((penScoreA: Int, penScoreB: Int)) if (penScoreA < penScoreB) => teamB
							case Some((penScoreA: Int, penScoreB: Int)) if (penScoreA == penScoreB) => throw new IllegalArgumentException("penalty scores shout not be equal !!!!")
							case None => None
						}
					}
			}
			case None => None
		}
	}
	
	/**
	 * @return true if team A is the winner, false if the result is a draw of if the match has not yet been played
	 */
	def isTeamAWinner: Boolean = {
		finalWinner.map(_ == teamA.get).getOrElse(false)
	}
	
	/**
	 * @return true if team B is the winner, false if the result is a draw of if the match has not yet been played
	 */
	def isTeamBWinner: Boolean = {
		finalWinner.map(_ == teamB.get).getOrElse(false)
	}

	/**
	 * @return true wether this match concerns both supplied teams (in any order) 
	 */
	def concernTeams(teamX: String, teamY: String): Boolean = {
		(teamA == teamX && teamB == teamY) || (teamA == teamY && teamB == teamX)
	}
	
	def isGroupStageMatch(): Boolean = {
		Phase.GROUPSTAGE.contains(phase)
	}
}

/**
 * Match Result.
 * This model is tailored for our forecasting system, not for storing real match results with all details.
 * In this regard, the penaltiesScore has been added so that it is possible to determine a winning team
 * for direct elimination matches (non group-stage matches).
 */
case class Result(
	outcome: MatchOutcome,
	winner: Option[String],
	scoreA: Int,
	scoreB: Int,
	penaltiesScore: Option[Tuple2[Int, Int]]	// the optional score for penalties
) {
	val score = (scoreA, scoreB)
}

object Match {
	val GroupFormula = """(WIN|SEC)_GROUP_(.*)""".r
	val MatchFormula = """WIN_(\d+)""".r

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
		get[Option[Int]]("match.scoreB") ~
		get[Option[Int]]("match.penaltyScoreA") ~
		get[Option[Int]]("match.penaltyScoreB") map {
			case id~teamA~teamAformula~teamB~teamBformula~kickoff~phase~result~scoreA~scoreB~penaltyScoreA~penaltyScoreB => {
				result.map { result => // Match with Result
					assert(scoreA.isDefined && scoreB.isDefined, "if there is a result, scores must exist")
					Match(id, teamA, teamAformula, teamB, teamBformula, kickoff, Phase.withName(phase), result match {
						case "DRAW" => {
							// in case of draw, penalties score determine the winner (for non-group stage matches)
							Some(Result(MatchOutcome.DRAW, None, scoreA.get, scoreB.get, 
									if (penaltyScoreA.isDefined && penaltyScoreB.isDefined) Some(penaltyScoreA.get, penaltyScoreB.get) else None))	
						} 
						case _ => {
							if (result == teamA.get) Some(Result(MatchOutcome.TEAM_A, Some(result), scoreA.get, scoreB.get, None))
							else if (result == teamB.get) Some(Result(MatchOutcome.TEAM_B, Some(result), scoreA.get, scoreB.get, None))
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
				'kickoff -> new java.sql.Timestamp(zmatch.kickoff.getTime()),
				'phase -> zmatch.phase.toString()
			).executeUpdate()
			
			zmatch.copy(id = Id(id))
		}
	}

	/**
	 * Set team names for a match (to be used for matches whose team names are not yet known (non-group stage matches))
	 */
	def setTeamNames(matchId: Long, teamA: String, teamB: String): Int = {
		DB.withConnection { implicit connection =>
			SQL("""
				update match set
					teamA = {teamA},
					teamB = {teamB}
				where id = {id} and phase not in ('MD1', 'MD2', 'MD3')
			""").on('id -> matchId, 'teamA -> teamA, 'teamB -> teamB)
			.executeUpdate()
		}
	}
	
	def setTeamName(matchId: Long, team: String, isTeamA: Boolean): Int = {
		DB.withConnection { implicit connection =>
			val teamCol = if (isTeamA) "teamA" else "teamB"
			SQL("update match set " + teamCol + " = {team} where id = {id} and phase not in ('MD1', 'MD2', 'MD3')")
				.on('id -> matchId, 'team -> team)
			.executeUpdate()
		}
	}

	/**
	 * Set team formulas for given match (check is made against phase)
	 */
	def setTeamFormulas(matchId: Long, teamAformula: String, teamBformula: String): Int = {
		DB.withConnection { implicit connection =>
			SQL("""
				update match set
					teamAformula = {teamAformula},
					teamBformula = {teamBformula}
				where id = {id} and phase not in ('MD1', 'MD2', 'MD3')
			""").on('id -> matchId, 'teamAformula -> teamAformula, 'teamBformula -> teamBformula)
			.executeUpdate()
		}
	}

	def updateResult(matchId: Long, scoreA: Int, scoreB: Int, penaltiesScore: Option[Tuple2[Int, Int]]): Option[Match] = {
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
					set result = {result}, scoreA = {scoreA}, scoreB = {scoreB}, 
						penaltyScoreA = {penaltyScoreA}, penaltyScoreB = {penaltyScoreB} 
					where id = {id}
				""").on(
					'id -> matchId,
					'result -> result,
					'scoreA -> scoreA,
					'scoreB -> scoreB,
					'penaltyScoreA -> penaltiesScore.map(_._1).getOrElse(null),
					'penaltyScoreB -> penaltiesScore.map(_._2).getOrElse(null)
				).executeUpdate()
			}
			
			// set result in match
			val newResult = result match {
				case "DRAW" => Some(Result(MatchOutcome.DRAW, None, scoreA, scoreB, penaltiesScore))
				case teamId if (teamId == zmatch.teamA.get) => Some(Result(MatchOutcome.TEAM_A, Some(teamId), scoreA, scoreB, penaltiesScore))
				case teamId if (teamId == zmatch.teamB.get) => Some(Result(MatchOutcome.TEAM_B, Some(teamId), scoreA, scoreB, penaltiesScore))
			}
			zmatch.copy(result = newResult)
		}
	}
	
	def clearResult(matchId: Long) = {
		DB.withConnection { implicit connection =>
			SQL("""
				update match 
				set result = null, scoreA = null, scoreB = null, penaltyScoreA = null, penaltyScoreB = null
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
		//println("  randInt = " + randInt)
		val index = randomScoreRanges.indexWhere(_.contains(randInt))
		// index is the score
		index
	}
	
	def generateRandomResult(): (Int, Int) = {
		(generateRandomScore(), generateRandomScore())
	}

	def generateRandomPenalties(): (Int, Int) = {
		val rslt = (generateRandomScore(), generateRandomScore())
println("*** Generating penalties : rslt = " + rslt)
		if (rslt._1 != rslt._2) // cannot accept a draw for penalties...
			rslt
		else
			generateRandomPenalties()	
	}

	def isFormula(str: String): Boolean = {
		str match {
			case GroupFormula(_) => true
			case MatchFormula(_) => true
			case _ => false
		}
	}
	
	// try to resolve a team name for a given formula and for the given matches results
	def formulaTeamLookup(formula: String, matches: Seq[Match]): Option[String] = {
		formula match {
			case GroupFormula(pos, group) => {
				val standings = computeStandings(group, matches)
				if (pos == "WIN") Some(standings(0).team) else Some(standings(1).team)
			}
			case MatchFormula(matchId) => {
				val m = matches.find(_.id.get == matchId.toLong)
println("*** AJO : match is " + m)
println("          finalWinner = " + m.get.finalWinner)
				matches.find(_.id.get == matchId.toLong).flatMap(_.finalWinner)
			}
			case _ => None
		}
	}
	
	/**
	 * @return the live (currently played) Match or None if no live match
	 */
	def getLiveMatch(): Option[Match] = {
		Param.getLiveMatch().map(matchId => findById(matchId).getOrElse(throw new IllegalArgumentException("cannot find live match "+matchId+"!!")))
		/*DB.withConnection { implicit connection =>
			SQL("select * from match where match.id = (select value from param where name = 'liveMatchId')").as(Match.simple.singleOpt)
		}*/
	}
	
	def startLiveMatch(matchId: Long): Match = {
		val zmatch = Match.updateResult(matchId, 0, 0, None)
		Param.setLiveMatch(matchId)
		zmatch.get
	}
	
	def stopLiveMatch() = {
		Param.clearLiveMatch()
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
