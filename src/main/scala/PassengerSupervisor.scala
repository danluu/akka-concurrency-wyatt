package zzz.akka.avionics

import akka.actor.{ActorRef, Actor, Props, ActorKilledException, ActorInitializationException, ActorLogging}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.{OneForOneStrategy}
import akka.routing.BroadcastRouter

import scala.concurrent.ExecutionContext.Implicits.global

object PassengerSupervisor {
  case object GetPassengerBroadcaster
  case class PassengerBroadcaster(broadcaster: ActorRef)
  def apply(callButton: ActorRef, bathrooms: ActorRef) = new PassengerSupervisor(callButton, bathrooms: ActorRef) with PassengerProvider
}

class PassengerSupervisor(callButton: ActorRef, bathrooms: ActorRef) extends Actor with ActorLogging { this: PassengerProvider =>
  import PassengerSupervisor._
  override val supervisorStrategy = OneForOneStrategy() {
    case _: ActorKilledException => Escalate
    case _: ActorInitializationException => Escalate
    case _ => Resume
  }

  //internal messages
  case class GetChildren(forSomeone: ActorRef)
  case class Children(children: Iterable[ActorRef], childrenFor: ActorRef)

  override def preStart() {
    context.actorOf(Props(new Actor {
      val config = context.system.settings.config
      override val supervisorStrategy = OneForOneStrategy() {
        case _: ActorKilledException => Escalate
        case _: ActorInitializationException => Escalate
        case _ => Stop
      }
      override def preStart() {
        import scala.collection.JavaConverters._
        import com.typesafe.config.ConfigList
        val passengers = config.getList("zzz.akka.avionics.passengers")
        passengers.asScala.foreach { nameWithSeat =>
          val id = nameWithSeat.asInstanceOf[ConfigList]
            .unwrapped().asScala.mkString("-")
            .replaceAllLiterally(" ", "_")
          context.actorOf(Props(newPassenger(callButton, bathrooms)), id)
        }
      }
      def receive = {
        case GetChildren =>
          log.info(s"${self.path.name} received GetChildren from ${sender.path.name}")
          sender ! context.children.toSeq
      }
    }), "PassengersSupervisor")
  }

  implicit val timeout = Timeout(4.seconds)
  def noRouter: Receive = {
    case GetPassengerBroadcaster =>
      log.info("PassengerSupervisor received GetPassengerBroadcaster")
      val destinedFor = sender
      val actor = context.actorFor("PassengersSupervisor")
      (actor ? GetChildren).mapTo[Seq[ActorRef]].map {
        passengers => (Props().withRouter(BroadcastRouter(passengers)),
          destinedFor)
      } pipeTo self
    case (props: Props, destinedFor: ActorRef) =>
      log.info(s"PassengerSupervisor received (${props.toString()},${destinedFor.toString()}) (transforming to withRouter)")
      val router = context.actorOf(props, "Passengers")
      destinedFor ! PassengerBroadcaster(router)
      context.become(withRouter(router))
  }

  def withRouter(router: ActorRef): Receive = {
    case GetPassengerBroadcaster =>
      sender ! PassengerBroadcaster(router)
    case Children(_, destinedFor) =>
      destinedFor ! PassengerBroadcaster(router)
  }

  def receive = noRouter
}
