package zzz.akka.avionics

import akka.actor.{Actor, ActorRef, Props}

trait AttendantCreationPolicy{ 
  val numberOfAttendants: Int = 8
  def createAttendant: Actor = FlightAttendant()
}

trait LeadFlightAttendantProvider{ 
  def newFlightAttendant: Actor = LeadFlightAttendant()
}

object LeadFlightAttendant{
  case object GetFlightAttendant
  case class Attendant(a: ActorRef)
  def apply() = new LeadFlightAttendant with AttendantCreationPolicy
}

class LeadFlightAttendant extends Actor { this: AttendantCreationPolicy => 
  import LeadFlightAttendant._

  override def preStart(){ 
    import scala.collection.JavaConverters._
    val attendantNames = context.system.settings.config.getStringList(
    "zzz.akka.avionics.flightcrew.attendantNames").asScala
    attendantNames take numberOfAttendants foreach{i => context.actorOf(Props(createAttendant),i)}

  }

  def randomAttendant(): ActorRef = {context.children.take(
    scala.util.Random.nextInt(numberOfAttendants)+ 1).last}

  def receive = { 
    case GetFlightAttendant => sender ! Attendant(randomAttendant())
    case m => randomAttendant() forward m
  }

}

object FlightAttendantPathChecker{ 
  def main(args: Array[String]){ 
    val system = akka.actor.ActorSystem("PlaneSimulation")
    val lead = system.actorOf(Props(new LeadFlightAttendant with AttendantCreationPolicy), "LeadFlightAttendant")
    Thread.sleep(2000)
    system.shutdown()
  }
}


