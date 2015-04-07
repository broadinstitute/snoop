package org.broadinstitute.dsde.snoop

import org.broadinstitute.dsde.snoop.dataaccess.SnoopSubmissionController
import org.broadinstitute.dsde.snoop.model.Submission
import org.broadinstitute.dsde.snoop.ws.AnalysisCallbackHandler.AnalysesOutputResponse
import org.broadinstitute.dsde.snoop.ws.WorkflowParameter.WorkflowParameter
import org.broadinstitute.dsde.snoop.ws._
import spray.httpx.SprayJsonSupport
import spray.http._
import StatusCodes._
import WorkflowExecutionJsonSupport._
import SprayJsonSupport._
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import spray.testkit.ScalatestRouteTest
import scala.concurrent.duration._

class SnoopApiServiceSpec extends FlatSpec with RootSnoopApiService with WorkflowExecutionApiService with ScalatestRouteTest with Matchers with TestDatabase {
  def actorRefFactory = system

  def executionServiceConstructor(): WorkflowExecutionService = ZamboniWorkflowExecutionService(
    MockZamboniApi,
    "test",
    "key",
    new SnoopSubmissionController(() => TestDatabase.db, DatabaseConfig.slickDriver),
    MockAnalysisCallbackHandler,
    MockOutputRepository)

  implicit val routeTestTimeout = RouteTestTimeout(5 second)

  "Snoop" should "return a greeting for GET requests to the root path" in {
    Get() ~> baseRoute ~> check {
      responseAs[String] should include("Snoop web service is operational")
    }
  }

  var workflowExecId: String = _
  it should "return 201 for post to workflowExecution" in {
    Post("/workflowExecutions", HttpEntity(ContentTypes.`application/json`, s"""{
          "workflowParameters": {"para1": "v1", "p2": "v2", "p3": [ "a", "b", "c"] },
          "workflowId":  "workflow_id",
          "callbackUri": "callback"}""")) ~>
      addHeader(HttpHeaders.`Cookie`(HttpCookie("iPlanetDirectoryPro", "test_token"))) ~>
      sealRoute(startWorkflowRoute) ~>
      check {
        assertResult(Created) { status }
        assertResult(WorkflowExecution(None, Map("para1" -> WorkflowParameter("v1"), "p2" -> WorkflowParameter("v2"), "p3" -> WorkflowParameter(Seq("a", "b", "c"))), "workflow_id", "callback", Some("SUBMITTED"))) {
          responseAs[WorkflowExecution].copy(id=None)
        }
        workflowExecId = responseAs[WorkflowExecution].id.get
      }
  }

  it should "return 400 for post to workflowExecution without auth cookie" in {
    Post("/workflowExecutions", HttpEntity(ContentTypes.`application/json`, s"""{
          "workflowParameters": {"para1": "v1", "p2": "v2"},
          "workflowId":  "workflow_id",
          "callbackUri": "callback"}""")) ~>
      sealRoute(startWorkflowRoute) ~>
      check {
        assertResult(BadRequest) { status }
      }
  }

  it should "return 200 for get to workflowExecution" in {
    Get("/workflowExecutions/" + workflowExecId) ~>
      addHeader(HttpHeaders.`Cookie`(HttpCookie("iPlanetDirectoryPro", "test_token"))) ~>
      sealRoute(workflowStatusRoute) ~>
      check {
        assertResult(OK) { status }
        assertResult(WorkflowExecution(Some(workflowExecId), Map(), "", "", Some("SUCCEEDED"))) {
          responseAs[WorkflowExecution]
        }
      }
    assertResult(Map("vcf" -> "key/foo.vcf", "bam" -> "key/foo.bam")) { MockAnalysisCallbackHandler.output }
  }

  it should "return 404 for get to workflowExecution" in {
    Get("/workflowExecutions/idonotexist") ~>
      addHeader(HttpHeaders.`Cookie`(HttpCookie("iPlanetDirectoryPro", "test_token"))) ~>
      sealRoute(workflowStatusRoute) ~>
      check {
        assertResult(NotFound) { status }
      }
  }

  "WorkflowParameter parser" should "parse a single value" in {
    import WorkflowExecutionJsonSupport._
    import spray.json._

    val start = WorkflowParameter("foo")
    val json = start.toJson
    val end = json.convertTo[WorkflowParameter]

    assertResult(JsString("foo")) { json }
    assertResult(start) { end }
  }

  it should "parse a list of values" in {
    import WorkflowExecutionJsonSupport._
    import spray.json._

    val start = WorkflowParameter(Seq("value1", "value2"))
    val json = start.toJson
    val end = json.convertTo[WorkflowParameter]

    assertResult(JsArray(JsString("value1"), JsString("value2"))) { json }
    assertResult(start) { end }
  }

  it should "fail to parse a number" in {
    import WorkflowExecutionJsonSupport._
    import spray.json._

    val json = JsNumber(12)
    intercept[DeserializationException] {
      json.convertTo[WorkflowParameter]
    }
  }

  it should "fail to parse a list of numbers" in {
    import WorkflowExecutionJsonSupport._
    import spray.json._

    val json = JsArray(JsNumber(12), JsNumber(33))
    intercept[DeserializationException] {
      json.convertTo[WorkflowParameter]
    }
  }

  it should "fail to parse a mixed list" in {
    import WorkflowExecutionJsonSupport._
    import spray.json._

    val json = JsArray(JsString("foo"), JsNumber(33))
    intercept[DeserializationException] {
      json.convertTo[WorkflowParameter]
    }
  }
}

object MockZamboniApi extends ZamboniApi {
  def start(zamboniSubmission: ZamboniSubmission): ZamboniSubmissionResult = {
    if (!zamboniSubmission.requestString.contains("test_token")) {
      throw new Exception("authToken not correctly populated in zamboni request")
    }
    if (!zamboniSubmission.requestString.contains("gcsSandboxBucket")) {
      throw new Exception("gcsSandboxBucket not populated")
    }
    ZamboniSubmissionResult("f00ba4", "SUBMITTED")
  }
  
  def status(zamboniId: String): ZamboniSubmissionResult = {
    ZamboniSubmissionResult("f00ba4", "SUCCEEDED")
  }
}

object MockAnalysisCallbackHandler extends AnalysisCallbackHandler {
  var output: Map[String, String] = _
  def putOutputs(submission: Submission, outputs: Map[String, String]): AnalysesOutputResponse = {
    this.output = outputs
    AnalysesOutputResponse("id", List.empty, Map.empty, Map.empty)
  }
}

object MockOutputRepository extends OutputRepository {
  override def listOutputs(bucket: String, keyPrefix: String): Seq[String] = {
    Vector(keyPrefix + "/foo.bam", keyPrefix + "/foo.vcf")
  }
}
