package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class User (
	name: String,
	email:String,
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
