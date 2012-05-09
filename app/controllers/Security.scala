package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.mvc._
import play.api.Logger
import models.User

object Security extends Controller with Debuggable {
	val logger = Logger(this.getClass())

	val USERNAME = "username"
	val USEREMAIL = "useremail"
	val USERGROUPS = "usergroups"
	val UUID = "uuid"
		
	def username(implicit request: RequestHeader) = request.session.get(USERNAME)
	def useremail(implicit request: RequestHeader) = request.session.get(USEREMAIL)

	def isAdmin(request: RequestHeader): Boolean = {
		// comma separated list of user groups
		val groups = request.session.get(USERGROUPS)
		groups.exists(_.split(',').contains(User.AdminGroup))
	}
	
	// ~~~~~~~~~~~~~~~~~ Authentication ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~	
	
	/*
	// EXP: define a Form (mapping request parameters to Model properties)
	val loginForm = Form(
		mapping(
			"name" -> nonEmptyText,
			"email" -> ignored(null:String),
			"password" -> nonEmptyText,
			"groups" -> ignored(Option.empty[String]),
			"enabled" -> ignored(false),
			"modifDate" -> ignored(null:java.util.Date)
		)(User.apply)(User.unapply) 
		verifying ("Invalid email or password", user => {
			User.authenticate(user.name, user.password).isDefined
		}) 
	)
	*/

	// a more complicated version of the login form inspired from Signup (play2 scala forms sample) class
	val loginFormV2 = Form(
		mapping(
			"name" -> nonEmptyText,
			"password" -> nonEmptyText
		)
	    // The mapping signature doesn't match the User case class signature,
		// so we have to define custom binding/unbinding functions.
		{ // binding function
			// here we authenticate user by selecting it from DB. It if exists, the User object will be filled
			// with all User fields (including email). If it doesn't exist, we create a User object WITHOUT email address
			(name, password) => User.authenticate(name, password).getOrElse(User(name, null, password, None, false, null))
		}
		{ // unbinding function
			user => Some(user.name, user.password)
		}
		verifying ("Invalid user or password", user => {
			// the user will be considered authenticated if it's been found in DB and thus if the email field is not null (see above)
			user.email != null
		}) 
	)

	def login = Logged() {
		Action { implicit request =>
			Ok(views.html.login(loginFormV2))
		}
	}

	def authenticate = Action { implicit request =>
		// bind form data from the request (see play.api.data.Form class)
		val filledForm = loginFormV2.bindFromRequest()

		// EXP: the fold methods (Form class) requires two functions, the first is the function to be 
		//   applied in case of errors, the second one is the success function (taking the form value as argument)
		filledForm.fold(
			formWithErrors => {
				println("login form has errors ! : " + formWithErrors)
				BadRequest(views.html.login(formWithErrors))
			},
			user => {
				println("login form is ok ! value is = " + user)
				logger.info("user " + user + " has logged in")
				if (request.body.asFormUrlEncoded.get.contains("rememberUser")) {
					println("! user has to be remembered !")
				}

				Redirect(routes.Application.index()).withSession(
						USERNAME -> user.name, 
						USEREMAIL -> user.email,
						USERGROUPS -> user.groups.getOrElse(""),
						UUID -> java.util.UUID.randomUUID().getMostSignificantBits().toString() // unique 'session id'
					) // EXP : add the username, email and groups to the session (stored in a cookie)
			}
		)
	}

	def logout() = Action { implicit request => // EXP: the "implicit request" is required because our templates requires the request
		logger.debug("logout requested")
		Redirect(routes.Security.login()).withNewSession
	}

	// ~~~~~~~~~~~~~~~~~ Registration ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
	
	val registerForm = Form(
		mapping(
			"name" -> (text verifying (Constraints.nonEmpty, 
										Constraints.minLength(4), 
										Constraints.pattern("""[a-zA-Z0-9._]+"""r, "constraint.username", "error.username"))),
			"password" -> nonEmptyText(minLength = 6),
			"email" -> email,
			"groups" -> ignored(Option.empty[String]),
			"enabled" -> ignored(false),
			"modifDate" -> ignored(null:java.util.Date)
		)(User.apply)(User.unapply)
			verifying ("A user with the same name or email already exists", user => ! User.exists(user.name, user.email))
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
				Ok(views.html.login(loginFormV2.fill(user)))
			}
		)
		
	}
	
	// ~~~~~~~~~~~~~~~~~ Secured trait (inspired from zentask sample) ~~~~~~~~~
	
	/**
	 * Provide security features
	 */
	trait Secured {
	  
		private def username(request: RequestHeader) = request.session.get(USERNAME)
		
		/**
		 * Redirect to login if the user in not authorized.
		 */
		private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Security.login)
	  
		/** 
		 * Action for authenticated users.
		 */
		def Authenticated(f: => String => Request[AnyContent] => Result) = 
				play.api.mvc.Security.Authenticated(username, onUnauthorized) { user =>
					Action(request => f(user)(request))
		}
		
		/**
		 * Action for Admin users
		 */
		def IsAdmin(f: => String => Request[AnyContent] => Result) = Authenticated { username => request =>
			// comma separated list of user groups
			val groups = request.session.get(USERGROUPS)
			//println("*** AJO.isAdmin groups = [" + groups + "]")
			if (groups.exists(_.split(',').contains(User.AdminGroup))) {
				f(username)(request)
			}
			else {
				//Results.Forbidden
				Unauthorized(views.html.defaultpages.unauthorized())
			}
		}
		
	}
	
}