package rest.sla

import akka.testkit.ImplicitSender
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import org.scalatest.BeforeAndAfterEach
import akka.testkit.TestKit
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout
import rest.sla.SlaService.Sla
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.mockito.Mockito._

@RunWith(classOf[JUnitRunner])
class SlaServiceTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  import system.dispatcher

  val slaService = SlaService()
  val longDuration = 250 milliseconds
  val token = "12132323"
    
  "An SlaService" must {
    "return the value within 250 ms" in {
      val startTime = System.nanoTime()
      val result = Await.result(slaService.getSlaByToken(token).mapTo[Sla], 300 milliseconds)
      val endTime = System.nanoTime()

      (endTime - startTime) should be >= longDuration.toNanos
    }
  }
}