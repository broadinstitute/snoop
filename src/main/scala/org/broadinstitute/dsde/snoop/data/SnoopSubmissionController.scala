package org.broadinstitute.dsde.snoop.data

import java.sql.Timestamp

import com.mchange.v2.c3p0.ComboPooledDataSource
import org.broadinstitute.dsde.snoop.SnoopConfig.DatabaseConfig
import scala.slick.jdbc.JdbcBackend._

/**
 * Created by plin on 3/25/15.
 */
object SnoopSubmissionController {
  val dataAccess = new DataAccess(DatabaseConfig.slickDriver)

  private val comboPooledDataSource = new ComboPooledDataSource
  comboPooledDataSource.setDriverClass(DatabaseConfig.jdbcDriver)
  comboPooledDataSource.setJdbcUrl(DatabaseConfig.jdbcUrl)
  comboPooledDataSource.setUser(DatabaseConfig.jdbcUser)
  comboPooledDataSource.setPassword(DatabaseConfig.jdbcPassword)
  DatabaseConfig.c3p0MaxStatementsOption.map(comboPooledDataSource.setMaxStatements)

  def database: Database = Database.forDataSource(comboPooledDataSource)

  def createSubmission(workflowId: String, callbackUri: String, status: String) : Submission = {
    database withTransaction {
      implicit session =>
        val submission = dataAccess.insertSubmission(Submission(workflowId, Option(new Timestamp(System.currentTimeMillis())), null, callbackUri, status))
        submission
    }
  }
}
