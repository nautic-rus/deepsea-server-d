package domain.deepsea

import domain.DBManager.{EleComplectEncoder, codecRegistry, mongoDB}
import domain.HttpManager.{HttpResponse, TextResponse}
import io.circe.syntax.EncoderOps
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters.equal
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object MongoEleManager {
  private val logger = LoggerFactory.getLogger(this.toString)
  sealed trait MongoEleManagerMessage

  case class GetEleComplects(replyTo: ActorRef[HttpResponse], project: String) extends MongoEleManagerMessage

  case class EleComplect(drawingId: String = "", drawingDescr: String = "", deck: String = "", project: String = "P701", systemNames: List[String] = List.empty[String], zoneNames: List[String] = List.empty[String])

  def apply(): Behavior[MongoEleManagerMessage] = Behaviors.setup { context =>

    Behaviors.receiveMessage {
      case GetEleComplects(replyTo, project) =>
        println("GetEleComplects")
        getEleComplects(project).onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse(s"Server error: ${exception.getMessage}"))
        }
        Behaviors.same
    }
  }

  def getEleComplects(project: String): Future[List[EleComplect]] = {
    println("getEleComplects")
    val espCollection: MongoCollection[EleComplect] = mongoDB.getCollection("eleComplects")
    espCollection.find(equal("project", project)).toFuture().map(_.toList)
  }
}