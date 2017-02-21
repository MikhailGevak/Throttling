package rest.hello

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import rest.throttling.ThrottlingService
import akka.actor.ActorRef
import rest.sla.SlaService
import rest.throttling.ThrottlingService.CacheOptions
import akka.actor.Props
import spray.http.HttpHeaders.Authorization

object ThrottlingHelloWorldServiceActor {
  def props(throttlingActor: ActorRef, graceRps: Int, cacheOptions: CacheOptions) = Props(classOf[ThrottlingHelloWorldServiceActor], throttlingActor, graceRps, cacheOptions)
}
// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class ThrottlingHelloWorldServiceActor(override val throttlingActor: ActorRef, override val graceRps: Int, override val cacheOptions: CacheOptions) extends Actor with ThrottlingHelloWorldService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = throttling orElse router

  def throttling: PartialFunction[Any, Unit] = {
    case request @ HttpRequest(_, _, headers, _, _) =>
      val token = headers collectFirst { case header: Authorization => header.credentials } collect { case credentials: OAuth2BearerToken => credentials.token }
      if (isRequestAllowed(token)) {
        router(request)
      } else {
        sender ! HttpResponse(status = StatusCodes.TooManyRequests, entity = "Too many requests! Wait a minute!")
      }
  }

  def router = runRoute(route)
}

// this trait defines our service behavior independently from the service actor
trait ThrottlingHelloWorldService extends HttpService with ThrottlingService {
  override val slaService = SlaService()
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