package rest.hello

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import akka.actor.Props
import akka.actor.ActorRef
import rest.sla.SlaService
import rest.throttling.ThrottlingService.CacheOptions
import rest.throttling.ThrottlingService
import spray.http.HttpHeaders.Authorization

object HelloWorldServiceActor {
  def props(throttlingActor: ActorRef, graceRps: Int, cacheOptions: CacheOptions, slaService: SlaService) = Props(classOf[HelloWorldServiceActor], throttlingActor, graceRps, cacheOptions, slaService)
}
// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class HelloWorldServiceActor(override val throttlingActor: ActorRef, override val graceRps: Int, override val cacheOptions: CacheOptions, override val slaService: SlaService) extends Actor with HelloWorldService with ThrottlingService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = throttling orElse router

  def throttling: PartialFunction[Any, Unit] = {
    case request @ HttpRequest(_, _, headers, _, _) =>
      val token = headers collectFirst { case header: Authorization => header.credentials } collect { case token: OAuth2BearerToken => token.token }
      if (isRequestAllowed(token)) {
        router(request)
      } else {
        sender ! HttpResponse(status = StatusCodes.TooManyRequests, entity = "Too many requests! Wait a minute!")
      }
  }

  def router = runRoute(route)
  
 // override def isRequestAllowed(token: Option[String]) = true //comment it if you don't want to use throttling
}

// this trait defines our service behavior independently from the service actor
trait HelloWorldService extends HttpService {
  val route =
    path("") {
      get {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            <html>
              <body>
                <h1>Hello world!</h1>
              </body>
            </html>
          }
        }
      }
    }
}