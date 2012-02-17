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

	def authenticate(username: String, password: String): Option[User] = {
		DB.withConnection { implicit connection =>
			SQL("select name, email, password from user where name = {username} and password = {password}")
				.on('username -> username, 'password -> password)
				.as(User.simple.singleOpt)
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
	
	def remove(username: String): Boolean = {
		DB.withConnection { implicit connection =>
			SQL("delete from user where name = {name}").on('name -> username).executeUpdate() > 0
		}
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
	
	def gravatarUrl(email: String) = {
		"http://www.gravatar.com/avatar/" + app.utils.MD5.hash(email) + "?s=140&d=mm";
	}
}

case class Forecast (
	username: String,
	matchid: Pk[Long],
	scoreA: Int,
	scoreB: Int
) {
	val score = (scoreA, scoreB)
	
	def matchOutcome = {
		if (scoreA > scoreB)
			MatchOutcome.TEAM_A
		else if (scoreA < scoreB)
			MatchOutcome.TEAM_B
		else
			MatchOutcome.DRAW 
	}
	
	def outcome(result: Result) = {
		if (matchOutcome == result.outcome)
			if (score == result.score)
				ForecastOutcome.CORRECT_SCORE
			else
				ForecastOutcome.CORRECT_RESULT
		else
			ForecastOutcome.WRONG_FORECAST
	}
} 

object Forecast {
	
	val simple = {
		get[String]("username") ~
		get[Pk[Long]]("matchid") ~
		get[Int]("scoreA") ~
		get[Int]("scoreB") map {
			case username ~ matchid ~ scoreA ~ scoreB => Forecast(username, matchid, scoreA, scoreB)
		}
	}
	
	def findAll(): Seq[Forecast] = {
		DB.withConnection { implicit connection =>
			SQL("select username, matchid, scoreA, scoreB from forecast").as(Forecast.simple *)
		}
	}
	
	def findByUser(user: String): Seq[Forecast] = {
		DB.withConnection { implicit connection =>
			SQL("select username, matchid, scoreA, scoreB from forecast where username = {username}")
				.on('username -> user)
				.as(Forecast.simple *)
		}
	}
	
	def getForecastsByMatch(user: String): collection.mutable.Map[anorm.Pk[Long], models.Forecast] = {
		Forecast.findByUser(user)
			.foldLeft(collection.mutable.Map[anorm.Pk[Long], Forecast]()) { (map: collection.mutable.Map[anorm.Pk[Long], Forecast], forecast: Forecast) =>
				map(forecast.matchid) = forecast
				map
			}
	}
	
	def create(forecast: Forecast)(implicit connection: java.sql.Connection) = {
		SQL("insert into forecast (username, matchid, scoreA, scoreB) values ({username}, {matchid}, {scoreA}, {scoreB})")
			.on('username -> forecast.username, 'matchid -> forecast.matchid, 'scoreA -> forecast.scoreA, 'scoreB -> forecast.scoreB)
			.executeUpdate()
	}
	
	def create(forecasts: Seq[Forecast]) = {
		DB.withConnection { implicit connection =>
			// batch creation
			val sqlstr = "insert into forecast (username, matchid, scoreA, scoreB) values ({username}, {matchid}, {scoreA}, {scoreB})"
			val batchSql = SQL(sqlstr).asBatch
			
			forecasts.foldLeft(batchSql) { (batchSql, forecast) =>
				batchSql.addBatch('username.name -> forecast.username, 'matchid.name -> forecast.matchid, 'scoreA.name -> forecast.scoreA, 'scoreB.name -> forecast.scoreB)
			}.execute()
				
//			val forecast = forecasts.head
//			SQL(sqlstr).asBatch.addBatch('username.name -> forecast.username, 'matchid.name -> forecast.matchid, 'scoreA.name -> forecast.scoreA, 'scoreB.name -> forecast.scoreB).execute()
		}
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
	
	def delete(user: String) = {
		DB.withConnection { implicit connection =>
			SQL("delete from forecast where username = {username}").on('username -> user).executeUpdate()
		}
	}
	
	def delete(user: String, matchid: Long) = {
		DB.withConnection { implicit connection =>
			SQL("delete from forecast where username = {username} and matchid = {matchid}").on('username -> user, 'matchid -> matchid).executeUpdate()
		}
	}
}

case class UserRanking (user: User, var points: Int, var forecastedMatches: Int, 
		var correctScores: Int, var correctResults: Int, var badForecasts: Int)

object Ranking {
	val correctResultMap = Map(
			Phase.MD1 -> 1, Phase.MD2 -> 1, Phase.MD3 -> 1,
			Phase.QUARTERFINALS -> 4,
			Phase.SEMIFINALS -> 6,
			Phase.FINAL -> 15
	)

	val correctScoreMap = Map(
			Phase.MD1 -> 3, Phase.MD2 -> 3, Phase.MD3 -> 3,
			Phase.QUARTERFINALS -> 7,
			Phase.SEMIFINALS -> 10,
			Phase.FINAL -> 20
	)

	def computeRanking(users: Seq[User], forecasts: Seq[Forecast], matches: Seq[Match]) = {
		// create a map of matches keyed by match.id
		val matchMap = matches.foldLeft(collection.mutable.Map[Pk[Long], Match]()) { (map: collection.mutable.Map[Pk[Long], Match], zmatch: Match) =>
			map(zmatch.id) = zmatch
			map
		}
		
		// create initial user ranking map with one entry for each user found (even if he has no forecast)
		val rankingMap = Map((for (user <- users) yield (user.name -> UserRanking(user, 0, 0, 0, 0, 0))) : _*)

		forecasts.foldLeft(rankingMap) { (map: Map[String, UserRanking], forecast: Forecast) =>
			val userRanking = map(forecast.username) // XXX: we should consider the case when one user has entered a forecast then was unregistered (removed) from the application ! (in which case this statement whill yield an exception...)
			matchMap.get(forecast.matchid).foreach { zmatch =>
				zmatch.result.foreach { result =>
					userRanking.forecastedMatches += 1
					
					println("forecast.matchid="+forecast.matchid+" forecast.matchOutcome="+forecast.matchOutcome+" result.outcome="+result.outcome)
					forecast.outcome(result) match {
						case ForecastOutcome.CORRECT_RESULT => {
							println("forecast.matchid="+forecast.matchid+" correct result")
							userRanking.correctResults += 1
							userRanking.points += correctResultMap(zmatch.phase)
						}
						case ForecastOutcome.CORRECT_SCORE => {
							println("forecast.matchid="+forecast.matchid+" correct score")
							userRanking.correctScores += 1
							userRanking.points += correctResultMap(zmatch.phase) + correctScoreMap(zmatch.phase)
						}
						case ForecastOutcome.WRONG_FORECAST => userRanking.badForecasts += 1 
					}					
				}
			}
			map
		}.values.toSeq.sortWith(_.points > _.points)
	}
}
