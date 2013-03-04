package zzz.akka.avionics

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging}

object HeadingIndicator{
  case class BankChange(amount: Float)
  case class HeadingUpdate(heading: Float)
  case class CurrentHeading(heading: Float)
  def apply() = new HeadingIndicator with ProductionEventSource
}

trait HeadingIndicator extends Actor with ActorLogging with StatusReporter { this: EventSource =>
  import StatusReporter._
  import HeadingIndicator._
  import context._
  import Plane.GetCurrentHeading

  def currentStatus = StatusOK

  case object Tick //internal message

  val maxDegPerSec = 5
  val ticker = system.scheduler.schedule(100.millis, 100.millis, self, Tick)
  var lastTick: Long = System.currentTimeMillis
  var rateOfBank = 0f
  var heading = 0f

  def headingIndicatorReceive: Receive = {
    case BankChange(amount) => rateOfBank = amount.min(1.0f).max(-1.0f)
    case Tick =>
      val tick = System.currentTimeMillis
      val timeDelta = (tick - lastTick) / 1000f
      val degs = rateOfBank * maxDegPerSec
      heading = (heading + (360 + (timeDelta * degs))) % 360
      lastTick = tick
      sendEvent(HeadingUpdate(heading))
    case GetCurrentHeading =>
      sender ! CurrentHeading(heading)
  }

  def receive = statusReceive orElse  eventSourceReceive orElse headingIndicatorReceive
  override def postStop(): Unit = ticker.cancel
}

trait HeadingIndicatorProvider{
  def newHeadingIndicator: Actor = HeadingIndicator()
}
