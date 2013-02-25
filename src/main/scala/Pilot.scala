package zzz.akka.avionics

import akka.actor.{Actor, ActorRef, FSM, Props}

object Pilots{ 
  case object ReadyToGo
  case object RelinquishControl
}

object Pilot {
  import FlyingBehaviour._
  import ControlSurfaces._
  val tipsyCalcElevator: Calculator  = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickForward(amt * 1.03f)
      case StickBack(amt) => StickBack(amt * 1.03f)
      case m => m
    }
  }
  val tipsyCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickLeft(amt * 1.03f)
      case StickRight(amt) => StickRight(amt * 1.03f)
      case m => m
    } 
  }
  val zaphodCalcElevator: Calculator = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickBack(1f)
      case StickBack(amt) => StickForward(1f)
      case m => m
    } 
  }
  val zaphodCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickRight(1f)
      case StickRight(amt) => StickLeft(1f)
      case m => m
    } 
  }
}

class Pilot (plane: ActorRef,
  heading: ActorRef,
  altimeter: ActorRef) extends Actor{ this: DrinkingProvider with FlyingProvider => 
  import Pilots._
  import Pilot._
  import Plane._
  import Altimeter._
  import ControlSurfaces._
  import DrinkingBehaviour._
  import FlyingBehaviour._
  import FSM._

  var copilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.copilotName")

  def setCourse(flyer: ActorRef) {
    flyer ! Fly(CourseTarget(20000, 250, System.currentTimeMillis + 30000))
  }

  override def preStart() {
    context.actorOf(newDrinkingBehaviour(self), "DrinkingBehaviour")
    context.actorOf(newFlyingBehaviour(plane, heading, altimeter), "FlyingBehaviour")
  }

  def bootstrap: Receive = {
    case ReadyToGo =>
      val copilot = context.actorFor("../" + copilotName)
      val flyer = context.actorFor("FlyingBehaviour")
      flyer ! SubscribeTransitionCallBack(self)
      setCourse(flyer)
      context.become(sober(copilot, flyer))
  }

  def sober(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => // We're already sober
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  def tipsy(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => // We're already tipsy
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
}

  def zaphod(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => // We're already Zaphod
  }
  def idle: Receive = {
    case _ => }
  def becomeSober(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(calcElevator)
    flyer ! NewBankCalculator(calcAilerons)
    context.become(sober(copilot, flyer))
  }

  def becomeTipsy(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(tipsyCalcElevator)
    flyer ! NewBankCalculator(tipsyCalcAilerons)
    context.become(tipsy(copilot, flyer))
  }

  def becomeZaphod(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(zaphodCalcElevator)
    flyer ! NewBankCalculator(zaphodCalcAilerons)
    context.become(zaphod(copilot, flyer))
  }

    override def unhandled(msg: Any): Unit = {
      msg match {
        case Transition(_, _, Flying) =>
          setCourse(sender)
        case Transition(_, _, Idle) =>
          context.become(idle)
        // Ignore these two messages from the FSM rather than
        // have them go to the log
        case Transition(_, _, _) =>
        case CurrentState(_, _) =>
        case m => super.unhandled(m)
      }
    }

    def receive = bootstrap //initial state


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
  def newPilot(plane: ActorRef, controls: ActorRef, altimeter: ActorRef): Actor = new Pilot(plane, controls, altimeter) with DrinkingProvider with FlyingProvider
  def newCoPilot(plane: ActorRef, altimeter: ActorRef): Actor = new CoPilot(plane, altimeter)
//  def autopilot: Actor = new AutoPilot
}
trait DrinkingProvider {
  def newDrinkingBehaviour(drinker: ActorRef): Props = Props(DrinkingBehaviour(drinker))
}
trait FlyingProvider {
  def newFlyingBehaviour(plane: ActorRef,
    heading: ActorRef,
    altimeter: ActorRef): Props = Props(new FlyingBehaviour(plane, heading, altimeter))
}
