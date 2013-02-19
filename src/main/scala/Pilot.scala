package zzz.akka.avionics

import akka.actor.{Actor, ActorRef}

object Pilots{ 
  case object ReadyToGo
  case object RelinquishControl
}

class Pilot extends Actor{ 
  import Pilots._
  import Plane._

  var controls: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString(
  "zzz.akka.avionics.flightcrew.copilotName")
  def receive = {
    case ReadyToGo =>
      context.parent ! Plane.GiveMeControl
      copilot = context.actorFor("../" + copilotName)
      autopilot = context.actorFor("../AutoPilot")

    //code from book doesn't compile. Using Stig's solution for this
/*
    case Controls(controlSurfaces) =>
      controls = controlSurfaces
 */
    case RelinquishControl => 
      controls = context.system.deadLetters
  }
}

class CoPilot extends Actor { 
  import Pilots._
  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString(
  "zzz.akka.avionics.flightcrew.pilotName")
  def receive = { 
    case ReadyToGo =>
      pilot = context.actorFor("../" + pilotName)
      autopilot = context.actorFor("../AutoPilot")
  }
}
