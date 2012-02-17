package models.test

import play.api.Logger
import models.User._
import models.User
import models.Match
import models.Forecast
import models.Phase
import anorm.Id

trait Scenario {
	val name: String = getClass().getSimpleName().dropRight(1) // remove the trailing $ from the object names
	val description: String
	val category: String
	def apply(): Unit
}

trait UndoableScenario extends Scenario {
	def unapply(): Unit
}

object Scenarios {
	def getScenarios(): Seq[Scenario] = {
		Seq(
			ResetMatchResults, ResetForecasts, ResetUsers, ResetAll,
			MD1Results, MD2Results, MD3Results, GroupStageResults, RandomResults
		)
	}
	
	def getScenario(name: String): Option[Scenario] = {
		getScenarios().groupBy(_.name).get(name).map(_.head)
	}
}

// ~~~~~~~~~~~~~~~~~~~~~ Clean-up Scenarios ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

protected trait CleanScenario extends Scenario {
	val category = "Clean-up"
} 
object ResetForecasts extends CleanScenario {
	val description = "Reset all forecasts"
	def apply() = {
		// delete forecasts for all users
		Forecast.findAll().groupBy(_.username).keys.foreach(Forecast.delete(_))
	}
}

object ResetMatchResults extends CleanScenario {
	val description = "Reset match results"
	def apply() = {
		// XXX: hardcoded match ids
		(1 to 31).foreach(Match.clearResult(_))
	}
}

object ResetUsers extends CleanScenario {
	val description = "Reset user list"
	def apply() = {
		// remove all users except system users
		val sysusers = Seq("toto", "ozeebee")
		User.findAll().filterNot(user => sysusers.contains(user.name)).foreach(user => User.remove(user.name))
	}
}

object ResetAll extends CleanScenario {
	val description = "Reset all DB changes (match results, forecasts, users)"
	def apply() = {
		ResetMatchResults.apply()
		ResetForecasts.apply()
		ResetUsers.apply()
	}
}

// ~~~~~~~~~~~~~~~~~~~~~ Match Scenarios ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

trait MatchResultScenario extends UndoableScenario {
	val category = "Match"
}
object MD1Results extends MatchResultScenario {
	val description = "Set match results for MD1 phase"
	def apply() = {
		Match.updateResult( 1, 1, 0)
		Match.updateResult( 2, 1, 2)
		Match.updateResult( 3, 1, 1)
		Match.updateResult( 4, 3, 1)
		Match.updateResult( 5, 0, 0)
		Match.updateResult( 6, 3, 4)
		Match.updateResult( 7, 2, 1)
		Match.updateResult( 8, 1, 2)
	}
	def unapply() = {
		(1 to 8).foreach(Match.clearResult(_))
	}
}

object MD2Results extends MatchResultScenario {
	val description = "Set match results for MD2 phase"
	def apply() = {
		Match.updateResult( 9, 1, 0)
		Match.updateResult(10, 1, 2)
		Match.updateResult(11, 1, 1)
		Match.updateResult(12, 3, 1)
		Match.updateResult(13, 0, 0)
		Match.updateResult(14, 3, 4)
		Match.updateResult(15, 2, 1)
		Match.updateResult(16, 1, 2)
	}
	def unapply() = {
		(9 to 16).foreach(Match.clearResult(_))
	}
}

object MD3Results extends MatchResultScenario {
	val description = "Set match results for MD3 phase"
	def apply() = {
		Match.updateResult(17, 1, 0)
		Match.updateResult(18, 1, 2)
		Match.updateResult(19, 1, 1)
		Match.updateResult(20, 3, 1)
		Match.updateResult(21, 0, 0)
		Match.updateResult(22, 3, 4)
		Match.updateResult(23, 2, 1)
		Match.updateResult(24, 1, 2)
	}
	def unapply() = {
		(17 to 24).foreach(Match.clearResult(_))
	}
}

object GroupStageResults extends MatchResultScenario {
	val description = "Set match results for group stages (MD1, MD2, MD3)"
	def apply() = {
		MD1Results.apply()
		MD2Results.apply()
		MD3Results.apply()
	}
	def unapply() = {
		MD3Results.unapply()
		MD2Results.unapply()
		MD1Results.unapply()
	}
}

object RandomResults extends MatchResultScenario {
	val description = "Set random results for *ALL* matches"
	def apply() = {
		(1 to 31).foreach(Match.updateResult(_, Match.generateRandomScore(), Match.generateRandomScore()))
	}
	def unapply() = {
		(1 to 31).foreach(Match.clearResult(_))
	}
}

// ~~~~~~~~~~~~~~~~~~~~~ Forecast Scenarios ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

trait ForecastScenario extends UndoableScenario {
	val category = "Forecast"
}
object RandomForecasts extends ForecastScenario {
	val description = "Create random forecasts for *ALL* users thereby deleting all existing forecasts"
	def apply() = {
		User.findAll().foreach { user =>
			val forecasts = for (i <- 1 to 31) yield Forecast(user.name, Id(i), Match.generateRandomScore(), Match.generateRandomScore())
			// first delete any existing forecast for the user to prevent duplicate key excpetions...
			Forecast.delete(user.name)
			Forecast.create(forecasts)
		}
	}
	def unapply() = {
		ResetForecasts.apply()
	}
}
