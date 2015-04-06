package org.broadinstitute.dsde.snoop.ws

import akka.actor.ActorSystem
import org.broadinstitute.dsde.snoop.model.Submission
import spray.client.pipelining._
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

  implicit val AnalysesOutputFormat = jsonFormat1(AnalysesOutput)
  implicit val AnalysesOutputResponseFormat = jsonFormat4(AnalysesOutputResponse)
}

import AnalysisCallbackHandler._

trait AnalysisCallbackHandler {
  def putOutputs(submission: Submission, outputs: Map[String, String]): AnalysesOutputResponse
}

case class StandardAnalysisCallbackHandler()(implicit val system: ActorSystem) extends AnalysisCallbackHandler {
  import system.dispatcher

  override def putOutputs(submission: Submission, outputs: Map[String, String]): AnalysesOutputResponse = {
    val pipeline = addHeader("X-Force-Location", "true") ~> sendReceive ~> unmarshal[AnalysesOutputResponse]

    val future = pipeline {
      Post(submission.callbackUri, AnalysesOutput(outputs))
    }

    Await.result(future, 1 minutes)
  }
}
