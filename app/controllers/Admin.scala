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
			"scoreB" -> number,
			"penaltyScoreA" -> optional(number),
			"penaltyScoreB" -> optional(number)
		) verifying ("Either both penalty scores must be set or none", data => {
			(data._3.isDefined && data._4.isDefined) || (data._3.isEmpty && data._4.isEmpty)
		})
	)

	def showAdmin = IsAdmin { username => implicit request =>
		Ok(views.html.admin())
	}

	def showMatches = IsAdmin { username => implicit request =>
		val matches = Match.findAll()
		Ok(views.html.adminpages.matches(matches, matchForm))
	}
	
	def newMatch = IsAdmin { username => implicit request =>
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
		IsAdmin { username => implicit request =>
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
					val (scoreA, scoreB, penaltyScoreA, penaltyScoreB) = data
					val penaltiesScore = if (penaltyScoreA.isDefined) { Some(penaltyScoreA.get, penaltyScoreB.get) } else { None }
					Match.updateResult(matchId, scoreA, scoreB, penaltiesScore).map { zmatch =>
						if (zmatch.isLive) {
							EventBus.publishRaw(event = "live-match-update", utils.SSE.formatResultScore(scoreA, scoreB, penaltiesScore))
						}
						Ok("ok")
					}.getOrElse {
						BadRequest("could not update match result")
					}
				}
			)
		}
	}
	
	def deleteMatchResult(matchId: Long) = Logged("deleteMatchResult") { 
		IsAdmin { username => implicit request =>
			Match.clearResult(matchId)
			Ok("ok")
		}
	}
	
	def showUsers = IsAdmin { username => implicit request =>
		val users = User.findAll()
		Ok(views.html.adminpages.users(users))
	}
	
	def setUserState(username: String) = Logged("setUserState") {
		IsAdmin { authenticatedUsername => implicit request =>
			val form = Form("isEnabled" -> boolean)
			form.bindFromRequest().fold(
				formWithErrors => BadRequest,
				isEnabled => {
					logger.debug("setUserState isEnabled=" + isEnabled)
					User.setState(username, isEnabled);
					Ok("")
				}
			)
		}
	}
	
	def showUserForecasts(username: String) = Logged("showUserForecasts") {
		IsAdmin { authenticatedUsername => implicit request =>
			val matches = Match.findAll()
			val forecastsByMatch = Forecast.getForecastsByMatch(username)

			Ok(views.html.adminpages.userForecasts(username, matches, forecastsByMatch))
		}
	}
	
	def deleteUserForecasts(username: String) = Logged("deleteUserForecasts") {
		IsAdmin { authenticatedUsername => implicit request =>
			Forecast.delete(username)
			Redirect(routes.Admin.showUserForecasts(username))
		}
	}
	
	def randomForecasts(username: String) = Logged("randomForecasts") {
		IsAdmin { authenticatedUsername => implicit request =>
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
		IsAdmin { authenticatedUsername => implicit request =>
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
		IsAdmin { authenticatedUsername => implicit request =>
			logger.debug("deleting forecast with username " + username + " and matchid " + matchid)
			Forecast.delete(username, matchid)
			Ok
		}
	}

	def showScenarios = IsAdmin { username => implicit request =>
		Ok(views.html.adminpages.scenarios(models.test.Scenarios.getScenariosByCategory()))
	}

	def applyScenario(name: String) = Logged("applyScenario") { 
		IsAdmin { authenticatedUsername => implicit request =>
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
		IsAdmin { authenticatedUsername => implicit request =>
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
		IsAdmin { authenticatedUsername => implicit request =>
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
		IsAdmin { authenticatedUsername => implicit request =>
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
					Ok
				}
			)
		}
	}
	
	def startLiveMatch(matchId: Long) = Logged("startLiveMatch") { 
		IsAdmin { authenticatedUsername => implicit request =>
			val zmatch = Match.startLiveMatch(matchId)
			/*val result = zmatch.result.get
			val jsonObj = Json.toJson(
				Map(
					"teamA" -> Map(
								"name" -> toJson(zmatch.teamAorFormula),
								"score" -> toJson(result.scoreA)
							),
					"teamB" -> Map(
								"name" -> toJson(zmatch.teamBorFormula),
								"score" -> toJson(result.scoreB)
							)
				)
			)
			val jsonStr = Json.stringify(jsonObj)
			logger.debug("jsonStr = " + jsonStr)
			val data = jsonStr.split("\n").map("data: " + _ + "\n").mkString + "\n"
			logger.debug("data = " + data)
			EventBus.publishRaw(event = "live-match-started")*/
			EventBus.publish(event = "live-match-started")
			Ok
		}
	}
	
	def stopLiveMatch(matchId: Long) = Logged("stopLiveMatch") { 
		IsAdmin { authenticatedUsername => implicit request =>
			Match.stopLiveMatch()
			EventBus.publish(event = "live-match-finished")
			Ok
		}
	}

}