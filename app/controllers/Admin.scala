package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.data.format._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.mvc._
import play.api.Logger
import models._
import models.Phase.Phase
import models.test._
import anorm.NotAssigned
import Security.Secured

object Admin extends Controller with Debuggable with Secured {
	val logger = Logger(this.getClass())

	val phaseFormat = new Formatter[Phase] {
		def bind(key: String, data: Map[String, String]) = {
			Formats.stringFormat.bind(key, data).right.flatMap { value =>
				println("value = " + value)
				scala.util.control.Exception.allCatch
					.either(Phase.withName(value))
					.left.map(e => Seq(FormError(key, "error.phase", Nil)))
			}
		}

		def unbind(key: String, value: Phase) = Map(
			key -> value.toString()
		)
	}

	val matchForm = Form(
		mapping(
			"id" -> ignored(NotAssigned: anorm.Pk[Long]),
			"teamA" -> optional(nonEmptyText),
			"teamAformula" -> ignored(None:Option[String]), // ignored(Option(null))
			"teamB" -> optional(nonEmptyText),
			"teamAformula" -> ignored(Option.empty[String]), // Option.empty[String] same as None:Option[String] same as Option(null.asInstanceOf[models.Result])
			"kickoff" -> date("dd/MM/yyyy HH:mm"),
			"phase" -> of[Phase](phaseFormat),
			"result" -> ignored(Option.empty[models.Result])
		)(Match.apply)(Match.unapply)
	)
	
	val resultForm = Form(
		tuple(
			"scoreA" -> number,
			"scoreB" -> number
		)
	)

	def showAdmin = Authenticated { username => implicit request =>
		Ok(views.html.admin())
	}

	def showMatches = Authenticated { username => implicit request =>
		val matches = Match.findAll()
		Ok(views.html.adminpages.matches(matches, matchForm))
	}
	
	def newMatch = Authenticated { username => implicit request =>
		logger.debug("newMatch")
		
		matchForm.bindFromRequest().fold(
			formWithErrors => {
				println("form has errors ! : " + formWithErrors)
				val matches = Match.findAll()
				BadRequest(views.html.adminpages.matches(matches, formWithErrors))
			},
			zmatch => {
				println("form is ok ! value is = " + zmatch)
				logger.info("new match : " + zmatch)
				Match.create(zmatch)
				Redirect(routes.Admin.showAdmin())
			}
		)
	}
	
	def updateMatchResult(matchId: Long) = Logged("updateMatchResult") { 
		Authenticated { username => implicit request =>
			val filledForm = resultForm.bindFromRequest() 
			filledForm.fold(
				formWithErrors => {
					logger.debug("ERROR !")
					val rslt = toJson(formWithErrors.errors.map(_.key))
					logger.debug("  content=" + rslt)
					BadRequest(rslt)
				},
				data => {
					logger.debug("OK, data = " + data)
					val scoreA = filledForm.get._1.toInt
					val scoreB = filledForm.get._2.toInt
					
					Match.updateResult(matchId, scoreA, scoreB).map { zmatch =>
						Ok("ok")
					}.getOrElse {
						BadRequest("could not update match result")
					}
				}
			)
		}
	}
	
	def deleteMatchResult(matchId: Long) = Logged("deleteMatchResult") { 
		Authenticated { username => implicit request =>
			Match.clearResult(matchId)
			Ok("ok")
		}
	}
	
	def showUsers = Authenticated { username => implicit request =>
		val users = User.findAll()
		Ok(views.html.adminpages.users(users))
	}
	
	def showUserForecasts(username: String) = Logged("showUserForecasts") {
		Authenticated { authenticatedUsername => implicit request =>
			val matches = Match.findAll()
			val forecastsByMatch = Forecast.getForecastsByMatch(username)

			Ok(views.html.adminpages.userForecasts(username, matches, forecastsByMatch))
		}
	}
	
	def deleteUserForecasts(username: String) = Logged("deleteUserForecasts") {
		Authenticated { authenticatedUsername => implicit request =>
			Forecast.delete(username)
			Redirect(routes.Admin.showUserForecasts(username))
		}
	}
	
