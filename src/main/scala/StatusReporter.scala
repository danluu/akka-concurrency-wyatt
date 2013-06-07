package zzz.akka.avionics

import akka.actor.{ Actor }

object StatusReporter {
  case object ReportStatus
  sealed trait Status
  case object StatusOK extends Status
  case object StatusNotGreat extends Status
  case object StatusBAD extends Status
}

trait StatusReporter { this: Actor =>
  import StatusReporter._
  def currentStatus: Status //abstract
  def statusReceive: Receive = {
    case ReportStatus =>
      sender ! currentStatus
  }
}
