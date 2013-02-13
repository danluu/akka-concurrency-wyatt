package zzz.akka.avionics

import  akka.actor.{Props,  Actor,  ActorLogging}
object  Plane{
  case  object  GiveMeControl
}

class Plane extends Actor with ActorLogging{
  import Altimeter._
  import Plane._

  val altimeter = context.actorOf(Props[Altimeter])
  val controls  = context.actorOf(Props(new ControlSurfaces(altimeter)))

  def receive = {
    case GiveMeControl =>
      log.info("Plane giving control")
      sender ! controls
  }

}