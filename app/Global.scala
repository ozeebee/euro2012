import play.api._

object Global extends GlobalSettings {

	override def onStart(app: Application) {
		InitialData.insert()
	}

	object InitialData {
		def date(str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)

		def insert() = {
		}
	}
}
