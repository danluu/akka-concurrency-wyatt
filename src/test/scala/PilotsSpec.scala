package zzz.akka.avionics

import akka.actor.{ActorSystem, Actor, ActorRef, Props, PoisonPill}
import akka.pattern.ask
import akka.testkit.{TestKit, ImplicitSender, TestProbe}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import scala.concurrent.Await
import scala.concurrent.duration._

class FakePilot extends Actor {
  override def receive = {
    case _ =>
  }
}

object PilotsSpec {
  val copilotName = "Mary"
  val pilotName = "Mark"
  val configStr = s"""
    zzz.akka.avionics.flightcrew.copilotName = "$copilotName"
    zzz.akka.avionics.flightcrew.pilotName = "$pilotName""""
}

class PilotsSpec extends TestKit(ActorSystem("PilotsSpec",
            ConfigFactory.parseString(PilotsSpec.configStr)))
    with ImplicitSender with WordSpec with MustMatchers {

  import Plane._
  import PilotsSpec._

  def nilActor: ActorRef = TestProbe().ref

  val pilotPath = s"/user/TestPilots/$pilotName"
  val copilotPath = s"/user/TestPilots/$copilotName"

  def pilotsReadyToGo(): ActorRef = {
    implicit val askTimeout = Timeout(4.seconds)

    val a = system.actorOf(Props(new IsolatedStopSupervisor with OneForOneStrategyFactory{
      def childStarter() {
        context.actorOf(Props[FakePilot], pilotName)
        context.actorOf(Props(new CoPilot(testActor, nilActor)), copilotName)
      }
    }), "TestPilots")
    Await.result(a ? IsolatedLifeCycleSupervisor.WaitForStart, 3.seconds)
    system.actorFor(copilotPath) ! Pilots.ReadyToGo
    a 
  }

  "CoPilot" should {
    "take control when the Pilot dies" in {
      pilotsReadyToGo()
      system.actorFor(pilotPath) ! PoisonPill
      expectMsg(GiveMeControl)
      lastSender must be (system.actorFor(copilotPath))
    }
  }
}
