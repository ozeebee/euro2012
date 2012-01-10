package models

import play.api.db.DB
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Team(
	id: String,
	name: String,
	group: String
)

object Team {
	
	// Parser
	val simple = {
		get[String]("team.id") ~
		get[String]("team.name") ~
		get[String]("team.group") map {
			case id~name~group => Team(id, name, group)
		}
		
	}
	
	def findAll(): Seq[Team] = {
		DB.withConnection { implicit connection =>
			SQL("select * from team").as(Team.simple *)
		}
	}
	
	// ~~~~~~~~~~~~~~~~~ Utilities ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * @return the teams in the given group
	 */
	def getTeams(teams: Seq[Team], group: String): Seq[Team] = {
		teams filter (_.group == group)
	}
}
