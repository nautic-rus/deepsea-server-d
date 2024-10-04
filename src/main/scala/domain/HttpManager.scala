package domain



import org.apache.pekko.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import com.typesafe.config.ConfigFactory
import domain.deepsea.{DeepseaManager, ForanManager}
import domain.deepsea.DeepseaManager.{DeleteFilterSaved, GetFilterSaved, GetIssueStages, GetMaterialsDirectory, GetProjectDoclist, GetProjectNames, GetSpecMaterials, GetTrustedUsers, GetWeightData, SaveFilters, SaveHullEsp, Str}
import domain.deepsea.ForanManager.GetCables
//import domain.deepsea.ForanManager.GetCables
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, HOURS}
import scala.util.{Failure, Success}
import org.apache.pekko.http.cors.scaladsl.CorsDirectives._

object HttpManager {
  private val logger = LoggerFactory.getLogger("http")
  private val config = ConfigFactory.load()

  trait HttpResponse

  case class TextResponse(value: String) extends HttpResponse

  def apply(system: ActorSystem[Nothing], deepsea: ActorRef[DeepseaManager.DeepseaManagerMessage], foran: ActorRef[ForanManager.ForanManagerMessage]): Future[Http.ServerBinding] = {
    try {
      implicit val sys: ActorSystem[Nothing] = system
      implicit val timeout: Timeout = Duration(1, HOURS)
      val route: Route = cors() {
        concat(
          (post & path("saveFilters") & entity(as[String])) { (json) =>
            forward(deepsea.ask(ref => SaveFilters(json, ref)))
          },
          (get & path("filtersSaved")  & parameter("userId")) { userId =>
            println(userId);
            forward(deepsea.ask(ref => GetFilterSaved(ref, userId.toInt)))
          },
          (get & path("deleteFilterSaved") & parameter("id")) { id =>
            forward(deepsea.ask(ref => DeleteFilterSaved(ref, id.toInt)))
          },
          (get & path("str") & entity(as[String])) { (json) =>
            forward(deepsea.ask(ref => Str(json, ref)))
          },
          (get & path("projectDoclist")  & parameter("project")) { project =>  //получаем данные для doclist по названию проекта
            forward(deepsea.ask(ref => GetProjectDoclist(ref, project)))
          },
          (get & path("projectNames")) {
            forward(deepsea.ask(ref => GetProjectNames(ref)))
          },
          (get & path("trustedUsers") & parameter("id")) { id =>
            forward(deepsea.ask(ref => GetTrustedUsers(ref, id.toInt)))
          },
          (get & path("weightData")  & parameter("project")) { project =>
            forward(deepsea.ask(ref => GetWeightData(ref, project)))
          },
          (get & path("issueStages")  & parameter("project")) { project =>
            forward(deepsea.ask(ref => GetIssueStages(ref, project.toInt)))
          },
          (get & path("specMaterials")) {
            forward(deepsea.ask(ref => GetSpecMaterials(ref)))
          },
          (get & path("materialsDirectory") & parameter("projectId") ) { projectId =>
            forward(deepsea.ask(ref => GetMaterialsDirectory(ref, projectId.toInt)))
          },
          (post & path("createHullEspPostgres") & entity(as[String])) { (json) =>
            println(json)
            forward(deepsea.ask(ref => SaveHullEsp(json, ref)))
          },
          (get & path("cables")) {
            forward(foran.ask(ref => GetCables(ref)))
          },

          //foran oracle

        )
      }
      logger.info("http started at " + config.getString("http.host") + ":" + config.getString("http.port"))
      Http().newServerAt(config.getString("http.host"), config.getInt("http.port")).bind(route)
    }
    catch {
      case e: Throwable =>
        println(e.toString)
        Thread.sleep(5 * 1000)
        HttpManager(system, deepsea, foran)
    }
  }

  private def forward(future: Future[Any]): Route = {
    try {
      onComplete(future) {
        case Success(value) => value match {
          case TextResponse(value) => complete(HttpEntity(value))
          case _ =>
            logger.error("unidentified message received from actor")
            complete(HttpEntity("unidentified message received from actor"))
        }
        case Failure(exception) =>
          logger.error(exception.toString)
          complete(HttpEntity(exception.toString))
      }
    }
    catch {
      case e: Throwable =>
        complete(HttpEntity(e.toString))
    }
  }

}
