package org.broadinstitute.dsde.snoop

import javax.sql.DataSource

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.mchange.v2.c3p0.ComboPooledDataSource
import com.wordnik.swagger.model.ApiInfo
import org.broadinstitute.dsde.snoop.dataaccess.SnoopSubmissionController
import org.broadinstitute.dsde.snoop.ws._
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import com.typesafe.config.{Config, ConfigFactory}
import java.io.File
import scala.reflect.runtime.universe._

import scala.sys.process.Process

object Boot extends App {

  private def setupSsl(conf: Config): Unit = {
    System.setProperty("javax.net.ssl.trustStore", conf.getString("ssl.truststore"))
    System.setProperty("javax.net.ssl.trustStorePassword", conf.getString("ssl.tsPasswd"))
    System.setProperty("javax.net.ssl.keyStore", conf.getString("ssl.keystore"))
    System.setProperty("javax.net.ssl.keyStorePassword", conf.getString("ssl.ksPasswd"))
    Process("env", None,
      "JAVA_TOOL_OPTIONS" -> "-Djavax.net.ssl.trustStore=/etc/truststore;-Djavax.net.ssl.trustStorePassword=truststore;-Djavax.net.ssl.keyStore=/etc/keystore;-Djavax.net.ssl.keyStorePassword=keystore")
  }

  def createDataSource(jdbcDriver: String, jdbcUrl: String, jdbcUser: String, jdbcPassword: String, c3p0MaxStatementsOption: Option[Int]): DataSource = {
    val comboPooledDataSource = new ComboPooledDataSource
    comboPooledDataSource.setDriverClass(jdbcDriver)
    comboPooledDataSource.setJdbcUrl(jdbcUrl)
    comboPooledDataSource.setUser(jdbcUser)
    comboPooledDataSource.setPassword(jdbcPassword)
    c3p0MaxStatementsOption.map(comboPooledDataSource.setMaxStatements)

    comboPooledDataSource
  }

  private def startup(): Unit = {
    val conf = ConfigFactory.parseFile(new File("/etc/snoop.conf"))
    setupSsl(conf)

    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem("snoop")

    val zamboniApi = StandardZamboniApi(conf.getString("zamboni.server"))

    def executionServiceConstructor(): WorkflowExecutionService =
      ZamboniWorkflowExecutionService(zamboniApi, conf.getString("workflow.sandbox"), SnoopSubmissionController(createDataSource(
        conf.getString("database.jdbc.driver"),
        conf.getString("database.jdbc.url"),
        conf.getString("database.jdbc.user"),
        conf.getString("database.jdbc.password"),
        if (conf.hasPath("database.c3p0.maxStatements")) Option(conf.getInt("database.c3p0.maxStatements")) else None
      ), conf.getString("database.slick.driver")))

    val swaggerConfig = conf.getConfig("swagger")
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

    val service = system.actorOf(SnoopApiServiceActor.props(executionServiceConstructor, swaggerService), "snoop-service")

    implicit val timeout = Timeout(5.seconds)
    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
  }

  startup()
}
