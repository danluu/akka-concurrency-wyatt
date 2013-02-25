package zzz.akka.avionics

import scala.concurrent.duration._
import akka.testkit.{TestKit, ImplicitSender}
import akka.actor.{ActorSystem, Actor, ActorRef, Props}
import org.scalatest.{WordSpec}
import org.scalatest.matchers.MustMatchers

trait TestDrinkRequestProbability extends DrinkRequestProbability {
  override val askThreshold = 0f
  override val requestMin = 0.millis
  override val requestUpper = 2.millis
}

class PassengersSpec extends TestKit(ActorSystem()) with ImplicitSender with WordSpec with MustMatchers{
  import akka.event.Logging.Info
  import akka.testkit.TestProbe
  import Passenger._

  var seatNumber = 9
  def newPassenger(): ActorRef = {
    seatNumber += 1
    system.actorOf(Props(new Passenger(testActor) with TestDrinkRequestProbability),
      s"Pat_Metheny-$seatNumber-B")
  }
  "Passengers" should {
    "fasten seatbelts when asked" in {
      val a = newPassenger()
      val p = TestProbe()
      system.eventStream.subscribe(p.ref, classOf[Info])
      a ! FastenSeatbelts
      p.expectMsgPF() {
        case Info(_, _, m) =>
          m.toString must include(" fastening seatbelt")
      }
    }
  }
}
