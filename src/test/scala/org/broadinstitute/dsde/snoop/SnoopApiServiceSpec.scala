package org.broadinstitute.dsde.snoop

import org.broadinstitute.dsde.snoop.ws.{WorkflowExecutionJsonSupport, ZamboniSubmissionResult}
import spray.httpx.SprayJsonSupport
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._
import WorkflowExecutionJsonSupport._
import SprayJsonSupport._
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import spray.testkit.ScalatestRouteTest

class SnoopApiServiceSpec extends FlatSpec with SnoopApiService with ScalatestRouteTest with Matchers {
  def actorRefFactory = system
  val submissionResult = ZamboniSubmissionResult("f00ba4", "SUBMITTED")

  "Snoop" should "return a greeting for GET requests to the root path" in {
    Get() ~> snoopRoute ~> check {
      responseAs[String] should include("Snoop web service is operational")
    }
  }
  it should "submission should return 200" in {
      Post("/workflowExecution", HttpEntity(ContentTypes.`application/json`, s"""{
         "submissionId": "f00ba4",
          "authToken": "some-token",
          "requestString":  "{\\"key1\\": \\"value1\\"}"
        }""")) ~>
        sealRoute(snoopRoute) ~> check {
        status === OK
        responseAs[ZamboniSubmissionResult] === submissionResult
      }
    }
}
