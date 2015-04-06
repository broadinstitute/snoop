package org.broadinstitute.dsde.snoop

import org.broadinstitute.dsde.snoop.model.Submission

import scala.collection.JavaConversions._
import java.io.File
import java.net.URI
import java.util.{Collections, UUID}

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.{HttpRequest, HttpBackOffUnsuccessfulResponseHandler, HttpResponse}
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff.Builder
import com.google.api.services.storage.Storage
import com.google.api.services.storage.model.{StorageObject, Objects}
import org.broadinstitute.dsde.snoop.dataaccess.SnoopSubmissionController
import org.broadinstitute.dsde.snoop.ws.PerRequest.RequestComplete
import spray.http.StatusCodes
import akka.actor.{Actor, ActorRefFactory, Props}
import akka.event.Logging
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.annotations._
import com.wordnik.swagger.model.ApiInfo
import org.broadinstitute.dsde.snoop.ws._
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport
import spray.routing.Directive.pimpApply
import spray.routing._

import scala.annotation.tailrec
import scala.reflect.runtime.universe._
import scala.util.matching._

object SnoopApiServiceActor {
  def props(executionServiceConstructor: () => WorkflowExecutionService, swaggerService: SwaggerService): Props = {
    Props(new SnoopApiServiceActor(executionServiceConstructor, swaggerService))
  }
}

class SwaggerService(override val apiVersion: String,
                     override val baseUrl: String,
                     override val docsPath: String,
                     override val swaggerVersion: String,
                     override val apiTypes: Seq[Type],
                     override val apiInfo: Option[ApiInfo])(implicit val actorRefFactory: ActorRefFactory) extends SwaggerHttpService

class SnoopApiServiceActor(executionServiceCtor: () => WorkflowExecutionService, swaggerService: SwaggerService) extends Actor with RootSnoopApiService with WorkflowExecutionApiService {
  implicit def executionContext = actorRefFactory.dispatcher
  def actorRefFactory = context
  def possibleRoutes = baseRoute ~ workflowRoutes ~ swaggerService.routes 

  def receive = runRoute(possibleRoutes)
  def apiTypes = Seq(typeOf[RootSnoopApiService], typeOf[WorkflowExecutionApiService])
  def executionServiceConstructor(): WorkflowExecutionService = executionServiceCtor()
}

@Api(value = "", description = "Snoop Base API", position = 1)
trait RootSnoopApiService extends HttpService {
  def executionServiceConstructor(): WorkflowExecutionService

  @ApiOperation(value = "Check if Snoop is alive",
    nickname = "poke",
    httpMethod = "GET",
    produces = "text/html",
    response = classOf[String])
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 500, message = "Snoop Internal Error")
  ))  
   def baseRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            <html>
              <body>
                <h1>Snoop web service is operational</h1>
              </body>
            </html>
          }
        }
      }
    } ~
    path("headers") {
      get {
        requestContext => requestContext.complete(requestContext.request.headers.mkString(",\n"))
      }
    }
}

// this trait defines our service behavior independently from the service actor
@Api(value = "workflowExecutions", description = "Snoop Workflow Execution API", position = 1)
trait WorkflowExecutionApiService extends HttpService with PerRequestCreator {
  import WorkflowExecutionJsonSupport._
  import SprayJsonSupport._

  def executionServiceConstructor(): WorkflowExecutionService

  def workflowRoutes = startWorkflowRoute ~ workflowStatusRoute

  @ApiOperation(value = "Start workflow",
    nickname = "start workflow",
    httpMethod = "POST",
    produces = "text/html",
    response = classOf[WorkflowExecution])
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 500, message = "Snoop Internal Error")
  ))  
  def startWorkflowRoute = 
    cookie("iPlanetDirectoryPro") { securityTokenCookie =>
      val securityToken = securityTokenCookie.content
      path("workflowExecutions") {
        post {
          entity(as[WorkflowExecution]) { workflowExecution =>
            requestContext =>
              perRequest(requestContext, WorkflowExecutionService.props(executionServiceConstructor), WorkflowExecutionService.WorkflowStart(workflowExecution, securityToken))
          }
        }
      }
    }

  @ApiOperation(value = "Get workflow status",
    nickname = "workflow status",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[WorkflowExecution])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "workflow id")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "successful request"),
    new ApiResponse(code = 404, message = "workflow id not found"),
    new ApiResponse(code = 500, message = "Snoop internal error")
  ))  
  def workflowStatusRoute =
    cookie("iPlanetDirectoryPro") { securityTokenCookie =>
      val securityToken = securityTokenCookie.content
      path("workflowExecutions" / Segment) { id =>
        respondWithMediaType(`application/json`) {
          requestContext =>
            perRequest(requestContext, WorkflowExecutionService.props(executionServiceConstructor), WorkflowExecutionService.WorkflowStatus(id, securityToken))
        }
      }
    }
}

object WorkflowExecutionService {
  case class WorkflowStart(workflowExecution: WorkflowExecution, securityToken: String)
  case class WorkflowStatus(id: String, securityToken: String)

  def props(executionServiceConstructor: () => WorkflowExecutionService): Props = {
    Props(executionServiceConstructor())
  }

}

