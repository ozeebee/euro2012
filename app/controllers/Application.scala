package controllers

import play.api._
import play.api.mvc._
import models.Team

object Application extends Controller {
	val logger = Logger(this.getClass())

	def index = Action {
		Ok(views.html.index("Your new application is ready."))
	}

	def showTeams = Action {
		val teams = Team.findAll()
		
		val groups = getGroups(teams)
		
		Ok(views.html.teams(teams, groups))
	}
	
	def getGroups(teams: Seq[Team]): Set[String] = {
		//teams map(println(_.group))
		val rslt = teams.map(_.group)
		println("rslt = " + rslt)
		val rslt2 = rslt.toSet
		println("rslt2 = " + rslt2)
		//teams map(_.group) toSet
		
		rslt2 map { group =>
			println("group : " + group)
		}
		
		rslt2
	}
}