package zzz.akka.avionics

import akka.actor.{Actor, ActorRef}

object ControlSurfaces{
  //amount [-1,1]
  case class StickBack(amount: Float)
  case class StickForward(amount: Float)
  case class StickLeft(amount: Float)
  case class StickRight(amount: Float)
  case class HasControl(somePilot: ActorRef)
}

class ControlSurfaces(plane: ActorRef, altimeter: ActorRef, heading: ActorRef) extends Actor{
  import ControlSurfaces._
  import Altimeter._
  import HeadingIndicator._

  def receive = controlledBy(context.system.deadLetters)

  def controlledBy(somePilot: ActorRef): Receive = {
    case StickBack(amount)    if sender == somePilot => altimeter ! RateChange(amount)
    case StickForward(amount) if sender == somePilot => altimeter ! RateChange(-1 * amount)
    case StickLeft(amount)    if sender == somePilot => heading ! RateChange(-1 * amount)
    case StickRight(amount)   if sender == somePilot => heading ! RateChange(amount)
    case HasControl(entity)   if sender == plane => context.become(controlledBy(entity))
}

}
