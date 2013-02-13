package zzz.akka.avionics

import akka.actor.{Props, Actor, ActorSystem, ActorLogging}
import scala.concurrent.duration._
//  The  Scheduler  needs  an  execution  context  -  we'll  just  use  the  global  one
import  scala.concurrent.ExecutionContext.Implicits.global


object Altimeter{
  case class RateChange(amount: Float) //sent to Altimeter
}

class Altimeter extends Actor with ActorLogging{
  import Altimeter._

  val ceiling = 43000       //feet
  val maxRateOfClimb = 5000 //feet per minute
  var rateOfClimb: Float = 0 //varying rate based on movement of the stick
  var altitude: Double = 0
  var lastTick = System.currentTimeMillis() //used to figure out how much time has passed
  var ticker =  context.system.scheduler.schedule(100.millis, 100.millis, self, Tick)

  //internal message used to tell us to update altitude
  case object Tick

  def receive = {
    case RateChange(amount) => //bound by [-1,]
      rateOfClimb = amount.min(1.0f).max(-1.0f) * maxRateOfClimb
      log.info(s"Altimeter changed rate of climb to $rateOfClimb")
    case Tick =>
      val tick = System.currentTimeMillis()
      altitude = altitude + rateOfClimb * ((tick - lastTick) / 60000.0)
      lastTick = tick
  }

  //Kill ticker when we stop
  override def postStop(): Unit = ticker.cancel
}