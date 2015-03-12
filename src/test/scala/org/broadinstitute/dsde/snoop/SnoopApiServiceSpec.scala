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
import org.broadinstitute.dsde.snoop.ws.ZamboniApi
import org.broadinstitute.dsde.snoop.ws.ZamboniSubmission
import scala.concurrent.Future
import org.broadinstitute.dsde.snoop.ws.WorkflowExecution
import spray.routing.RequestContext
import org.broadinstitute.dsde.snoop.ws.ZamboniWorkflowExecutionService

class SnoopApiServiceSpec extends FlatSpec with SnoopApiService with ScalatestRouteTest with Matchers {
  def actorRefFactory = system
  val zamboniApi = MockZamboniApi
  
  val executionServiceHandler: RequestContext => WorkflowExecutionService = ZamboniWorkflowExecutionService(MockZamboniApi)

  "Snoop" should "return a greeting for GET requests to the root path" in {
    Get() ~> snoopRoute ~> check {
      responseAs[String] should include("Snoop web service is operational")
    }
  }

  "Snoop" should "return 200 for post to workflowExecution" in {
    Post("/workflowExecutions", HttpEntity(ContentTypes.`application/json`, s"""{
          "workflowParameters": {"para1": "v1", "p2": "v2"},
          "workflowId":  "workflow_id",
          "callbackUri": "callback"}""")) ~>
      sealRoute(snoopRoute) ~> check {
      status === OK
      responseAs[WorkflowExecution] === WorkflowExecution(Some("f00ba4"), Map("para1" -> "v1", "p2" -> "v2"), "workflow_id", "callback", Some("SUBMITTED"))
    }
  }

  "Snoop" should "return 200 for get to workflowExecution" in {
    Get("/workflowExecutions/f00ba4") ~>
      sealRoute(snoopRoute) ~> check {
      status === OK
      responseAs[WorkflowExecution] === WorkflowExecution(Some("f00ba4"), Map("para1" -> "v1", "p2" -> "v2"), "workflow_id", "callback", Some("RUNNING"))
    }
  }
}

object MockZamboniApi extends ZamboniApi {
  import scala.concurrent.ExecutionContext.Implicits.global
  def start(zamboniSubmission: ZamboniSubmission): Future[ZamboniSubmissionResult] = {
    Future {
      if (!zamboniSubmission.requestString.contains("gcsSandboxBucket")) {
        throw new Exception("gcsSandboxBucket not populated")
      }
      ZamboniSubmissionResult("f00ba4", "SUBMITTED")
    }
  }
  
  def status(zamboniId: String): Future[ZamboniSubmissionResult] = {
    Future {
      ZamboniSubmissionResult("f00ba4", "RUNNING")
    }
  }
}