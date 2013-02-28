package zzz.akka.avionics

import akka.actor.{ ActorSystem, Actor, ActorRef, Props }
import akka.testkit.{ TestKit, ImplicitSender }
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import org.scalatest.{ WordSpec, BeforeAndAfterAll }
import org.scalatest.matchers.MustMatchers

object PassengerSupervisorSpec {
  val config = ConfigFactory.parseString("""
      zzz.akka.avionics.passengers = [
        [ "Kelly Franqui", "23", "A" ],
        [ "Tyrone Dotts", "23", "B" ],
        [ "Malinda Class", "23", "C" ],
        [ "Kenya Jolicoeur", "24", "A" ],
        [ "Christian Piche", "24", "B" ]
      ] 
  """)
}

trait TestPassengerProvider extends PassengerProvider {
  override def newPassenger(callButton: ActorRef): Actor =
    new Actor {
      def receive = {
        case m => callButton ! m
      }
    }
}

class PassengerSupervisorSpec extends TestKit(ActorSystem("PassengerSupervisorSpec",
  PassengerSupervisorSpec.config))
  with ImplicitSender with WordSpec with BeforeAndAfterAll with MustMatchers {

  import PassengerSupervisor._

  override def afterAll() {
    system.shutdown()
  }

  "PassengerSupervisor" should {
    "work" in {
      val a = system.actorOf(Props(new PassengerSupervisor(testActor) with TestPassengerProvider))
      a ! GetPassengerBroadcaster
      val broadcaster = expectMsgType[PassengerBroadcaster].broadcaster
      broadcaster ! "Hithere"
      // All 5 passengers should say "Hithere"
      expectMsg("Hithere")
      expectMsg("Hithere")
      expectMsg("Hithere")
      expectMsg("Hithere")
      expectMsg("Hithere")
      // And then nothing else!
      expectNoMsg(100.milliseconds)
      // Ensure that the cache works
      a ! GetPassengerBroadcaster
      expectMsg(PassengerBroadcaster(`broadcaster`))

    }
  }
}
