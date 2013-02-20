package zzz.akka.avionics

import akka.actor._
import akka.actor.SupervisorStrategy._
import concurrent.duration.Duration
import akka.actor.{SupervisorStrategy, OneForOneStrategy, AllForOneStrategy}
import akka.actor.SupervisorStrategy.Decider


trait SupervisionStrategyFactory {
  def makeStrategy(maxNrRetries: Int, withinTimeRange: Duration)(decider: Decider): SupervisorStrategy
}

trait OneForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(maxNrRetries: Int, withinTimeRange: Duration)(decider: Decider): SupervisorStrategy = 
  OneForOneStrategy(maxNrRetries, withinTimeRange)(decider)
}

trait AllForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(maxNrRetries: Int, withinTimeRange: Duration)(decider: Decider): SupervisorStrategy =
  AllForOneStrategy(maxNrRetries, withinTimeRange)(decider)
}

