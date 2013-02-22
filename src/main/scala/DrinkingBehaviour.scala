package zzz.akka.avionics

import akka.actor.{ActorRef, Actor}
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object DrinkingBehaviour {
  case class LevelChanged(level: Float) //BAC
  case object FeelingSober
  case object FeelingTipsy
  case object FeelingLikeZaphod
  def apply(drinker: ActorRef) = new DrinkingBehaviour(drinker) with DrinkingResolution
}

trait DrinkingResolution{
  import scala.util.Random
  def initialSobering: FiniteDuration = 1.second
  def soberingInterval: FiniteDuration = 1.second
  def drinkInterval(): FiniteDuration = Random.nextInt(300).seconds
}

class DrinkingBehaviour(drinker: ActorRef) extends Actor{ this: DrinkingResolution =>
  import DrinkingBehaviour._

  var currentLevel = 0f
  val scheduler = context.system.scheduler
  val sobering = scheduler.schedule(initialSobering, soberingInterval, self, LevelChanged(-0.0001f))

  override def postStop() { sobering.cancel() }
  override def preStart() { drink() }

  def drink() = scheduler.scheduleOnce(drinkInterval(), self, LevelChanged(0.005f))

  def receive = {
    case LevelChanged(amount) =>
      currentLevel = (currentLevel + amount).max(0f)
      drinker ! (if (currentLevel <= 0.01) {
        drink()
        FeelingSober
      } else if (currentLevel <= 0.03) {
        drink()
      }
      else FeelingLikeZaphod)
  }
}
