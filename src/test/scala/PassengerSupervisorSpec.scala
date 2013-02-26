package zzz.akka.avionics

import akka.actor.{ActorSystem, Actor, ActorRef, Props}
import akka.testkit.{TestKit, ImplicitSender}
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import org.scalatest.{WordSpec, BeforeAndAfterAll}
import org.scalatest.matchers.MustMatchers

object PassengerSupervisorSpec {
    val config = ConfigFactory.parseString("""
      zzz.akka.avionics.passengers = [
        [ "Kelly Franqui",
        [ "Tyrone Dotts",
        [ "Malinda Class",
        [ "Kenya Jolicoeur", "24", "A" ],
        [ "Christian Piche", "24", "B" ]
      ] 
  """)
}
