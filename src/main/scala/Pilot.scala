package zzz.akka.avionics

import akka.actor.{Actor, ActorRef}

object Pilots{ 
  case object ReadyToGo
  case object RelinquishControl
}

class Pilot (plane: ActorRef,
  var controls: ActorRef,
  altimeter: ActorRef) extends Actor{
  import Pilots._
  import Plane._

  var copilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString(
  "zzz.akka.avionics.flightcrew.copilotName")
  def receive = {
    case ReadyToGo =>
      context.parent ! Plane.GiveMeControl
      copilot = context.actorFor("../" + copilotName)
      autopilot = context.actorFor("../AutoPilot")
    case Controls(controlSurfaces) =>
      controls = controlSurfaces


  }
}

class CoPilot (plane: ActorRef, altimeter: ActorRef) extends Actor { 
  import Pilots._
  import akka.actor.Terminated
  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString(
  "zzz.akka.avionics.flightcrew.pilotName")
  def receive = { 
    case ReadyToGo =>
      pilot = context.actorFor("../" + pilotName)
      context.watch(pilot)
      autopilot = context.actorFor("../AutoPilot")
    case Terminated(_) =>
      // Pilot died
      plane ! Plane.GiveMeControl
  }
}

trait PilotProvider{ 
  def newPilot(plane: ActorRef, controls: ActorRef, altimeter: ActorRef): Actor = new Pilot(plane, controls, altimeter)
  def newCoPilot(plane: ActorRef, altimeter: ActorRef): Actor = new CoPilot(plane, altimeter)
//  def autopilot: Actor = new AutoPilot
}
