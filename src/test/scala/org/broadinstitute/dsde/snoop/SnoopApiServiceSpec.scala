package org.broadinstitute.dsde.snoop

import org.broadinstitute.dsde.snoop.ws.{WorkflowExecutionJsonSupport, ZamboniSubmissionResult}
import org.specs2.mutable.Specification
import spray.httpx.SprayJsonSupport
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._
import WorkflowExecutionJsonSupport._
import SprayJsonSupport._

class SnoopApiServiceSpec extends Specification with SnoopApiService with Specs2RouteTest {
  def actorRefFactory = system
  val submissionResult = ZamboniSubmissionResult("f00ba4", "SUBMITTED")

  "Snoop" should {
      "return a greeting for GET requests to the root path" in {
        Get() ~> snoopRoute ~> check {
          responseAs[String] must contain("Snoop web service is operational")
        }
      }
      "should return 200" in {
        Post("/workflowExecution", HttpEntity(ContentTypes.`application/json`, s"""{
         "submissionId": "f00ba4",
          "authToken": "some-token",
          "requestString":  "{\\"key1\\": \\"value1\\"}"
        }""")) ~>
          snoopRoute ~> check {
          status === OK
          responseAs[ZamboniSubmissionResult] === submissionResult
        }
      }
  }
}
