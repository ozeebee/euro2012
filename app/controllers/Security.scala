package controllers

import play.api.data._
import play.api.mvc._
import play.api.Logger
import models.User

object Security extends Controller with Debuggable {
	val logger = Logger(this.getClass())

	val USERNAME = "username"
	
	// EXP: define a Form (mapping request parameters to Model properties)
	val loginForm = Form(
		of(User.apply _, User.unapply _)(
			"username" -> requiredText(minLength = 4),
			"password" -> text
		)
	)
	
	def login = Logged() {
		Action { implicit request =>
			Ok(views.html.login(loginForm))
		}
	}

	def authenticate = Action { implicit request =>
		// bind form data from the request (see play.api.data.Form class)
		val filledForm = loginForm.bindFromRequest()

		// EXP: the fold methods (Form class) requires two functions, the first is the function to be 
		//   applied in case of errors, the second one is the success function (taking the form value as argument)
		filledForm.fold(
			formWithErrors => {
				println("form has errors ! : " + formWithErrors)
				BadRequest(views.html.login(formWithErrors))
			},
			user => {
				println("form is ok ! value is = " + user)

				if (request.body.asUrlFormEncoded.get.contains("rememberUser")) {
					println("! user has to be remembered !")
				}

				logger.info("user " + user + " has logged in")
				Redirect(routes.Application.index()).withSession(session + (USERNAME -> user.username)) // EXP : add the username to the session (stored in a cookie)
			}
		)
	}

	def logout() = Action { implicit request => // EXP: the "implicit request" is required because our templates requires the request
		logger.debug("logout requested")
		Redirect(routes.Security.login()).withNewSession
	}



}