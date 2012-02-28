package controllers

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.mvc._
import models._
import Security.Secured

object Application extends Controller with Secured with Debuggable {
	val logger = Logger(this.getClass())

	// ~~~~~~~~~~~~~~~~~ Actions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	def index = Authenticated { username => implicit request =>
		Ok(views.html.index())
	}

	def showAbout = Authenticated { username => implicit request =>
		Ok(views.html.about())
	}
	
	def showTeams = Authenticated { username => implicit request =>
		val teams = Team.findAll()
		val groups = Team.getGroups()
		
		Ok(views.html.teams(teams, groups))
	}
	
	def showGroups = Authenticated { username => implicit request =>
		val teams = Team.findAll()
		val groups = Team.getGroups()
		Ok(views.html.groups(teams, groups))
	}

	def showSchedule = Authenticated { username => implicit request =>
		val matches = Match.findAll()
		val matchesByPhase = matches groupBy { zmatch =>
			zmatch.phase.toString()
		}
		
		Ok(views.html.schedule(matchesByPhase))
	}

	def showStandings = Authenticated { username => implicit request =>
		val groups = Team.getGroups()
		
		val matches = Match.findAll()
		val standings = Match.computeStandings(matches)
		println("standings = " + standings)
		
		Ok(views.html.standings(groups, standings))
	}
	
	def showGroup(group:String) = Authenticated { username => implicit request =>
		val matches = Match.findByGroup(group)
		val standings = Match.computeStandings(group, matches)
		Ok(views.html.group(group, standings, matches))
	}

	def test() = Authenticated { username => implicit request =>
		val values = Set("Value1", "Value2", "Value3")
		Ok(views.html.test(values))
	} 

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	def showForecasts = Authenticated { username => implicit request =>
		val matches = Match.findAll()
		val matchesByPhase = matches groupBy { zmatch =>
			zmatch.phase.toString()
		}
		
		val forecastsByMatch = Forecast.getForecastsByMatch(username)
		
		Ok(views.html.forecasts(matchesByPhase, forecastsByMatch))
	}
	
	def showForecastsForUser(user: String) = Authenticated { username => implicit request =>
		// show only forecasts of other players for played matches
		val matches = Match.findAll().filter(_.played)
		val matchesByPhase = matches groupBy { zmatch =>
			zmatch.phase.toString()
		}
		
		val forecastsByMatch = Forecast.getForecastsByMatch(user)

		// show only played phases
		val phases = Phase.values.toSeq.filter(phase => matchesByPhase.contains(phase.toString))
		Ok(views.html.forecastsForUser(user, phases, matchesByPhase, forecastsByMatch))
	}

	val forecastForm = Form(
		tuple(
			"matchId" -> longNumber,
			"scoreA" -> number(min=0, max=99),
			"scoreB" -> number(min=0, max=99)
		)
	)
	
	def saveForecast = Logged("saveForecast") { 
		Authenticated { username => implicit request =>
			forecastForm.bindFromRequest().fold(
				formWithErrors => BadRequest,
				dataTuple => {
					logger.debug("saving forecast dataTuple = " + dataTuple)
					Forecast.saveForecast(Forecast(username, anorm.Id(dataTuple._1), dataTuple._2, dataTuple._3))
					Ok
				} 
			)
		}
	}
	
	def showRanking = Authenticated { username => implicit request =>
		val users = User.findAll()
		val playedMatches = Match.findPlayed()
		val forecasts = Forecast.findAll()
		val rankings = Ranking.computeRanking(users, forecasts, playedMatches)
		Ok(views.html.ranking(rankings))
	}
	
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @return true if the forecast can be captured for the given target kickoff date/time
	 */
	def canForecast(kickoff: java.util.Date): Boolean = {
		val now = Param.getCurrentDateTime()
		now.before(kickoff)
	}
	
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
	object Json extends Controller with Secured with Debuggable {
		val logger = Logger(this.getClass())
		
		def getTeams = Authenticated { _ => _ =>
			val teams = Team.getTeams().map(_.id)
			Ok(toJson(teams))
		}
		
	}
}

trait Debuggable {
	val logger: Logger
	
	/**
	 * An action wrapper that logs the action invocation.
	 * @ajo implemented for testing composition (see Controller and Action in play.api.mvc for more details)
	 */
	def Logged[A](methodName: String = "<function>")(action: Action[A]): Action[A] = {
		Action(action.parser) { request =>
			logger.debug("*** [" + methodName + "] before invoke : " + action)
			val time = System.currentTimeMillis()
			val result = action(request)
			val time2 = System.currentTimeMillis()
			logger.debug("*** [" + methodName + "] after invoke : " + action + " time = " + (time2 - time) + " ms")
			result
		}
	}

	/*
	def Logged[A](action: Action[A]): Action[A] = {
		action.compose { (request, originalAction) =>
			logger.debug("*** before invoke : " + originalAction)
			val result = originalAction(request)
			logger.debug("*** after invoke : " + originalAction)
			result
		}
	}
	*/

}
