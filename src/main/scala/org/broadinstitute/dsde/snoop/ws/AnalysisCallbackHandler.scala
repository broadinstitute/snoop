package org.broadinstitute.dsde.snoop.ws

import akka.actor.ActorSystem
import org.broadinstitute.dsde.snoop.model.Submission
import spray.client.pipelining._
import spray.http.{HttpCookie, HttpHeaders}
import spray.http.HttpHeaders.Cookie
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by plin on 4/1/15.
 */
object AnalysisCallbackHandler extends DefaultJsonProtocol {
  case class AnalysesOutput(files: Map[String, String])
  case class AnalysesOutputResponse(id: String, input: List[String], metadata: Map[String, String], files: Map[String, String])
  case class AnalysesObject(guid: String, `type`: String)

  implicit val AnalysesOutputFormat = jsonFormat1(AnalysesOutput)
  implicit val AnalysesOutputResponseFormat = jsonFormat4(AnalysesOutputResponse)
  implicit val AnalysesObjectFormat = jsonFormat2(AnalysesObject)
}

import AnalysisCallbackHandler._

trait AnalysisCallbackHandler {
  def putOutputs(submission: Submission, outputs: Map[String, String], securityToken: String): AnalysesOutputResponse
}

case class StandardAnalysisCallbackHandler(server: String, queryPath: String)(implicit val system: ActorSystem) extends AnalysisCallbackHandler {
  import system.dispatcher

  override def putOutputs(submission: Submission, outputs: Map[String, String], securityToken: String): AnalysesOutputResponse = {
    /*
    please note the technical debt here: we really should be using the callback uri stored in the the submission
    object in the db but that is not populated yet because the apollo api creates the submission before
    creating the vault object so the callback uri cannot be constructed when submitting the workflow.
    So we need to query the vault for the vault id now but really we should have no knowledge of the vault here.
     */

    val callbackUri = submission.callbackUri.getOrElse(getVaultCallbackUri(submission, securityToken))

    val pipeline =
      addHeader(HttpHeaders.`Cookie`(HttpCookie("iPlanetDirectoryPro", securityToken))) ~>
      addHeader("X-Force-Location", "true") ~>
      sendReceive ~>
      unmarshal[AnalysesOutputResponse]

    val future = pipeline {
      Post(callbackUri, AnalysesOutput(outputs))
    }

    Await.result(future, 1 minutes)
  }

  def getVaultCallbackUri(submission: Submission, securityToken: String): String = {
    val vaultId = getAnalysesObject(submission.id, securityToken).guid
    s"$server/api/analyses/$vaultId/outputs"
  }

  private def getAnalysesObject(id: String, securityToken: String): AnalysesObject = {
    val pipeline = addHeader(HttpHeaders.`Cookie`(HttpCookie("iPlanetDirectoryPro", securityToken))) ~> sendReceive ~> unmarshal[AnalysesObject]

    val future = pipeline {
      Get(s"$server$queryPath$id")
    }

    Await.result(future, 1 minutes)
  }
}
