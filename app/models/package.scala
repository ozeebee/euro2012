import scala.xml.Node

package object models {

	trait ModelObject[T] {
		val DATA_DIR = System.getProperty("user.home") + "/Dev/Workspaces/AJO/PersoProjects/euro2012/data"
			
		def fromXml(xml: Node): T
	}
	
}