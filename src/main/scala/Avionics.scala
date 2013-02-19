package zzz.akka.avionics

import akka.actor.{Props,  Actor,  ActorRef,  ActorSystem}
import akka.pattern.ask

import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

object Avionics {
  implicit val timeout =  Timeout(5.seconds)
  val system  =  ActorSystem("PlaneSimulation")
  val plane  =  system.actorOf(Props[Plane], "Plane")

  def main(args: Array[String]){
    val control = Await.result((plane ? Plane.GiveMeControl).mapTo[ActorRef], 5.seconds)

    system.scheduler.scheduleOnce(200.millis){control ! ControlSurfaces.StickBack(1f)}
    system.scheduler.scheduleOnce(1.seconds) {control ! ControlSurfaces.StickBack(0f)}
    system.scheduler.scheduleOnce(3.seconds) {control ! ControlSurfaces.StickBack(.5f)}
    system.scheduler.scheduleOnce(4.seconds) {control ! ControlSurfaces.StickBack(0f)}
    system.scheduler.scheduleOnce(5.seconds) {system.shutdown()}
  }



}
