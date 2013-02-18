package zzz.akka.avionics

import  akka.actor.{Props,  Actor,  ActorLogging}
object  Plane{
  case  object  GiveMeControl
}

class Plane extends Actor with ActorLogging{
  import Altimeter._
  import Plane._
  import EventSource._

  val altimeter = context.actorOf(Props[Altimeter])
  val controls  = context.actorOf(Props(new ControlSurfaces(altimeter)))

  override def preStart() {
    altimeter ! RegisterListener(self)
  }

  def receive = {
    case GiveMeControl =>
      log.info("Plane giving control")
      sender ! controls
    case AltitudeUpdate(altitude) =>
      log.info("Altitude is now: " + altitude)
  }

}