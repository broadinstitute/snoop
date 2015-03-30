package org.broadinstitute.dsde.snoop.dataaccess

import org.broadinstitute.dsde.snoop.util.Reflection

import scala.slick.driver.JdbcProfile

/**
 * Created by plin on 3/25/15.
 */
class DataAccess(val driver: JdbcProfile) extends SubmissionComponent with DriverComponent {
  import driver.simple._

  def this(driverName: String) {
    this(Reflection.getObject[JdbcProfile](driverName))
  }

}
