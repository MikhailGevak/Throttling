package rest.throttling

import akka.actor.Actor
import scala.collection.mutable
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import akka.actor.Props
import akka.actor.PoisonPill
import akka.actor.ActorSystem
import akka.actor.ActorLogging

object ThrottlingUserActor {
  case class IsAllowedRequest(user: User)
  case class AllowedAnswer(user: String, allowed: Boolean)

  private[ThrottlingUserActor] case class UpdateRequest(tickNumber: Int)

  private[ThrottlingUserActor] val TICKS_PER_SECOND = 10
  private[ThrottlingUserActor] val FULL_PERIOD = TimeUnit.SECONDS.toNanos(1) nanos
  private[ThrottlingUserActor] val FULL_PERIOD_NANOS = FULL_PERIOD.toNanos
  private[ThrottlingUserActor] val UPDATE_PERIOD = FULL_PERIOD / TICKS_PER_SECOND
  def props(rps: Int) = Props(classOf[ThrottlingUserActor], rps)
}

class ThrottlingUserActor(requestsPerSecond: Int) extends Actor with ActorLogging {
  import ThrottlingUserActor._
  import context.dispatcher

  private[this] var requestsAllow = requestsPerSecond.toDouble // Count of the requests which are allowed
  private[this] var incCount = requestsPerSecond.toDouble / TICKS_PER_SECOND

  val actorSystem = context.system
  actorSystem.scheduler.schedule(UPDATE_PERIOD, UPDATE_PERIOD, self, UpdateRequest(1))//Update (increase allowed requests) counter every 1/10 second

  log.debug(s"!!!Started $self")
  
  def receive = {
    case IsAllowedRequest(user: User) =>
      //rpi can be changed. Not often but can 
      if (user.rps != requestsPerSecond) {
        updateRequestsPerSecond(requestsPerSecond)
      }
      
      if (Math.signum(requestsAllow - 1) >= 0.0) {
        requestsAllow -= 1
        sender ! AllowedAnswer(user.name, true)
      } else {
        sender ! AllowedAnswer(user.name, false)
      }
    case UpdateRequest(tickNumber) =>
      requestsAllow = (requestsAllow + incCount).min(requestsPerSecond)
  }

  private[this] def updateRequestsPerSecond(requestsPerSecond: Int) {
    requestsAllow = requestsPerSecond.toDouble
    incCount = requestsPerSecond.toDouble / TICKS_PER_SECOND
  }
}