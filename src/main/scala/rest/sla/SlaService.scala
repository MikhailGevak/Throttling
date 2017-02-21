package rest.sla

import scala.concurrent.{ Future, Await }
import scala.collection.mutable
import scala.util.Random
import akka.actor.Actor
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.HttpMethods._
import spray.http.Uri
import spray.routing._
import spray.http.MediaTypes._
import scala.concurrent.duration._
import spray.http.HttpEntity
import spray.caching.LruCache
import spray.caching.{ LruCache, Cache }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object SlaService {
  def apply() = new SlaService() {}

  case class Sla(user: String, rps: Int)
}

trait SlaService {
  import SlaService._
  var nextElement = -1 //we have to start from 0
  val users = IndexedSeq[Sla](Sla("john", 50), Sla("ivan", 30), Sla("kraine", 70), Sla("kane", 40))
  val tokens = new mutable.HashMap[String, Sla]()
  val rand = new Random(System.currentTimeMillis())

  def getSlaByToken(token: String): Future[Sla] = Future[Sla] {
    Thread.sleep(250)
    getToken(token)
  }

  private[this] def getToken(token: String) = tokens.getOrElseUpdate(token, getNextUser)
  
  private[this] def getNextUser = {
    nextElement = (nextElement + 1 ) % users.length
    users(nextElement) 
  }

}