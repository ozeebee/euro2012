import play.api._
import play.api.mvc._

object Global extends GlobalSettings {

	override def onStart(app: Application) {
		InitialData.insert()
	}

	object InitialData {
		def date(str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)

		def insert() = {
		}
	}

	/*
	override def onRouteRequest(request: RequestHeader): Option[Handler] = {
		//println("executed before every request:" + request.toString)
		super.onRouteRequest(request).map {
			case action: Action[_] => Action(action.parser) { request =>
				val uuidDefined = request.session.get("uuid").isDefined
				if (uuidDefined)
					println("uuid = " + request.session.get("uuid").get)
				//println("  is uuid defined ? " + generateUuid)
				/*val tmp =*/ action(request) match {
					// add uuid to session if not already present
					case result: PlainResult if !uuidDefined => result.withSession("uuid" -> genUUID()) 
					case other => other
				}
				//println("after action invocation")
				//tmp
			}
			case other => println("other " + other); other
		}
	}
	
	def genUUID(): String = {
		java.util.UUID.randomUUID().getMostSignificantBits().toString()
	}
	*/
}
