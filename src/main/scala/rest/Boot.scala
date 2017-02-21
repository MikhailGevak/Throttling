package rest

import akka.actor.{ ActorSystem, Props }
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import rest.hello.HelloWorldService
import rest.hello.HelloWorldServiceActor
import rest.hello.ThrottlingHelloWorldServiceActor
import rest.throttling.ThrottlingActor
import com.typesafe.config.ConfigFactory
import rest.throttling.ThrottlingService.CacheOptions
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit

object Boot extends App {
  val conf = ConfigFactory.load
  val throttlingConf = conf.getConfig("throttling")
  val sprayConf = conf.getConfig("spray.can.server")
  val graceRps = throttlingConf.getInt("grace-rps")
  val cacheOptions = getCacheOptionsFromConf(throttlingConf.getConfig("cache"))
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")

  // create and start our service actors
  val throttlingActor = system.actorOf(ThrottlingActor.props, ThrottlingActor.name)
  val helloWorldService = system.actorOf(HelloWorldServiceActor.props, "hello-world-service")
  val throttlingHelloWorldService = system.actorOf(ThrottlingHelloWorldServiceActor.props(throttlingActor, graceRps, cacheOptions), "throttling-hello-world-service")

  implicit val timeout = Timeout(5.seconds)
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(helloWorldService, interface = "localhost", port = sprayConf.getInt("port"))
  IO(Http) ! Http.Bind(throttlingHelloWorldService, interface = "localhost", port = sprayConf.getInt("throttling-port"))

  private[this] def getCacheOptionsFromConf(conf: Config) = {
    val defaultConfig = CacheOptions()
    val maxCapacity = if (conf.hasPath("max-capacity")) { conf.getInt("max-capacity") } else { defaultConfig.maxCapacity }
    val initialCapacity = if (conf.hasPath("initial-capacity")) { conf.getInt("initial-capacity") } else { defaultConfig.initialCapacity }
    val timeToLive = if (conf.hasPath("time-to-live")) { Duration(conf.getLong("time-to-live"), TimeUnit.SECONDS) } else { defaultConfig.timeToLive }
    val timeToIdle = if (conf.hasPath("time-to-idle")) { Duration(conf.getLong("time-to-idle"), TimeUnit.SECONDS) } else { defaultConfig.timeToIdle }

    CacheOptions(maxCapacity, initialCapacity, timeToLive, timeToIdle)
  }
}
