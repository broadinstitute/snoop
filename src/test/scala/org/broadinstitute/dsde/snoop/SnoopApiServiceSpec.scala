package org.broadinstitute.dsde.snoop

import org.broadinstitute.dsde.snoop.ws.{WorkflowExecutionJsonSupport, ZamboniSubmissionResult}
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import spray.httpx.SprayJsonSupport
import spray.http._
import StatusCodes._
import WorkflowExecutionJsonSupport._
import SprayJsonSupport._
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import spray.testkit.ScalatestRouteTest

class SnoopApiServiceSpec extends FlatSpec with SnoopApiService with ScalatestRouteTest with Matchers {
  def actorRefFactory = system

  "Snoop" should "return a greeting for GET requests to the root path" in {
    Get() ~> snoopRoute ~> check {
      responseAs[String] should include("Snoop web service is operational")
    }
  }
  //disable the test b/c it failed on travis
  //the fix is to allow zamboni test server picard02 accept ip from outside Broad network
  //or do not do routetest at all
  ignore should "return 200 for submission to workflowExecution" in {
    Post("/workflowExecutions", HttpEntity(ContentTypes.`application/json`, s"""{
         "id": "f00ba4",
          "workflowParameters": {"para1": "v1", "p2": "v2"},
          "workflowId":  "workflow_id",
          "callbackUri": "callback",
          "status": "submitting"
}""")) ~>
      sealRoute(snoopRoute) ~> check {
      status === OK
      responseAs[ZamboniSubmissionResult] === ZamboniSubmissionResult("f00ba4", "SUBMITTED")
    }
  }
}
