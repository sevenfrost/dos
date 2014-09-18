
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem
import akka.routing.RoundRobinRouter
import java.security.MessageDigest

trait Message
case object Mining extends Message
case class Work(uf: String, k: Int, start: Long, nrOfElements: Long) extends Message
case class Result(bitcoin: String, hash: String) extends Message
case object AskForTask extends Message
case object Start extends Message
case object Stop extends Message
 
  
  class Worker extends Actor {
    
	def map(num: Long):String = {
      var s = (32 + num % 95).toChar.toString
      if (num / 95 == 0)  s else s + map(num / 95)
    }
 

    def receive = {
      case Start => 
        sender ! AskForTask
        
      case Work(uf, k, start, nrOfElements) =>
         val md = MessageDigest.getInstance("SHA-256");
         def hex_digest(s: String): String = {
			md.digest(s.getBytes).foldLeft("")((s: String, b: Byte) => 
			  s + Character.forDigit((b & 0xf0) >> 4, 16) + Character.forDigit(b & 0x0f, 16))
         }
        for (i <- start until nrOfElements + start) {
          var s = uf + map(i)
          //var sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(s);
          var sha256hex = hex_digest(s)
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
  
  class Master(uf: String, k: Int, nrOfWorkers: Int, nrOfMessages: Int, nrOfElements: Long) extends Actor {
    //val start: Long = System.currentTimeMillis
    var MessageSent = 0
    var stopnum = 0
    def receive = {
      case Result(bitcoin, hash) =>;
        println(bitcoin + '\t' + hash)
        
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
          //println(stopnum)
        }
	//println(MessageSent)
	//println(stopnum)
        if (stopnum == 2) {
        	println("supposed to stop")
        	context.system.shutdown()
    	}
    } 	
  }
  
  object Master {
    def main(args: Array[String]) {
      val k = if (args.length > 0) args(0) toInt else 5
      val uf = "shuanglin"
      val nrOfWorkers = 2
      val nrOfMessages = 2
      val N = 5000000
      val system = ActorSystem("Master")
	  val master = system.actorOf(Props(new Master(uf, k, nrOfWorkers, nrOfMessages, N)),name = "MasterActor")
		master ! Mining
	  }
  }
 
