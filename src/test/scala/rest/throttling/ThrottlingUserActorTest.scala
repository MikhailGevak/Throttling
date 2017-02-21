package rest.throttling

import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import akka.actor.ActorSystem
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.concurrent.Future
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.Await
import org.scalatest.BeforeAndAfterEach
import akka.actor.ActorRef

@RunWith(classOf[JUnitRunner])
class ThrottlingUserActorTest() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  import system.dispatcher
  import ThrottlingUserActor._
  implicit val timeout: Timeout = 5 milliseconds
  val RPS = 5
  val user = AuthUser("TestUser", RPS)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An ThrottlingUserActor actor" must {
    "send back positive AllowedAnswer for one request" in {
      val userCounter = system.actorOf(ThrottlingUserActor.props(user.rps))

      userCounter ! ThrottlingUserActor.IsAllowedRequest(user)
      expectMsg(ThrottlingUserActor.AllowedAnswer(user.name, true))
      
      system.stop(userCounter)
    }

    "send back 5 positive AllowedAnswer and 1 negative AllowedAnswer for 6 requests" in {
      val userCounter = system.actorOf(ThrottlingUserActor.props(user.rps))
        
      val responsesFutures = for (i <- 1 to RPS + 1) yield { ask(userCounter, IsAllowedRequest(user)).mapTo[AllowedAnswer] }

      val responses = Await.result(Future.sequence(responsesFutures), timeout.duration)

      val positives = responses.filter(_.allowed).size
      val negatives = responses.filterNot(_.allowed).size

      positives should equal(RPS)
      negatives should equal(1)
      
      system.stop(userCounter)
    }
    
    "send back 10 positive AllowedAnswer for 5 requests and 5 requests every 215 ms" in {
      val userCounter = system.actorOf(ThrottlingUserActor.props(user.rps))
        
      val responsesFutures1 = for (i <- 1 to RPS) yield { ask(userCounter, IsAllowedRequest(user)).mapTo[AllowedAnswer] }

      val responsesFutures2 = for (i <- 1 to RPS) yield {
        Thread.sleep((1000 / RPS) + 15)
        ask(userCounter, IsAllowedRequest(user)).mapTo[AllowedAnswer]
      }

      val responses = Await.result(Future.sequence(responsesFutures1 ++ responsesFutures2), timeout.duration)

      val positives = responses.filter(_.allowed).size
      val negatives = responses.filterNot(_.allowed).size

      positives should equal(2 * RPS)
      negatives should equal(0)
      
      system.stop(userCounter)
    }
  }
}