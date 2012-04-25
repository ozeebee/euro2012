package models

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import play.api.Logger

/**
 * TODO : cache params
 */
object Param {
	val logger = Logger(this.getClass())

	val dateTimeFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
	
	val simple = {
		get[String]("name") ~
		get[Option[String]]("value") map {
			case name ~ value => (name, value)
		}
	}
	
	def findAll(): Map[String, Option[String]] = {
		DB.withConnection { implicit connection => 
			val lst = SQL("select name, value from param").as(Param.simple *)
			Map(lst: _*)
		}
	}

	def findValueByName(name: String): Option[String] = {
		DB.withConnection { implicit connection => 
			val opt: Option[(String, Option[String])] = 
				SQL("select name, value from param where name = {name}")
					.on('name -> name)
					.as(Param.simple.singleOpt)
			opt.flatMap(_._2)
		}
	}
	
	def create(name: String, value: String)(implicit connection: java.sql.Connection) = {
		SQL("insert into param (name, value) values ({name}, {value})")
			.on('name -> name, 'value -> value).executeUpdate()
	}
	
	def update(name: String, value: String)(implicit connection: java.sql.Connection) = {
		SQL("update param set value = {value} where name = {name}")
			.on('name -> name, 'value -> value).executeUpdate()
	}
	
	def setValue(name: String, value: String) = {
		DB.withConnection { implicit connection =>
			// check wether we should issue an insert or update statement
			SQL("select name from param where name = {name}").on('name -> name).singleOpt() match {
				case Some(result) => update(name, value) // record exists, update it
				case None => create(name, value) // no record yet, create it 
			}
		}
	}
	
	// ----------------- specific params 
	
	def getCurrentDateTime(): java.util.Date = {
		findValueByName("currentDateTime")
			.map(dateTimeFormat.parse(_))
			.getOrElse { 
				logger.info("no currentDateTime param defined, using now")
				new java.util.Date()
			}
	}
	
	def setCurrentDateTime(dateTime: java.util.Date) = {
		setValue("currentDateTime", dateTimeFormat.format(dateTime))
	}

	def getLiveMatch(): Option[Long] = {
		findValueByName("liveMatchId").map(_.toLong)
	}

	def setLiveMatch(matchId: Long) = {
		setValue("liveMatchId", matchId.toString)
	}
	
	def clearLiveMatch() = {
		setValue("liveMatchId", null)
	}
}