package zzz.akka.avionics

import akka.actor._
import scala.concurrent.duration._

trait AirportSpecifics {
  lazy val headingTo: Float = 0.0f
  lazy val altitude: Double = 0
}

object Airport {
  case class DirectFlyerToAirport(flyingBehaviour: ActorRef)
  case class StopDirectingFlyer(flyingBehaviour: ActorRef)
  def toronto(): Props = Props(new Airport with BeaconProvider with AirportSpecifics {
    override lazy val headingTo: Float = 314.3f
    override lazy val altitude: Double = 26000
  })
}

class Airport extends Actor {
  this: AirportSpecifics with BeaconProvider =>
  import Airport._
  import Beacon._
  import FlyingBehaviour._
  import GenericPublisher._

  val beacon = context.actorOf(Props(newBeacon(headingTo)), "Beacon")
  def receive = {
    case DirectFlyerToAirport(flyingBehaviour) =>
      val when = (1 hour fromNow).time.toMillis
      context.actorOf(Props(
        new MessageTransformer(from = beacon,
          to = flyingBehaviour, {
            case BeaconHeading(heading) =>
              Fly(CourseTarget(altitude, heading, when))
          })))
    case StopDirectingFlyer(_) =>
      context.children.foreach { context.stop }
  }
}

class MessageTransformer(from: ActorRef, to: ActorRef, transformer: PartialFunction[Any, Any]) extends Actor {
  import GenericPublisher._
  override def preStart() {
    from ! RegisterListener(self)
  }
  override def postStop() {
    from ! UnregisterListener(self)
  }

  def receive = {
    case m => to forward transformer(m)
  }
}
