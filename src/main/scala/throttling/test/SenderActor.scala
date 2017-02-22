package throttling.test

import akka.actor.Actor
import akka.actor.ActorLogging
import spray.http.HttpResponse
import spray.http.HttpRequest
import spray.http.HttpMethods._
import scala.concurrent.duration._
import spray.http.Uri
import akka.util.Timeout
import akka.pattern.ask
import scala.util.Success
import scala.util.Failure
import akka.actor.Props
import spray.http.HttpHeader
import akka.actor.ActorRef

object SenderActor {
  case class Request(url: String, headers: List[HttpHeader])
  case class Response(response: HttpResponse, time: Duration)
  
  def props(ioHttp: ActorRef) = Props(classOf[SenderActor], ioHttp)
}

class SenderActor(ioHttp: ActorRef) extends Actor with ActorLogging {
  import SenderActor._
  import context.dispatcher
  private implicit val timeout: Timeout = 5.seconds
  implicit val system = context.system

  def receive = {
    case Request(url, headers) =>
      val _sender = sender
      val startTime = System.nanoTime()
      (ioHttp ? HttpRequest(GET, Uri(url), headers)).mapTo[HttpResponse] onComplete {
        case Success(response: HttpResponse) =>
          _sender ! Response(response, Duration.fromNanos(System.nanoTime - startTime))
        case Success(other) =>
          log.error(other.toString)
        case Failure(error) =>
          log.error("Error response", error)
      }
      val endTime = System.nanoTime()
  }

}