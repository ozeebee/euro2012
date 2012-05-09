package utils

import play.api.libs.json._
import play.api.libs.json.Json._

/**
 * Utility object for Server-Sent Events message format
 */
object SSE {
	def formatResultScore(scoreA: Int, scoreB: Int, penaltiesScore: Option[(Int, Int)]): String = {
		var map = Map(
				"scoreA" -> toJson(scoreA),
				"scoreB" -> toJson(scoreB)
		)
		penaltiesScore.foreach(p =>
			map += ("penaltyScoreA" -> toJson(p._1), "penaltyScoreB" -> toJson(p._2))
		)
		val jsonObj = Json.toJson(map)
		val jsonStr = Json.stringify(jsonObj)
		//println("jsonStr = " + jsonStr)
		//val data = jsonStr.split("\n").map("data: " + _ + "\n").mkString + "\n"
		"data: " + jsonStr + "\n\n"
	}
}