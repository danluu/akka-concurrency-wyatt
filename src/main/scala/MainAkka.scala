/*
Code from book that doesn't seem copmatible with current scala


package  zzz.akka.investigation
import  java.util.concurrent.Executors
import  scala.concurrent.{Await,  Future,  ExecutionContext}
import  scala.concurrent.util.duration._
object  MainAkka {
  val  pool  =  Executors.newCachedThreadPool()
  implicit  val  ec  =  ExecutionContext.fromExecutorService(pool)
  def  main(args:  Array[String])  {
    val  future  =  Future  {  "Fibonacci  Numbers"  }
    val  result  =  Await.result(future,  1.second)
    println(result)
    pool.shutdown()
  }
}

*/