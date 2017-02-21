package rest.throttling

import akka.actor.Actor
import akka.actor.Props

object ThrottlingActor {
  val name = "throttling-service"
  def props = Props[ThrottlingActor]
}
class ThrottlingActor extends Actor {
  import ThrottlingUserActor._

  def receive = {
    case request @ IsAllowedRequest(user) =>
      getUserActor(user) forward request
  }
  protected def getUserActor(user: User) = context.child(user.name) getOrElse context.actorOf(ThrottlingUserActor.props(user.rps), name = user.name)
}