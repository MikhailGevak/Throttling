package rest.throttling

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import akka.testkit.TestKit
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import akka.testkit.ImplicitSender
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import rest.throttling.ThrottlingUserActor.AllowedAnswer
import scala.concurrent.Await
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ThrottlingActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  implicit val timeout: Timeout = 5 milliseconds
  val futureDuration: Duration = 6 milliseconds
  
  val users = List[User](User("user1", 10), User("user2", 20), User("user3", 30), User("user4", 40), User("user5", 50))
  import system.dispatcher

  "An ThrottlingActor actor" must {
    "send back positive AllowedAnswer for one request for each user" in {
      val throttling = system.actorOf(ThrottlingActor.props)

      val responsesFuture = users map { user => ask(throttling, ThrottlingUserActor.IsAllowedRequest(user)).mapTo[AllowedAnswer] }

      val responses = Await.result(Future.sequence(responsesFuture), futureDuration)

      val positives = responses.filter(_.allowed).size
      val negatives = responses.filterNot(_.allowed).size

      positives should equal(users.length)
      negatives should equal(0)

      system.stop(throttling)
    }

    "send back 1 negative AllowedAnswer for rps+1 requests for each user" in {
      import system.dispatcher

      val throttling = system.actorOf(ThrottlingActor.props)

      val responsesFuture = users map { user => for (i <- 1 to user.rps + 1) yield { ask(throttling, ThrottlingUserActor.IsAllowedRequest(user)).mapTo[AllowedAnswer] } }

      val responses = Await.result(Future.sequence(responsesFuture.flatten), futureDuration)

      val positives = responses.filter(_.allowed).size
      val negatives = responses.filterNot(_.allowed).size

      positives should equal(users.foldLeft(0)((accum, user) => accum + user.rps))
      negatives should equal(users.length)

      system.stop(throttling)
    }
  }
}