package throttling.test

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration.Duration
import akka.actor.Cancellable
import scala.concurrent.duration._
import akka.util.Timeout
import spray.http.HttpHeaders
import akka.actor.Props
import java.io.PrintWriter
import spray.http.HttpHeader
import java.util.Date
import spray.can.Http
import akka.io.IO

object TestLoadActor {
  case object StartTest
  case class TestFinished()
  private[TestLoadActor] case class FinishTest(sender: ActorRef)

  def props(host: String, rps: Int, duration: FiniteDuration, printWriter: PrintWriter, headers: List[HttpHeader]) = Props(classOf[TestLoadActor], host, rps, duration, printWriter, headers)
}

//Actor for sending for one user
class TestLoadActor(host: String, rps: Int, duration: FiniteDuration, printWriter: PrintWriter, headers: List[HttpHeader]) extends Actor with ActorLogging {
  import TestLoadActor._
  import context.dispatcher

  val scheduler = context.system.scheduler
  var task: Option[Cancellable] = None
  val interval = (1 seconds) / rps
  val format = new java.text.SimpleDateFormat("dd-MM-yyyy hh:mm:ss.ms")
  val ioHttp = IO(Http)(context.system)

  def receive = {
    case StartTest =>
      val _sender = sender
      scheduler.scheduleOnce(duration, self, FinishTest(_sender))
      task = Some(scheduler.schedule(Duration.Zero, interval, context.actorOf(SenderActor.props(ioHttp)), SenderActor.Request(host, headers)))
    case FinishTest(initiator) =>
      task map { _.cancel }
      initiator ! TestFinished()
    case SenderActor.Response(response, duration) =>
      printWriter.println(s"${format.format(new Date())}\t${response.status}\t${response.entity.data.length}\t${duration.toMillis} ms")
  }
}