package org.broadinstitute.dsde.snoop

import org.scalatest._
import spray.testkit.ScalatestRouteTest


abstract class SnoopDatabaseSpec extends FreeSpec with Matchers with OptionValues with Inside with Inspectors with ScalatestRouteTest with TestDatabase
