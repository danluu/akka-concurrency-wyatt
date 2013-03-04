package zzz.akka.avionics

import akka.actor._
import akka.agent.Agent
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import scala.collection.immutable.Queue

sealed abstract class Gender
case object Male extends Gender
case object Female extends Gender
case class GenderAndTime(gender: Gender, peakDuration: Duration, count: Int)

object Bathroom {
  sealed trait State
  case object Vacant extends State
  case object Occupied extends State
  sealed trait Data

  case class InUse(by: ActorRef, atTimeMillis: Long, queue: Queue[ActorRef]) extends Data
  case object NotInUse extends Data

  case object IWannaUseTheBathroom
  case object YouCanUseTheBathroomNow
  case class Finished(gender: Gender)

  private def updateCounter(male: Agent[GenderAndTime], female: Agent[GenderAndTime],
    gender: Gender, dur: Duration) {
    gender match {
      case Male => male send { c =>
        GenderAndTime(Male, dur.max(c.peakDuration),
          c.count + 1)
      }
      case Female => female send { c =>
        GenderAndTime(Female, dur.max(c.peakDuration),
          c.count + 1)
      }
    }
  }
}

class Bathroom(femaleCounter: Agent[GenderAndTime], maleCounter: Agent[GenderAndTime]) extends Actor
  with FSM[Bathroom.State, Bathroom.Data] {
  import Bathroom._
  startWith(Vacant, NotInUse)
  when(Vacant) {
    case Event(IWannaUseTheBathroom, _) =>
      log.info(s"Bathroom request from ${sender} granted (bathroom was empty)")
      sender ! YouCanUseTheBathroomNow
      goto(Occupied) using InUse(by = sender,
        atTimeMillis = System.currentTimeMillis,
        queue = Queue())
  }
  when(Occupied) {
    case Event(IWannaUseTheBathroom, data: InUse) =>
      log.info(s"${sender} added to bathroom queue")
      stay using data.copy(queue = data.queue.enqueue(sender))
    case Event(Finished(gender), data: InUse) if sender == data.by =>
      updateCounter(maleCounter, femaleCounter, gender,
        Duration(System.currentTimeMillis - data.atTimeMillis, TimeUnit.MILLISECONDS))
      if (data.queue.isEmpty)
        goto(Vacant) using NotInUse
      else {
        val (next, q) = data.queue.dequeue
        next ! YouCanUseTheBathroomNow
        stay using InUse(next, System.currentTimeMillis, q)
      }
  }
  initialize
}
