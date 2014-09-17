

import akka.actor.Actor
import akka.actor.Props
import akka.routing.RoundRobinRouter
import akka.actor.ActorSystem


trait Message
case class Work(uf: String, k: Int, start: Int, nrOfElements: Int) extends Message
case class Result(bitcoin: String, hash: String) extends Message
case object AskForTask extends Message
case object Start extends Message
case object Stop extends Message

class Worker(ip: String) extends Actor {
	val masterRef = "akka.tcp://Master@" + ip + ":2666/user/MasterActor"
    val master = context.actorFor(masterRef)
    
	def map(num: Int):String = {
      var s = (32 + num % 95).toChar.toString
      if (num / 95 == 0)  s else s + map(num / 95)
    }
 
    def receive = {
      case Start => 
        master ! AskForTask
        
      case Work(uf, k, start, nrOfElements) =>
        println("get work")
        for (i <- start until nrOfElements + start) {
          var s = uf + map(i)
          var sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(s);
          try {
        	  val foo = Integer.parseInt(sha256hex.substring(0, k));
        	  if (foo == 0) {
        	    sender ! Result(s, sha256hex)
        	  }
          } 
          catch {
          	case e:NumberFormatException => ;
          }
        }
        master ! AskForTask
        
      case Stop => 
        println("stop message received")
        //context.stop(self)
        context.system.shutdown()
    }
  }

object Worker {
	def main(args: Array[String]) {
    val ip = if (args.length > 0) args(0)  else "128.227.248.195"
    val system = ActorSystem("BitCoinSystem")
    val worker = system.actorOf(Props(new Worker(ip)), name = "worker")
    worker ! Start
  }
}
