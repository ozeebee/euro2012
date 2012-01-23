package controllers

import play.api.data._
import play.api.data.format._
import play.api.libs.json._
import play.api.mvc._
import play.api.Logger
import models._
import models.Phase.Phase
import anorm.NotAssigned

object Admin extends Controller with Debuggable {
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
		of(Match.apply _, Match.unapply _)(
			"id" -> ignored(NotAssigned),
			"teamA" -> optional(nonEmptyText),
			"teamAformula" -> ignored(Option(null)),
			"teamB" -> optional(nonEmptyText),
			"teamAformula" -> ignored(Option(null)),
			"kickoff" -> date("dd/MM/yyyy HH:mm"),
			"phase" -> of[Phase](phaseFormat),
			"result" -> ignored(Option(null))
		)
	)
	
	val resultForm = Form(
		of(
			"scoreA" -> number,
			"scoreB" -> number
		)
	)

	def showAdmin = Action { implicit request =>
		Ok(views.html.admin())
	}

	def showMatches = Action { implicit request =>
		val matches = Match.findAll()
		Ok(views.html.adminpages.matches(matches, matchForm))
	}
	
	def newMatch = Action { implicit request =>
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
		Action { implicit request =>
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
		Action { implicit request =>
			Match.clearResult(matchId)
			Ok("ok")
		}
	}
	
	def showUsers = Action { implicit request =>
		val users = User.findAll()
		Ok(views.html.adminpages.users(users))
	}
	
	def showUserForecasts(username: String) = Logged("showUserForecasts") {
		Action { implicit requests =>
			val matches = Match.findAll()
			
			val username = Security.username.get
			
			val forecastsByMatch = Forecast.findByUser(username)
				.foldLeft(collection.mutable.Map[anorm.Pk[Long], Forecast]()) { (map: collection.mutable.Map[anorm.Pk[Long], Forecast], forecast: Forecast) =>
					map(forecast.matchid) = forecast
					map
				}

			Ok(views.html.adminpages.userForecasts(username, matches, forecastsByMatch))
		}
	}
	
	val currentDateTimeForm = Form(of("dateTime" -> date("dd/MM/yyyy HH:mm")))
	
	def setCurrentDateTime() = Logged("setCurrentDateTime") {
		Action { implicit request =>
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
	
}