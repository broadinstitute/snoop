package org.broadinstitute.dsde.snoop

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import org.broadinstitute.dsde.snoop.ws.{StandardZamboniApi, ZamboniWorkflowExecutionService}
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import spray.routing.RequestContext
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import java.io.File

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")
  
  val conf = ConfigFactory.parseFile(new File("/etc/snoop.conf"))

  /*
    So now the definition of how to go from a RequestContext to a Props is contained outside of the Snoop layer altogether
    and can be determined however we want.
   */
  val executionServicePropsFunc: RequestContext => Props = ZamboniWorkflowExecutionService.props(StandardZamboniApi(conf.getString("zamboni.server")))

  // create and start our service actor
  val service = system.actorOf(SnoopApiServiceActor.props(executionServicePropsFunc), "snoop-service")

  implicit val timeout = Timeout(5.seconds)
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
}