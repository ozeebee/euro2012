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
	 * @return the winner (team id) or None it's a draw OR if no result yet
	 */
	def winner: Option[String] = {
		if (result.isEmpty)
			Option(null)
		else
			result.get.winner
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
		get[Pk[Long]]("match.id") ~/
		get[Option[String]]("match.teamA") ~/
		get[Option[String]]("match.teamAformula") ~/
		get[Option[String]]("match.teamB") ~/
		get[Option[String]]("match.teamBformula") ~/
		get[java.util.Date]("match.kickoff") ~/
		get[String]("match.phase") ~/
		get[Option[String]]("match.result") ~/
		get[Option[Int]]("match.scoreA") ~/
		get[Option[Int]]("match.scoreB") ^^ {
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
			SQL("select * from match where id={id}").on('id -> id).as(Match.simple ?)
		}
	}
	
	def create(zmatch: Match): Match = {
		DB.withConnection { implicit connection =>
			// generated identifier
			val id: Long =  SQL("select next value for match_seq").as(scalar[Long])
			
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
}
