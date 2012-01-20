package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class User (
	name: String,
	email: String,
	password: String
)

object User {
	/**
	 * Parse a User from a ResultSet
	 */
	val simple = {
		get[String]("user.name") ~
		get[String]("user.email") ~
		get[String]("user.password") map {
			case name ~ email ~ password => User(name, email, password)
		}
	}

	def create(user: User) = {
		DB.withConnection { implicit connection =>
			SQL(
			  """
				insert into user (name, email, password) values (
					{name}, {email}, {password}
				)
			  """
			).on(
				'email -> user.email,
				'name -> user.name,
				'password -> user.password
			).executeUpdate()
		}
		
		user
	}
	
	def findAll(): Seq[User] = {
		DB.withConnection { implicit connection =>
			SQL("select name, email, password from user").as(User.simple *)
		}
	}
	
	/**
	 * @return true if a user with this name or email address already exists
	 */
	def exists(name: String, email: String): Boolean = {
		DB.withConnection { implicit connection =>
			SQL("select name from user where name = {name} or email = {email}")
				.on('name -> name, 'email -> email)
				.singleOpt()
				.isDefined
		}
	}
}

case class Forecast (
	username: String,
	matchid: Pk[Long],
	scoreA: Int,
	scoreB: Int
)

object Forecast {
	
	val simple = {
		get[String]("username") ~
		get[Pk[Long]]("matchid") ~
		get[Int]("scoreA") ~
		get[Int]("scoreB") map {
			case username ~ matchid ~ scoreA ~ scoreB => Forecast(username, matchid, scoreA, scoreB)
		}
	}
	
	def findByUser(user: String): Seq[Forecast] = {
		DB.withConnection { implicit connection =>
			SQL("select username, matchid, scoreA, scoreB from forecast where username = {username}")
				.on('username -> user)
				.as(Forecast.simple *)
		}
	}
	
	def create(forecast: Forecast)(implicit connection: java.sql.Connection) = {
		SQL("insert into forecast (username, matchid, scoreA, scoreB) values ({username}, {matchid}, {scoreA}, {scoreB})")
			.on('username -> forecast.username, 'matchid -> forecast.matchid, 'scoreA -> forecast.scoreA, 'scoreB -> forecast.scoreB)
			.executeUpdate()
	}
	
	def update(forecast: Forecast)(implicit connection: java.sql.Connection) = {
		SQL("update forecast set scoreA = {scoreA}, scoreB = {scoreB} where username = {username} and matchid = {matchid}")
			.on('username -> forecast.username, 'matchid -> forecast.matchid, 'scoreA -> forecast.scoreA, 'scoreB -> forecast.scoreB)
			.executeUpdate()
	}
	
	def saveForecast(forecast: Forecast) = {
		DB.withConnection { implicit connection =>
			// check wether we should issue an insert or update statement
			SQL("select matchid from forecast where username = {username} and matchid = {matchid}")
					.on('username -> forecast.username, 'matchid -> forecast.matchid)
					.singleOpt() match {
				case Some(result) => update(forecast) // record exists, update it
				case None => create(forecast) // no record yet, create it 
			}
		}
	}
}
