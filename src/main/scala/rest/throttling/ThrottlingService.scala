package rest.throttling

import akka.actor.Actor
import akka.actor.Props
import rest.sla.SlaService
import scala.util.{ Success, Failure }
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.Await
import spray.routing.HttpService
import akka.actor.ActorRef
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import spray.caching.Cache
import rest.sla.SlaService.Sla
import spray.caching.LruCache

object ThrottlingService {
  case class CacheOptions(maxCapacity: Int = 500, initialCapacity: Int = 16, timeToLive: Duration = Duration.Inf, timeToIdle: Duration = Duration.Inf)
  
  def apply(_graceRps: Int, _slaService: SlaService, _throttlingActor: ActorRef, _cacheOptions: CacheOptions) = {
    new ThrottlingService {
      override val graceRps = _graceRps
      override val slaService = _slaService
      override val throttlingActor = _throttlingActor
      override val cacheOptions = _cacheOptions
    }
  }
}

trait ThrottlingService {
  import ThrottlingUserActor._
  import ThrottlingService._
  ///////////////////////////////////
  val graceRps: Int
  val slaService: SlaService
  val throttlingActor: ActorRef
  val cacheOptions: CacheOptions
  lazy val cache: Cache[Sla] = LruCache(cacheOptions.maxCapacity, cacheOptions.initialCapacity, cacheOptions.timeToLive, cacheOptions.timeToIdle)
  ///////////////////////////////////
  lazy val guest = GuestUser(graceRps)

  implicit val timeout = Timeout(5 milliseconds)

  //It will use cached result if cache options are present
  protected[throttling] def getSlaByTokenCached(token: String) = cache(token) {
    slaService.getSlaByToken(token)
  }

  /**
   *  Return true if the request is within allowed RPS.
   *  And increment count of requests.
   */

  def isRequestAllowed(token: Option[String]): Boolean = {
    implicit val timeout = Timeout(5 milliseconds)
    val result: AllowedAnswer = Await.result(getThrottlingAsk(token).mapTo[AllowedAnswer], timeout.duration)
    result.allowed
  }

  protected[throttling] def getUserByToken(token: String): User = {
    val futureSla = getSlaByTokenCached(token)
    futureSla.value match {
      case Some(Success(sla)) =>
        AuthUser(sla.user, sla.rps)
      case other =>
        guest
    }
  }
  
  private[this] def getThrottlingAsk(token: Option[String]): Future[Any] = {
    val user = token map { getUserByToken } getOrElse guest

    throttlingActor ? IsAllowedRequest(user) recover {
      case th: Throwable =>
        th.printStackTrace
        AllowedAnswer(user.name, false)
    }
  }

  protected[throttling] def clearCache = {
    cache.clear
  }

}