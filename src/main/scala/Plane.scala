package zzz.akka.avionics

import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import akka.actor.SupervisorStrategy._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._


object Plane{
  case object GiveMeControl
  case class Controls(controls: ActorRef)

  def apply() = new Plane with AltimeterProvider with PilotProvider with LeadFlightAttendantProvider
}

class Plane extends Actor with ActorLogging{
  this: AltimeterProvider with PilotProvider with LeadFlightAttendantProvider =>
  import Altimeter._
  import Plane._
  import IsolatedLifeCycleSupervisor._

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
//          context.actorOf(Props(newAutopilot), "AutoPilot")
          context.actorOf(Props(new ControlSurfaces(alt)), "ControlSurfaces")
        }
    }), "Equipment")
    Await.result(controls ? WaitForStart, 1.second)
  }

  def startPeople() {
    val plane = self
    val controls = actorForControls("ControlSurfaces")
    val autopilot = actorForControls("AutoPilot")
    val altimeter = actorForControls("Altimeter")
    val people = context.actorOf(Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
      def childStarter() {
          context.actorOf(Props(newCoPilot(plane, altimeter)), copilotName)
          context.actorOf(Props(newPilot(plane, controls, altimeter)), pilotName)
        }
    }), "Pilots")
    // Use the default strategy here, which
    // restarts indefinitely
    context.actorOf(Props(newFlightAttendant), attendantName)
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
      log.info("Plane giving control")
      sender ! Controls(actorForControls("ControlSurfaces"))
    case AltitudeUpdate(altitude) =>
      log.info("Altitude is now: " + altitude)
  }

}