trait WorkflowExecutionService extends Actor {
  val snoopSubmissionController: SnoopSubmissionController
  val callbackHandler: AnalysisCallbackHandler
  val gcsSandboxBucket: String
  val gcsSandboxKeyPrefix: String
  val outputRepository: OutputRepository

  implicit val system = context.system
  val log = Logging(system, getClass)

  val succeeded = "SUCCEEDED"

  val outputPatternsByType: Map[String, String] = Map(
    ("vcf" -> raw".+\.vcf"),
    ("bam" -> raw".+\.bam")
  )

  def submissionSandbox(submissionId: String) = s"gs://$gcsSandboxBucket/$gcsSandboxKeyPrefix/$submissionId"

  override def receive = {
    case WorkflowExecutionService.WorkflowStart(workflowExecution, securityToken) =>
      val id = UUID.randomUUID().toString
      val submissionId = start(workflowExecution.copy(id = Option(id)), securityToken, submissionSandbox(id))
      snoopSubmissionController.createSubmission(id, submissionId, workflowExecution.callbackUri, "SUBMITTED")
      context.parent ! RequestComplete(StatusCodes.Created, workflowExecution.copy(id=Option(id), status = Option("SUBMITTED")))

    case WorkflowExecutionService.WorkflowStatus(id, securityToken) =>
      val statusVal = status(id, securityToken)
      if (statusVal == succeeded) {
        val submission = snoopSubmissionController.getSubmission(id)
        if (submission.status != succeeded) {
          callbackHandler.putOutputs(submission, locateOutputs(submission))
          snoopSubmissionController.updateSubmissionStatus(id, statusVal)
        }
      }
      context.parent ! WorkflowExecution(id=Option(id), status=Option(statusVal))
  }

  /**
   * Starts a workflow execution, emits response directly to requestContext which should include
   * the id of the workflow execution
   *
   * @return the submission id in the underlying workflow engine
   */
  def start(workflowExecution: WorkflowExecution, securityToken: String, submissionSandbox: String): String
  
  /**
   * Gets status of a workflow execution, emits response directly to requestContext
   *
   * @return status
   */
  def status(id: String, securityToken: String): String

  def locateOutputs(submission: Submission): Map[String, String] = {
    val sandbox = submissionSandbox(submission.id)
    val outputs = outputRepository.listOutputs(gcsSandboxBucket, gcsSandboxKeyPrefix)

    val results = for (
      output <- outputs;
      outputTypePattern <- outputPatternsByType;
      if (output.matches(outputTypePattern._2))
    ) yield (outputTypePattern._1 -> output)

    results.toMap
  }

}

trait OutputRepository {
  def listOutputs(bucket: String, keyPrefix: String): Seq[String]
}

case class GcsOutputRepository(googleEmail: String, p12FilePath: File) extends OutputRepository {
  import scala.collection.JavaConversions._

  private val STORAGE_SCOPE = "https://www.googleapis.com/auth/devstorage.read_write"
  private val JSON_FACTORY: JacksonFactory = JacksonFactory.getDefaultInstance
  private val credential: GoogleCredential = new GoogleCredential.Builder()
    .setTransport(GoogleNetHttpTransport.newTrustedTransport)
    .setJsonFactory(JSON_FACTORY)
    .setServiceAccountId(googleEmail)
    .setServiceAccountScopes(Collections.singleton(STORAGE_SCOPE))
    .setServiceAccountPrivateKeyFromP12File(p12FilePath)
    .build

  private val storage = new Storage.Builder(GoogleNetHttpTransport.newTrustedTransport, JSON_FACTORY, credential)
    .setApplicationName("snoop")
    .build

  def listOutputs(bucket: String, keyPrefix: String): Seq[String] = {
    val list = storage.objects.list(bucket)
    list.setPrefix(keyPrefix)
    list.setMaxResults(500L)

    @tailrec
    def iteratePages(soFar: Seq[String]): Seq[String] = {
      val objects = executeWithBackOff(list.buildHttpRequest, list.getResponseClass)
      val fileObjects: Seq[StorageObject] = Option(objects.getItems: Seq[StorageObject]).getOrElse(Seq.empty[StorageObject])
      val results = for (fileObject <- fileObjects) yield { fileObject.getName }
      if (objects.getNextPageToken == null) {
        soFar ++ results
      } else {
        iteratePages(soFar ++ results)
      }
    }

    iteratePages(Vector.empty)
  }

  val backOffBuilder = new Builder()
    .setInitialIntervalMillis(100)
    .setMaxIntervalMillis(1000)
    .setMultiplier(2.5)

  private def executeWithBackOff(r: HttpRequest): HttpResponse = {
    val handler: HttpBackOffUnsuccessfulResponseHandler = new HttpBackOffUnsuccessfulResponseHandler(backOffBuilder.build)
    handler.setBackOffRequired(HttpBackOffUnsuccessfulResponseHandler.BackOffRequired.ON_SERVER_ERROR)
    r.setUnsuccessfulResponseHandler(handler)
    return r.execute
  }

  private def executeWithBackOff[T](r: HttpRequest, clazz: Class[T]): T = {
    return executeWithBackOff(r).parseAs(clazz)
  }
}
