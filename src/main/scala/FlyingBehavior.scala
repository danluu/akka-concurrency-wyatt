package zzz.akka.avionics

import akka.actor.{ActorRef, Actor, FSM}
import scala.concurrent.duration._

object FlyingBehaviour {
  import ControlSurfaces._

  sealed trait State
  case object Idle extends State
  case object Flying extends State
  case object PreparingToFly extends State

  case class CourseTarget(altitude: Double, heading: Float, byMillis: Long)
  case class CourseStatus(altitude: Double, heading: Float, headingSinceMS: Long, altitudeSinceMS: Long)
  case class NewElevatorCalculator(f: Calculator)
  case class NewBankCalculator(f: Calculator)

  type Calculator = (CourseTarget, CourseStatus) => Any

  sealed trait Data
  case object Unititialized extends Data
  case class FlightData(controls: ActorRef, elevCalc: Calculator, bankCalc: Calculator, target: CourseTarget, status: CourseStatus) extends Data

  case class Fly(target: CourseTarget)

  def currentMS = System.currentTimeMillis

  def calcElevator(target: CourseTarget, status: CourseStatus): Any = {
    val alt = (target.altitude - status.altitude).toFloat
    val dur = target.byMillis - status.altitudeSinceMS
    if (alt < 0) StickForward((alt/dur) * -1)
    else StickBack(alt / dur)
  }

  def calcAilerons(target: CourseTarget, status: CourseStatus): Any = {
    import scala.math.{abs,signum}
    val diff = target.heading - status.heading
    val dur = target.byMillis - status.headingSinceMS
    val amount = if (abs(diff) < 180) diff
                 else signum(diff) * (abs(diff) - 360f)

    if (amount > 0) StickRight(amount/dur)
    else StickLeft((amount / dur) * -1)
  }
}

class FlyingBehaviour(plane: ActorRef, heading: ActorRef, altimeter: ActorRef) extends Actor
    with FSM[FlyingBehaviour.State, FlyingBehaviour.Data] {
  import FSM._
  import FlyingBehaviour._
  import Pilots._
  import Plane._
  import Altimeter._
  import HeadingIndicator._
  import EventSource._

  case object Adjust

  def adjust(flightData: FlightData): FlightData = {
    val FlightData(c, elevCalc, bankCalc, t, s) = flightData
    c ! elevCalc(t, s)
    c ! bankCalc(t, s)
    flightData
  }

  startWith(Idle, Unititialized)

  when(Idle) {
    case Event(Fly(target), _) => goto(PreparingToFly) using 
      FlightData(context.system.deadLetters, calcElevator, calcAilerons, target, CourseStatus(-1, -1, 0, 0))
  }
  onTransition{
    case Idle -> PreparingToFly =>
      plane ! GiveMeControl
      heading ! RegisterListener(self)
      altimeter ! RegisterListener(self)
  }
  when(PreparingToFly, stateTimeout = 5.seconds)(transform{
    case Event(HeadingUpdate(head), d: FlightData) =>
        stay using d.copy(status = d.status.copy(heading = head, headingSinceMS = currentMS))
    case Event(AltitudeUpdate(alt), d: FlightData) =>
        stay using d.copy(status = d.status.copy(altitude = alt, headingSinceMS = currentMS))
    case Event(Controls(ctrls), d: FlightData) =>
      stay using d.copy(controls = ctrls)
    case Event(StateTimeout, _) => 
      plane ! LostControl
      goto(Idle)
  } using {
    case s if prepComplete(s.stateData) => s.copy(stateName = Flying)
  })

  def prepComplete(data: Data): Boolean = {
    data match {
      case FlightData(c, _, _, _, s) =>
        if (!c.isTerminated &&
          s.heading != -1f && s.altitude != -1f)
          true
        else false
      case _ =>
        false
    } 
  }

  onTransition{
    case PreparingToFly -> Flying => setTimer("Adjustment", Adjust, 200.milliseconds, repeat = true)
  }

  when(Flying){
    case Event(AltitudeUpdate(alt), d: FlightData) =>
      stay using d.copy(status = d.status.copy(altitude = alt, altitudeSinceMS = currentMS))
    case Event(HeadingUpdate(head), d: FlightData) =>
      stay using d.copy(status = d.status.copy(heading = head, headingSinceMS = currentMS))
    case Event(Adjust, flightData: FlightData) => stay using adjust(flightData)
    case Event(NewBankCalculator(f), d: FlightData) =>
      stay using d.copy(bankCalc = f)
    case Event(NewElevatorCalculator(f), d: FlightData) =>
      stay using d.copy(elevCalc = f)
  }

  onTransition{
    case Flying -> _ => cancelTimer("Adjustment")
  }

  onTransition {
    case _ -> Idle =>
      heading ! UnregisterListener(self)
      altimeter ! UnregisterListener(self)
  }

  whenUnhandled{
    case Event(RelinquishControl, _) => goto(Idle)
  }

  initialize
}













