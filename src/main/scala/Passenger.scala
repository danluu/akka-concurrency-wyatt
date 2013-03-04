package zzz.akka.avionics

import akka.actor.{ ActorRef, Actor, ActorLogging }
import scala.concurrent.duration._
import  scala.concurrent.ExecutionContext.Implicits.global

object Passenger {
  case object FastenSeatbelts
  case object UnfastenSeatbelts
  val SeatAssignment = """([\w\s_]+)-(\d+)-([A-Z])""".r
}

trait DrinkRequestProbability {
  val askThreshold = 0.9f
  val requestMin = 20.minutes
  val requestUpper = 30.minutes
  def randomishTime(): FiniteDuration = {
    requestMin + scala.util.Random.nextInt(
      requestUpper.toMillis.toInt).millis
  }
}

trait PassengerProvider {
  def newPassenger(callButton: ActorRef, bathrooms: ActorRef): Actor =
    new Passenger(callButton, bathrooms) with DrinkRequestProbability
}

class Passenger(callButton: ActorRef, bathrooms: ActorRef) extends Actor with ActorLogging { this: DrinkRequestProbability =>

  import Passenger._
  import FlightAttendant.{ GetDrink, Drink }
  import scala.collection.JavaConverters._
  val r = scala.util.Random
  case object CallForDrink
  case object CallForBathroom
  case object FinishWithBathroom
  import Bathroom.{IWannaUseTheBathroom, YouCanUseTheBathroomNow}
  // The name of the Passenger can't have spaces in it,
  // since that's not a valid character in the URI spec.  We
  // know the name will have underscores in place of spaces,
  // and we'll convert those back here.
  val SeatAssignment(myname, _, _) = self.path.name.replaceAllLiterally("_", " ")
  val drinks = context.system.settings.config.getStringList("zzz.akka.avionics.drinks").asScala.toIndexedSeq
  val scheduler = context.system.scheduler

  override def preStart() {
    self ! CallForDrink
    self ! CallForBathroom
  }

  def maybeSendDrinkRequest(): Unit = {
    if (r.nextFloat() > askThreshold) {
      val drinkname = drinks(r.nextInt(drinks.length))
      callButton ! GetDrink(drinkname)
    }
    scheduler.scheduleOnce(randomishTime(), self, CallForDrink)
  }

  def UseBathroom(): Unit = {
    scheduler.scheduleOnce(randomishTime(), self, FinishWithBathroom)
  }

  //using this stub, but we should base this off of the number of drinks + duration
  //instead, everyone tries to use the bathroom when they get on, and never try again
  def maybeSendBathroomRequest(): Unit = {
    log.info(s"$myname wants to use the bathroom (${bathrooms})")
    bathrooms ! IWannaUseTheBathroom
  }


  def receive = {
    case CallForDrink =>
      maybeSendDrinkRequest()
    case Drink(drinkname) =>
      log.info(s"$myname received a $drinkname - Yum")
    case FastenSeatbelts =>
      log.info(s"$myname fastening seatbelt")
    case UnfastenSeatbelts =>
      log.info(s"$myname unfastening seatbelt")
    case CallForBathroom =>
      maybeSendBathroomRequest()
    case YouCanUseTheBathroomNow =>
      log.info(s"$myname is using the bathroom")
      UseBathroom()
    case FinishWithBathroom => 
      sender ! Bathroom.Finished(Male) //fixing a random gender
  }

}
