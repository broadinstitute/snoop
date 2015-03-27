package org.broadinstitute.dsde.snoop

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

object SnoopConfig {
  val conf = ConfigFactory.parseFile(new File("/etc/snoop.conf"))
  private val testConfig = ConfigFactory.load()

  val env = System.getProperties().getProperty("env.type")
  System.out.println("Env type is " + env)
  private def chooseConf(): Config = {
    if (env == "test") testConfig else conf
  }

  private def getOrElse(config: Config, path: String, default: String): String = {
    if (config.hasPath(path)) config.getString(path) else default
  }

  private def getOption(config: Config, path: String, default: Option[Int] = None): Option[Int] = {
    if (config.hasPath(path)) Option(config.getInt(path)) else default
  }

  object DatabaseConfig {
    private val database = chooseConf().getConfig("database")
    lazy val slickDriver = database.getString("slick.driver")
    lazy val liquibaseSetup = database.hasPath("liquibase")
    lazy val liquibaseChangeLog = database.getString("liquibase.changelog")
    lazy val liquibaseConnection = getOrElse(database, "liquibase.connection", "liquibase.database.jvm.JdbcConnection")
    lazy val jdbcUrl = database.getString("jdbc.url")
    lazy val jdbcDriver = database.getString("jdbc.driver")
    lazy val jdbcUser = getOrElse(database, "jdbc.user", null)
    lazy val jdbcPassword = getOrElse(database, "jdbc.password", null)
    lazy val c3p0MaxStatementsOption = getOption(database, "c3p0.maxStatements")
  }

}
