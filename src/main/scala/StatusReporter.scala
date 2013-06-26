package zzz.akka.avionics

import akka.actor.Actor

trait StatusReporter { this: Actor =>
  import StatusReporter._
  // Abstract - implementers need to define this
  def currentStatus: Status
  // This must be combined with orElse into the
  // ultimate receive method
  def statusReceive: Receive = {
    case ReportStatus =>
      sender ! currentStatus
  }
}

object StatusReporter {
  // The message indicating that status should be reported
  case object ReportStatus
  // The different types of status that can be reported
  sealed trait Status
  case object StatusOK extends Status
  case object StatusNotGreat extends Status
  case object StatusBAD extends Status
}