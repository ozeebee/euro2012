package models

import scala.xml.Elem
import scala.xml.Node

case class Team(
	id: String,
	name: String,
	group: String
)

object Team extends ModelObject[Team] {
	
	def findAll(): Seq[Team] = {
		println("datadir = " + DATA_DIR)
		val teamsElem = xml.XML.loadFile(DATA_DIR + "/teams.xml")
		(teamsElem \ "team") map { node =>
			fromXml(node)
		}	
	}
	
	def fromXml(xml: Node): Team = {
		new Team (
			id = (xml \ "id").text,
			name = (xml \ "name").text,
			group = (xml \ "group").text
		)
	}
}