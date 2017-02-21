package rest.hello

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import akka.actor.Props

object HelloWorldServiceActor{
  def props = Props[HelloWorldServiceActor]
}
// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class HelloWorldServiceActor extends Actor with HelloWorldService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(route)
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