package controllers

import play.api.data._
import play.api.data.validation._
import play.api.mvc._
import play.api.Logger
import models.User

object Security extends Controller with Debuggable {
	val logger = Logger(this.getClass())

	val USERNAME = "username"
		
	def username(implicit request: RequestHeader) = request.session.get(USERNAME)
	
	// ~~~~~~~~~~~~~~~~~ Authentication ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~	
	
	// EXP: define a Form (mapping request parameters to Model properties)
	val loginForm = Form(
		of(User.apply _, User.unapply _)(
			"name" -> nonEmptyText(minLength = 4),
			"password" -> text,
			"email" -> ignored(null)
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
				println("login form has errors ! : " + formWithErrors)
				BadRequest(views.html.login(formWithErrors))
			},
			user => {
				println("login form is ok ! value is = " + user)

				if (request.body.asUrlFormEncoded.get.contains("rememberUser")) {
					println("! user has to be remembered !")
				}

				logger.info("user " + user + " has logged in")
				Redirect(routes.Application.index()).withSession(session + (USERNAME -> user.name)) // EXP : add the username to the session (stored in a cookie)
			}
		)
	}

	def logout() = Action { implicit request => // EXP: the "implicit request" is required because our templates requires the request
		logger.debug("logout requested")
		Redirect(routes.Security.login()).withNewSession
	}

	// ~~~~~~~~~~~~~~~~~ Registration ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
	
	val registerForm = Form(
		of(User.apply _, User.unapply _)(
			"name" -> (text verifying (Constraints.nonEmpty, 
										Constraints.minLength(4), 
										Constraints.pattern("""[a-zA-Z0-9._]+"""r, "constraint.username", "error.username"))),
			"password" -> nonEmptyText(minLength = 6),
			"email" -> email
		) verifying ("A user with the same name or email already exists", user => ! User.exists(user.name, user.email))
	)
	
	def register = Logged() {
		Action { implicit request =>
			Ok(views.html.register(registerForm))
		}
	}
	
	def registerSubmit = Action { implicit request =>
		val filledForm = registerForm.bindFromRequest()

		filledForm.fold(
			formWithErrors => {
				println("register form has errors ! : " + formWithErrors)
				BadRequest(views.html.register(formWithErrors))
			},
			user => {
				println("register form is ok ! value is = " + user)

				logger.info("user " + user + " created")

				User.create(user)
				
				//Redirect(routes.Security.login(user))
				Ok(views.html.login(loginForm.fill(user)))
			}
		)
		
	}

}