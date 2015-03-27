package org.broadinstitute.dsde.snoop.data

import scala.slick.driver.JdbcProfile

trait DriverComponent {
  val driver: JdbcProfile
}
