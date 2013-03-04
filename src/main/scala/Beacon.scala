package zzz.akka.avionics

import akka.actor._
import scala.concurrent.duration._

import  scala.concurrent.ExecutionContext.Implicits.global

trait BeaconResolution {
  lazy val beaconInterval = 1.second
}

trait BeaconProvider {
  def newBeacon(heading: Float) = Beacon(heading)
}

object GenericPublisher {
  case class RegisterListener(actor: ActorRef)
  case class UnregisterListener(actor: ActorRef)
}

object Beacon {
  case class BeaconHeading(heading: Float)
  def apply(heading: Float) = new Beacon(heading) with BeaconResolution
}

class Beacon(heading: Float) extends Actor { this: BeaconResolution =>
  import Beacon._
  import GenericPublisher._

  case object Tick
  val bus = new EventBusForActors[BeaconHeading, Boolean]({
    _: BeaconHeading => true
  })

  val ticker = context.system.scheduler.schedule(beaconInterval, beaconInterval, self, Tick)
  def receive = {
    case RegisterListener(actor) => bus.subscribe(actor, true)
    case UnregisterListener(actor) => bus.unsubscribe(actor)
    case Tick => bus.publish(BeaconHeading(heading))
  }
}
