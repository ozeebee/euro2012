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
			ResetMatchResults, ResetTeamNames, ResetForecasts, ResetUsers, ResetAll,
			MD1Results, MD2Results, MD3Results, GroupStageResults, RandomGroupStageResults, 
				RandomQFResults, RandomSFResults, RandomFinalResult, RandomResults,
			RandomForecasts,
			TenMoreUsers,
			ResolveQFTeamNames, ResolveSFTeamNames, ResolveFinalTeamNames
		)
	}
	
	def getScenariosByCategory(): Map[String, Seq[Scenario]] = {
		getScenarios().groupBy(_.category)
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

object ResetTeamNames extends CleanScenario {
	val description = "Reset team names AS WELL AS RESULTS for non-group stage matches"
	def apply() = {
		Match.findAll().filterNot(_.isGroupStageMatch)
			.foreach { m =>
				Match.clearResult(m.id.get)
				Match.setTeamNames(m.id.get, null, null)
			}
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
	val description = "Reset all DB changes (match results, team names, forecasts, users)"
	def apply() = {
		ResetMatchResults.apply()
		ResetTeamNames.apply()
		ResetForecasts.apply()
		ResetUsers.apply()
	}
}

// ~~~~~~~~~~~~~~~~~~~~~ User Scenarios ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

protected trait UserScenario extends UndoableScenario {
	val category = "User"
} 
object TenMoreUsers extends UserScenario {
	val description = "Create ten more users (totoXXX)"
	def apply() = {
		val startIndex = findHigherTotoIndex + 1
		(startIndex until startIndex+10).foreach(i => User.create(User("toto"+i, "toto"+i+"@gmail.com", "welcome1")))
	}
	def unapply() = {
		val startIndex = findHigherTotoIndex
		(startIndex until startIndex-10 by -1).foreach(i => User.remove("toto"+i))
	}
	def findHigherTotoIndex = {
		val regex = """toto(\d+)""".r
		// find higher toto user index
		User.findAll().foldLeft(0) { (index: Int, user:User) =>
			regex.findFirstMatchIn(user.name).map { m =>
				val userIndex = m.group(1).toInt
				if (userIndex > index) userIndex else index
			}.getOrElse(index)
		}
	}
}

// ~~~~~~~~~~~~~~~~~~~~~ Match Scenarios ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

trait MatchResultScenario extends UndoableScenario {
	val category = "Match"

	/** generate random result (with penalties for draws) */
	protected def setRandomResult(matchId: Long) {
		val rslt = Match.generateRandomResult()
		val penaltiesScore = if (rslt._1 == rslt._2) { Some(Match.generateRandomPenalties()) } else { None }
		Match.updateResult(matchId, rslt._1, rslt._2, penaltiesScore)
	}
}
object MD1Results extends MatchResultScenario {
	val description = "Set match results for MD1 phase"
	def apply() = {
		Match.updateResult( 1, 1, 0, None)
		Match.updateResult( 2, 1, 2, None)
		Match.updateResult( 3, 1, 1, None)
		Match.updateResult( 4, 3, 1, None)
		Match.updateResult( 5, 0, 0, None)
		Match.updateResult( 6, 3, 4, None)
		Match.updateResult( 7, 2, 1, None)
		Match.updateResult( 8, 1, 2, None)
	}
	def unapply() = {
		(1 to 8).foreach(Match.clearResult(_))
	}
}

object MD2Results extends MatchResultScenario {
	val description = "Set match results for MD2 phase"
	def apply() = {
		Match.updateResult( 9, 1, 0, None)
		Match.updateResult(10, 1, 2, None)
		Match.updateResult(11, 1, 1, None)
		Match.updateResult(12, 3, 1, None)
		Match.updateResult(13, 0, 0, None)
		Match.updateResult(14, 3, 4, None)
		Match.updateResult(15, 2, 1, None)
		Match.updateResult(16, 1, 2, None)
	}
	def unapply() = {
		(9 to 16).foreach(Match.clearResult(_))
	}
}

object MD3Results extends MatchResultScenario {
	val description = "Set match results for MD3 phase"
	def apply() = {
		Match.updateResult(17, 1, 0, None)
		Match.updateResult(18, 1, 2, None)
		Match.updateResult(19, 1, 1, None)
		Match.updateResult(20, 3, 1, None)
		Match.updateResult(21, 0, 0, None)
		Match.updateResult(22, 3, 4, None)
		Match.updateResult(23, 2, 1, None)
		Match.updateResult(24, 1, 2, None)
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

object RandomGroupStageResults extends MatchResultScenario {
	val description = "Set random match results for group stages (MD1, MD2, MD3)"
	def apply() = {
		(1 to 24).foreach(Match.updateResult(_, Match.generateRandomScore(), Match.generateRandomScore(), None))
	}
	def unapply() = {
		(1 to 24).foreach(Match.clearResult(_))
	}
}

object RandomQFResults extends MatchResultScenario {
	val description = "Set random match results for Quarter Finals"
	def apply() = {
		(25 to 28).foreach(setRandomResult(_))
	}
	def unapply() = {
		(25 to 28).foreach(Match.clearResult(_))
	}
}

object RandomSFResults extends MatchResultScenario {
	val description = "Set random match results for Semi Finals"
	def apply() = {
		(29 to 30).foreach(setRandomResult(_))
	}
	def unapply() = {
		(29 to 30).foreach(Match.clearResult(_))
	}
}

object RandomFinalResult extends MatchResultScenario {
	val description = "Set random match result for the Final"
	def apply() = {
		setRandomResult(31)
	}
	def unapply() = {
		Match.clearResult(31)
	}
}

object RandomResults extends MatchResultScenario {
	val description = "Set random results for *ALL* matches"
	def apply() = {
		(1 to 31).foreach(setRandomResult(_))
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

// ~~~~~~~~~~~~~~~~~~~~~ Tool Scenarios ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

trait ToolScenario extends Scenario {
	val category = "Tool"
}

trait ResolveTeamNames extends ToolScenario with UndoableScenario {
	val phase: Phase.Phase

	/** subclasses must return None if ok or the reason why the precondition is not met */
	def precondition(matches: Seq[Match]): Option[String]
	
	def apply() = {
		val matches = Match.findAll()
		matches.filter(_.phase == phase).foreach { zmatch =>
			assert(zmatch.teamAformula.isDefined, "teamAformula is not defined !")
			assert(zmatch.teamBformula.isDefined, "teamBformula is not defined !")
			val rslt = precondition(matches)
			assert(rslt == None, rslt.get)
			val teamAOpt = Match.formulaTeamLookup(zmatch.teamAformula.get, matches)
			val teamBOpt = Match.formulaTeamLookup(zmatch.teamBformula.get, matches)
			teamAOpt match {
				case Some(teamA) => teamBOpt match {
					case Some(teamB) => Match.setTeamNames(zmatch.id.get, teamA, teamB)
					case None => println("could not determine teamB from formula " + zmatch.teamBformula.get)
				}
				case None => println("could not determine teamA from formula " + zmatch.teamAformula.get)
			}
				
		}
	}
	def unapply() = {
		val matches = Match.findAll()
		matches.filter(_.phase == phase).foreach { zmatch =>
			Match.setTeamNames(zmatch.id.get, null, null)
		}
	}
}

object ResolveQFTeamNames extends ResolveTeamNames {
	val description = "Resolve team names for Quarter finals based on group results"
	val phase = Phase.QUARTERFINALS
	def precondition(matches: Seq[Match]): Option[String] = {
		// check that ALL group stage matches have been played
		if (matches.filter(_.isGroupStageMatch).forall(_.played))
			None
		else
			Some("Not all group stage matches have been played")
	}
}

object ResolveSFTeamNames extends ResolveTeamNames {
	val description = "Resolve team names for Semi finals based QF matches results"
	val phase = Phase.SEMIFINALS
	def precondition(matches: Seq[Match]): Option[String] = {
		// check that all Quarter finals have been played
		if (matches.filter(_.phase == Phase.QUARTERFINALS).forall(_.played))
			None
		else
			Some("Not all Quarter Finals matches have been played")
	}
}

object ResolveFinalTeamNames extends ResolveTeamNames {
	val description = "Resolve team names for the Final based SF matches results"
	val phase = Phase.FINAL
	def precondition(matches: Seq[Match]): Option[String] = {
		// check that all Semi finals have been played
		if (matches.filter(_.phase == Phase.SEMIFINALS).forall(_.played))
			None
		else
			Some("Not all Semi Finals matches have been played")
	}
}
