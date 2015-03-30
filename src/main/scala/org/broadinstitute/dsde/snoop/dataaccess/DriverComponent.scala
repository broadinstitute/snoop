package org.broadinstitute.dsde.snoop.dataaccess

import scala.slick.driver.JdbcProfile

trait DriverComponent {
  val driver: JdbcProfile
}
