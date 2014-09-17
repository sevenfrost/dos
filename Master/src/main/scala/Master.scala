
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem
import akka.routing.RoundRobinRouter

trait Message
case object Mining extends Message
case class Work(uf: String, k: Int, start: Int, nrOfElements: Int) extends Message
case class Result(bitcoin: String, hash: String) extends Message
case object AskForTask extends Message
case object Start extends Message
case object Stop extends Message
 
  
  class Worker extends Actor {
    
	def map(num: Int):String = {
      var s = (32 + num % 95).toChar.toString
      if (num / 95 == 0)  s else s + map(num / 95)
    }
 
    def receive = {
      case Start => 
        sender ! AskForTask
        
      case Work(uf, k, start, nrOfElements) =>
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
        sender ! AskForTask
        
      case Stop => context.stop(self)
    }
    
  }
  
  class Master(uf: String, k: Int, nrOfWorkers: Int, nrOfMessages: Int, nrOfElements: Int) extends Actor {
    val start: Long = System.currentTimeMillis
    var MessageSent = 0
    var stopnum = 0
    def receive = {
      case Result(bitcoin, hash) =>;
       // println(bitcoin + '\t' + hash)
        
      case Mining => 
        for (i <- 0 until nrOfWorkers) {
          val worker = context.actorOf(Props[Worker], name = "worker"+i)
          worker ! Start
        }
        
      case AskForTask => 
        if (MessageSent < nrOfMessages) {
          sender ! Work(uf, k, MessageSent * nrOfElements, nrOfElements)
          MessageSent = MessageSent + 1
        }
        else {
          sender ! Stop
          stopnum = stopnum + 1
	  println(stopnum)
        }
	//println(MessageSent)
	//println(stopnum)
        if (stopnum == 3) {
	  println("supposed to stop")
	  context.system.shutdown()
    	}
    } 	
  }
  
  object Master {
    def main(args: Array[String]) {
      val k = if (args.length > 0) args(0) toInt else 4
      val uf = "shuanglin"
      val nrOfWorkers = 2
      val nrOfMessages = 100
      val N = 100000
      val system = ActorSystem("Master")
	  val master = system.actorOf(Props(new Master(uf, k, nrOfWorkers, nrOfMessages, N)),name = "MasterActor")
		master ! Mining
	  }
  }
 
