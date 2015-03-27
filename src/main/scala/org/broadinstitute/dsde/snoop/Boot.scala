package org.broadinstitute.dsde.snoop

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.wordnik.swagger.model.ApiInfo
import org.broadinstitute.dsde.snoop.ws.{StandardZamboniApi, ZamboniWorkflowExecutionService}
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import spray.routing.RequestContext
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import java.io.File
import scala.reflect.runtime.universe._

import scala.sys.process.Process

object Boot extends App {
  val conf = ConfigFactory.parseFile(new File("/etc/snoop.conf"))
  //set system properties for ssl db connection
  System.setProperty("javax.net.ssl.trustStore", conf.getString("ssl.truststore"))
  System.setProperty("javax.net.ssl.trustStorePassword", conf.getString("ssl.tsPasswd"))
  System.setProperty("javax.net.ssl.keyStore", conf.getString("ssl.keystore"))
  System.setProperty("javax.net.ssl.keyStorePassword", conf.getString("ssl.ksPasswd"))
  Process("env", None,
    "JAVA_TOOL_OPTIONS" -> "-Djavax.net.ssl.trustStore=/etc/truststore;-Djavax.net.ssl.trustStorePassword=truststore;-Djavax.net.ssl.keyStore=/Users/plin/github/snoop/keystore;-Djavax.net.ssl.keyStorePassword=keystore")

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("snoop")


  val executionServiceHandler: RequestContext => WorkflowExecutionService =
    ZamboniWorkflowExecutionService(StandardZamboniApi(conf.getString("zamboni.server")), conf.getString("workflow.sandbox"))

  private val swaggerConfig = conf.getConfig("swagger")
  val swaggerService = new SwaggerService(
    swaggerConfig.getString("apiVersion"),
    swaggerConfig.getString("baseUrl"),
    swaggerConfig.getString("apiDocs"),
    swaggerConfig.getString("swaggerVersion"),
    Seq(typeOf[RootSnoopApiService], typeOf[WorkflowExecutionApiService]),
    Option(new ApiInfo(
      swaggerConfig.getString("info"),
      swaggerConfig.getString("description"),
      swaggerConfig.getString("termsOfServiceUrl"),
      swaggerConfig.getString("contact"),
      swaggerConfig.getString("license"),
      swaggerConfig.getString("licenseUrl"))
  ))

  val service = system.actorOf(SnoopApiServiceActor.props(executionServiceHandler, swaggerService), "snoop-service")

  implicit val timeout = Timeout(5.seconds)
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
}