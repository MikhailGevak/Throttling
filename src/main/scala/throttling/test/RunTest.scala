package throttling.test

import scala.util.{ Failure, Success }
import akka.actor.ActorSystem
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import java.io.PrintWriter
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.Future
import spray.http.HttpHeaders.Authorization
import spray.http.OAuth2BearerToken
import spray.http.HttpHeader
import scala.concurrent.duration.FiniteDuration
/**
 * Implement the load test that proves that for N users, K rsp during T
 * seconds around T*N*K requests were successful. Measure the
 * overhead of using ThrottlingService service, compared with same
 * rest endpoint without ThrottlingService
 */
object RunTest extends App {
  val conf = ConfigFactory.load
  val sprayConf = conf.getConfig("spray.can.server")

  val host = s"http://localhost:${sprayConf.getInt("port")}"

  val users = args(0).toInt
  val rps = args(1).toInt
  val duration = args(2).toInt seconds

  implicit val timeout: Timeout = 1.5 * duration.toMillis milliseconds

  implicit val system = ActorSystem("simple-example")
  val log = Logging(system, getClass)
  import system.dispatcher
  val printWriter = new PrintWriter(s"tests/${args(3)}_${users}_${rps}_${duration}.log")

  val header = Authorization(OAuth2BearerToken("1234"))

  val testWorkers = (1 to users) map { i =>
    system.actorOf(TestLoadActor.props(host, rps, duration, printWriter, List[HttpHeader](header)), s"test-load-actor-$i")
  } map { worker => ask(worker, TestLoadActor.StartTest).mapTo[TestLoadActor.TestFinished] } map { future =>
    future onComplete {
      case Success(_) => "Finished!"
      case Failure(th) => th.printStackTrace()
    }
    future
  }

  Await.result(Future.sequence(testWorkers), 1.5 * duration)

  printWriter.close()
  println("finished")
  system.shutdown
}
