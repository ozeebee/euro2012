import scala.xml.Node

package object models {

	trait ModelObject[T] {
		val DATA_DIR = System.getProperty("user.home") + "/Dev/Workspaces/AJO/PersoProjects/euro2012/data"
			
		def fromXml(xml: Node): T
	}

	object Phase extends Enumeration {
		type Phase = Value
		
		val MD1 = Value // Match Day 1
		val MD2 = Value // Match Day 2
		val MD3 = Value // Match Day 3
		val QUARTERFINALS = Value // Quarter Finals
		val SEMIFINALS = Value // Semi Finals
		val FINAL = Value // Final
	}
}