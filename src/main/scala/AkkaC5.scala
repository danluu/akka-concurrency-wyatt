package akka.tutorial

import akka.actor.{Actor, Props, ActorSystem}

class BadSActor extends Actor{
  def receive = {
    case "Good Morning"    => println ("Him: Forsooth 'tis the 'morn")
    case "You're terrible" => println ("Him: Yup")
    case _                 => throw new Exception("BadSActor received unknown message")
  }
}

object BadSMain{
  val system = ActorSystem("BadS")
  val actor = system.actorOf(Props[BadSActor])

  def send(msg: String){
    println("Me:   " + msg)
    actor ! msg
    Thread.sleep(100)
  }


  def main(args: Array[String]){
    send("Good Morning")
    send("You're terrible")
    send("Hrmmm")
    system.shutdown()
  }
}