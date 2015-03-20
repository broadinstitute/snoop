/*
Copyright (c) 2015, Broad Institute, Inc.
All rights reserved.
 
Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:
 
* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
 
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
 
* Neither the name Broad Institute, Inc. nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.
 
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
*/

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