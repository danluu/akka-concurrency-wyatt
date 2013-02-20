package zzz.akka.avionics

import  akka.actor.{Props,  Actor,  ActorLogging}

object  Plane{
  case  object  GiveMeControl
}

class Plane extends Actor with ActorLogging{
  import Altimeter._
  import Plane._
  import EventSource._

  val cfgstr = "zzz.akka.avionics.flightcrew"
  val altimeter = context.actorOf(Props(Altimeter()), "Altimeter")
  val controls  = context.actorOf(Props(new ControlSurfaces(altimeter)))
  val config = context.system.settings.config
  val pilot = context.actorOf(Props[Pilot], config.getString(s"$cfgstr.pilotName"))
  val copilot = context.actorOf(Props[CoPilot], config.getString(s"$cfgstr.copilotName"))
//  val autopilot = context.actorOf(Props[AutoPilot], "AutoPilot"))
  val flightattendant = context.actorOf(Props(LeadFlightAttendant()),config.getString(s"$cfgstr.leadAttendantName"))

  override def preStart() {
    altimeter ! RegisterListener(self)
    List(pilot,copilot).foreach{_ ! Pilots.ReadyToGo}
  }

  def receive = {
    case GiveMeControl =>
      log.info("Plane giving control")
      sender ! controls
    case AltitudeUpdate(altitude) =>
      log.info("Altitude is now: " + altitude)
  }

}
