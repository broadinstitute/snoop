package org.broadinstitute.dsde.snoop

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._

class SnoopApiServiceSpec extends Specification with SnoopApiService with Specs2RouteTest {
  def actorRefFactory = system

  "Snoop" should {
      "return a greeting for GET requests to the root path" in {
        Get() ~> snoopRoute ~> check {
          responseAs[String] must contain("Snoop web service is operational")
        }
      }

      "should return 200" in {
        Post(s"http://127.0.0.1:8080/workflowExecution", HttpEntity(ContentTypes.`application/json`, s"""{
         "submissionId": "f00ba4",
          "authToken": "some-token",
          "requestString":  "{\\"key1\\": \\"value1\\"}"
        }""")) ~>
          snoopRoute ~> check {
          status === OK
          responseAs[String] must contain("f00ba4")
          responseAs[String] must contain("SUBMITTED")
        }
      }
  }
}