	def randomForecasts(username: String) = Logged("randomForecasts") {
		Authenticated { authenticatedUsername => implicit request =>
			val matches = Match.findAll()
			val forecastsByMatch = Forecast.getForecastsByMatch(username)
			// get matches for which no forecast has been entered yet
			//  and generate a forecast with random scores for each of them
			val forecasts = matches.filter(m => ! forecastsByMatch.contains(m.id)).map { zmatch =>
				val randomResult = Match.generateRandomResult()
				logger.debug("generating random forecast for match " + zmatch.id + " score : " + randomResult._1 + "-" + randomResult._2)
				Forecast(username, zmatch.id, randomResult._1, randomResult._2)
			}
			
			Forecast.create(forecasts)
			
			Redirect(routes.Admin.showUserForecasts(username))
		}
	}
	
	val forecastForm = Form(
		tuple(
			"username" -> nonEmptyText,
			"matchId" -> longNumber,
			"scoreA" -> number(min=0, max=99),
			"scoreB" -> number(min=0, max=99)
		)
	)
	
	def updateForecast(username: String, matchId: Long) = Logged("updateForecast") { 
		Authenticated { authenticatedUsername => implicit request =>
			forecastForm.bindFromRequest().fold(
				formWithErrors => BadRequest,
				dataTuple => {
					logger.debug("saving forecast dataTuple = " + dataTuple)
					val (user, matchId, scoreA, scoreB) = dataTuple
					Forecast.saveForecast(Forecast(user, anorm.Id(matchId), scoreA, scoreB))
					Ok
				} 
			)
		}
	}
	
	def deleteForecast(username: String, matchid: Long) = Logged("deleteForecast") { 
		Authenticated { authenticatedUsername => implicit request =>
			logger.debug("deleting forecast with username " + username + " and matchid " + matchid)
			Forecast.delete(username, matchid)
			Ok
		}
	}

	def showScenarios = Authenticated { username => implicit request =>
		Ok(views.html.adminpages.scenarios(models.test.Scenarios.getScenariosByCategory()))
	}

	def applyScenario(name: String) = Logged("applyScenario") { 
		Authenticated { authenticatedUsername => implicit request =>
			Scenarios.getScenario(name).map { scenario =>
				try {
					scenario.apply()
					Ok
				}
				catch {
					case e: AssertionError => BadRequest(e.getMessage())
					case t: Throwable => { t.printStackTrace(); BadRequest(t.toString()) }
				}
			}.getOrElse {
				BadRequest("Scenario " + name + " not found")
			}
		}
	}
	
	def unapplyScenario(name: String) = Logged("unapplyScenario") { 
		Authenticated { authenticatedUsername => implicit request =>
			Scenarios.getScenario(name).map { scenario =>
				scenario match {
					case undoableScenario: UndoableScenario => {
						try {
							undoableScenario.unapply(); 
							Ok
						}
						catch {
							case e: AssertionError => BadRequest(e.getMessage())
							case _ => BadRequest
						}
					}
					case _ => BadRequest("Scenario " + name + " cannt be unapplied because it is not undoable")
				}
			}.getOrElse {
				BadRequest("Scenario " + name + " not found")
			}
		}
	}
	
	val currentDateTimeForm = Form(single("dateTime" -> date("dd/MM/yyyy HH:mm")))
	
	def setCurrentDateTime() = Logged("setCurrentDateTime") {
		Authenticated { authenticatedUsername => implicit request =>
			Form("dateTime" -> date("dd/MM/yyyy HH:mm")).bindFromRequest().fold(
				formWithErrors => BadRequest,
				date => {
					logger.debug("OK, date = " + date)
					Param.setCurrentDateTime(date)
					Ok("ok")
				}
			)
		}
	}
	
	def setTeamName() = Logged("setTeamName") {
		Authenticated { authenticatedUsername => implicit request =>
			val form = Form(
				tuple(
					"matchId" -> longNumber,
					"team" -> text,
					"isTeamA" -> boolean
				)
			)
			
			form.bindFromRequest().fold(
				formWithErrors => BadRequest,
				dataTuple => {
					logger.debug("setTeamName dataTuple=" + dataTuple)
					val (matchId, team, isTeamA) = dataTuple
					// check wether it's a realm team or the formula
					val teamValue: String = if (Match.isFormula(team)) null else team
					Match.setTeamName(matchId, teamValue, isTeamA)
					Ok("")
				}
			)
		}
	}
}