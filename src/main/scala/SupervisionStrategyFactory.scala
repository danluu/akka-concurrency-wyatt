import akka.actor.{SupervisorStrategy, OneForOneStrategy, AllForOneStrategy}
import akka.actor.SupervisorStrategy.Decider
import concurrent.duration.Duration

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
