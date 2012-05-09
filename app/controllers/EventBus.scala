package controllers

import akka.actor._

import play.api._
import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Enumerator._

import play.api.Play.current

/**
 * Object that encapsulates actors used to asynchronously send events (for instance with SSE (Server-Sent Events))
 */
object EventBus {
	lazy val default = Akka.system.actorOf(Props[EventBus])
	
	// public api
	
	def addEventSource(uuid: String, pushee: Pushee[String]) = {
		default ! AddEventSource(uuid, pushee)
	}
	
	def closeEventSource(uuid: String) = {
		default ! CloseEventSource(uuid)
	}
	
	def publish(event: String, data: String = ""): Unit = {
		default ! Publish(Some(event), data)
	} 

	def publishRaw(event: String, data: String = ""): Unit = {
		default ! Publish(Some(event), data, true)
	} 
	
	// main actor
	class EventBus extends Actor {
		val logger = Logger(this.getClass())
		
		//var eventSourcePushee: Option[Pushee[String]] = None
		var pushees: Map[String, Pushee[String]] = Map.empty
		
		override def preStart() = {
			logger.debug("EventBus actor started")
		}
		
		override def postStop() = {
			logger.debug("EventBus actor stopped")
		}
		
		def receive = {
			case AddEventSource(uuid, pushee) => 
				pushees = pushees + (uuid -> pushee)
				logger.debug("add EventSource pushee for uuid %s, clients size = %d" format (uuid, pushees.size))
			case CloseEventSource(uuid) => 
				pushees = pushees - uuid
				logger.debug("close EventSource pushee for uuid %s, clients size = %d" format (uuid, pushees.size))
			case Publish(event, data, isRawData) => 
				logger.debug("publish event=%s,data=%s to %d clients" format (event, data, pushees.size))
				pushees foreach { case (uuid, pushee) =>
					event foreach (e => pushee.push("event: " + e + "\n"))
					if (isRawData)
						pushee.push(data)
					else
						pushee.push("data: " + data + "\n\n")
				}
			case _ => logger.debug("unknown message received") 
		}
	}

	// messages
	case class AddEventSource(uuid: String, pushee: Pushee[String])
	case class CloseEventSource(uuid: String)
	case class Hello()
	case class Publish(event: Option[String], data: String, isRawData: Boolean = false)
}