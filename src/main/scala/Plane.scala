package zzz.akka.avionics

import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import akka.actor.SupervisorStrategy._
import akka.pattern.ask
import akka.util.Timeout
import akka.routing.FromConfig
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Success, Failure}

import  scala.concurrent.ExecutionContext.Implicits.global

object Plane{
  case object GiveMeControl
  case object LostControl //this is used without definition or explanation in the text
  case object GetCurrentHeading
  case object GetCurrentAltitude

  case class Controls(controls: ActorRef)

  def apply() = new Plane with AltimeterProvider with PilotProvider with LeadFlightAttendantProvider with HeadingIndicatorProvider
}

class Plane extends Actor with ActorLogging{
  this: AltimeterProvider with PilotProvider with LeadFlightAttendantProvider with HeadingIndicatorProvider =>
  import Altimeter._
  import Plane._
  import IsolatedLifeCycleSupervisor._
  import Altimeter.CurrentAltitude

  val cfgstr = "zzz.akka.avionics.flightcrew"
  val config = context.system.settings.config
  val pilotName = config.getString(s"$cfgstr.pilotName")
  val copilotName = config.getString(s"$cfgstr.copilotName")
  val attendantName = config.getString(s"$cfgstr.leadAttendantName")

  implicit val askTimeout = Timeout(1.second)

  def actorForControls(name: String) = context.actorFor("Equipment/" + name)
  def actorForPilots(name: String) = context.actorFor("Pilots/" + name)

  def startEquipment() {
    val controls = context.actorOf(Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
      def childStarter() {
        val alt = context.actorOf(Props(newAltimeter), "Altimeter")
        val head = context.actorOf(Props(newHeadingIndicator), "HeadingIndicator")
//          context.actorOf(Props(newAutopilot), "AutoPilot")
          context.actorOf(Props(new ControlSurfaces(self, alt, head)), "ControlSurfaces")
        }
    }), "Equipment")
    Await.result(controls ? WaitForStart, 1.second)
  }

  def startPeople() {
    val plane = self
    val controls = actorForControls("ControlSurfaces")
//    val autopilot = actorForControls("AutoPilot")
    val altimeter = actorForControls("Altimeter")
    val leadAttendant = context.actorOf(Props(newFlightAttendant).withRouter(FromConfig()),"LeadFlightAttendant")
    val people = context.actorOf(Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
      def childStarter() {
          context.actorOf(Props(PassengerSupervisor(leadAttendant)), "Passengers")
          context.actorOf(Props(newCoPilot(plane, altimeter)), copilotName)
          context.actorOf(Props(newPilot(plane, controls, altimeter)), pilotName)
        }
    }), "Pilots")
    // Use the default strategy here, which
    // restarts indefinitely
//    context.actorOf(Props(newFlightAttendant), attendantName)
    Await.result(people ? WaitForStart, 1.second)
  }

  override def preStart() {
    import EventSource.RegisterListener
    import Pilots.ReadyToGo

    startEquipment()
    startPeople()

    actorForControls("Altimeter") ! RegisterListener(self)
    actorForPilots(pilotName) ! ReadyToGo
    actorForPilots(copilotName) ! ReadyToGo
  }

  def receive = {
    case GiveMeControl =>
      log.info(s"Plane giving control to ${sender.path.name}, ${sender.toString}")
      sender ! actorForControls("ControlSurfaces")
    case AltitudeUpdate(altitude) =>
//      log.info("Altitude is now: " + altitude)
    case GetCurrentHeading =>
      log.info("Heading request")
    case GetCurrentAltitude =>
      val destinedFor = sender
      (actorForControls("Altimeter") ? GetCurrentAltitude).mapTo[CurrentAltitude].onComplete {
        case Success(CurrentAltitude(altitude)) => 
          log.info(s"Sending CurrentAltitude(${altitude}) to ${destinedFor}")
          destinedFor ! CurrentAltitude(altitude)
        case Success(m) => log.info(s"Bogus message returned from altimeter: ${m}")
        case Failure(m) => log.info(s"Failed altitude request: ${m}")
      }

      
  }

}
