package org.broadinstitute.dsde.snoop

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._

class SnoopApiServiceSpec extends Specification with Specs2RouteTest with SnoopApiService {
  def actorRefFactory = system

  "Snoop" should {
    "return a greeting for GET requests to the root path" in {
      Get() ~> snoopRoute ~> check {
        responseAs[String] must contain("Snoop web service is operational")
      }
    }
  }
}
