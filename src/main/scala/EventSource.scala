package zzz.akka.avionics

import akka.actor.{Actor, ActorRef}
import java.util


//object EventSource{
object EventSource{
  case class RegisterListener(listener: ActorRef)
  case class UnregisterListener(listener: ActorRef)
}

trait EventSource{
  def sendEvent[T](event: T): Unit
  def eventSourceReceive: Actor.Receive
}

trait ProductionEventSource extends EventSource { this: Actor =>

  import EventSource._

  var listeners = scala.collection.immutable.Vector.empty[ActorRef]
  def sendEvent[T](event: T): Unit = listeners.foreach{ _ ! event}

  def eventSourceReceive: Receive = {
    case RegisterListener(listener)   => listeners = listeners :+ listener
    case UnregisterListener(listener) => listeners = listeners.filter{_ != listener}
  }
}
