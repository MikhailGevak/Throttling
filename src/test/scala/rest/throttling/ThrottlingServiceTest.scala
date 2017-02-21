package rest.throttling

import akka.testkit.ImplicitSender
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import akka.testkit.TestKit
import akka.actor.ActorSystem
import rest.sla.SlaService
import rest.throttling.ThrottlingService.CacheOptions
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import rest.sla.SlaService.Sla
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import rest.throttling.ThrottlingService.CacheOptions

@RunWith(classOf[JUnitRunner])
class ThrottlingServiceTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  val token1 = "12132323"
  val token2 = "7777777777"

  val shortDuration = 5 milliseconds
  val longDuration = 250 milliseconds

  val throttlingActor = system.actorOf(ThrottlingActor.props)
  val GRASE_RPS = 5
  val slaService = SlaService()
  val throttlingService: ThrottlingService = ThrottlingService(GRASE_RPS, slaService, throttlingActor, CacheOptions())
  implicit val timeout: Timeout = 5 milliseconds

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An ThrottlingService.getSlaFunction" must {
    "work long at first time" in {
      val startTime = System.nanoTime()
      val result = Await.result(throttlingService.getSlaByTokenCached(token1).mapTo[Sla], 300 milliseconds)
      val endTime = System.nanoTime()

      (endTime - startTime) should be >= longDuration.toNanos
    }
    "immidiatly (faster than 5 ms) works at second time" in {
      val startTime = System.nanoTime()
      val result = Await.result(throttlingService.getSlaByTokenCached(token1).mapTo[Sla], 300 milliseconds)
      val endTime = System.nanoTime()

      (endTime - startTime) should be <= shortDuration.toNanos
    }
  }

  "An ThrottlingService.getUserByToken" must {
    "return Guest user (rps = default) at first time quickly (faster than 5 ms) " in {
      val startTime = System.nanoTime()
      val result = throttlingService.getUserByToken(token2)
      val endTime = System.nanoTime()
      (endTime - startTime) should be <= shortDuration.toNanos

      result.name should be("guest")
      result.rps should be(GRASE_RPS)
    }

    "return auth user at second time (after paused for 300 ms quickly (faster than 5 ms) )" in {
      Thread.sleep(300) //Wait for "getting result" in cache's future
      val startTime = System.nanoTime()
      val result = throttlingService.getUserByToken(token2)
      val endTime = System.nanoTime()
      (endTime - startTime) should be <= shortDuration.toNanos

      result.name should not be ("guest")
    }
  }

  "An ThrottlingService.isRequestAllowed" must {
    "allow only five request per second for guest (default rps is 5)" in {
      val results = for (i <- 1 to 6) yield { throttlingService.isRequestAllowed(None) }

      val positives = results.count(x => x)
      val negatives = results.count(x => !x)

      positives should be(5)
      negatives should be(1)
    }
    "allow rps according to user" in {
      val user = throttlingService.getUserByToken(token1)
      val results = for (i <- 1 to user.rps + 1) yield { throttlingService.isRequestAllowed(Some(token1)) }

      val positives = results.count(x => x)
      val negatives = results.count(x => !x)

      positives should be(user.rps)
      negatives should be(1)
    }
  }
}