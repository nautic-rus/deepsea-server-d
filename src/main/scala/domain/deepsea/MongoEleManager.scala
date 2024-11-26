package domain.deepsea

import domain.DBManager.{EleComplectEncoder, codecRegistry, mongoDB}
import domain.HttpManager.{HttpResponse, TextResponse}
import domain.deepsea.ForanManager.{getCablesPdf, getFilteredCableNodes}
import domain.deepsea.pdfEleComplectGenerator.createEleComplectPdf
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
  case class CreateEleComplectPdf(replyTo: ActorRef[HttpResponse], drawingId: String) extends MongoEleManagerMessage

  case class EleComplect(drawingId: String = "", drawingDescr: String = "", deck: String = "", project: String = "P701", systemNames: List[String] = List.empty[String], zoneNames: List[String] = List.empty[String])  //комплекты из монги со стр tools/ele

  def apply(): Behavior[MongoEleManagerMessage] = Behaviors.setup { context =>

    Behaviors.receiveMessage {
      case GetEleComplects(replyTo, project) =>
        println("GetEleComplects")
        getEleComplectsByProject(project).onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse(s"Server error: ${exception.getMessage}"))
        }
        Behaviors.same

      case CreateEleComplectPdf(replyTo, drawingId) =>  //генерирую и загружаю файл на сервер и возвращаю ссылку на загруженный файл
        println("PrintCablesPdf cablesssss URL")
        getEleComplectPdfURL(drawingId).onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same
    }
  }

  def getEleComplectsByProject(project: String): Future[List[EleComplect]] = {
    println("getEleComplects")
    val espCollection: MongoCollection[EleComplect] = mongoDB.getCollection("eleComplects")
    espCollection.find(equal("project", project)).toFuture().map(_.toList)
  }

  def getEleComplectsByDrawingId(drawingId: String): Future[List[EleComplect]] = {
    println("getEleComplects")
    val espCollection: MongoCollection[EleComplect] = mongoDB.getCollection("eleComplects")
    espCollection.find(equal("drawingId", drawingId)).toFuture().map(_.toList)
  }

//  def getEleComplectsByDrawingId(drawingId: String): Future[EleComplect] = {
//    println("getEleComplects")
//    val espCollection: BSONCollection = mongoDB.getCollection("eleComplects")
//    espCollection.find(BSONDocument("drawingId" -> drawingId)).one[EleComplect].map(_.head)
//  }



  def getEleComplectPdfURL(drawingId: String): Future[String] = {
    for {
      cables <- getCablesPdf()
      filteredNodes <- getFilteredCableNodes()
      complect <- getEleComplectsByDrawingId(drawingId)
    } yield {
      createEleComplectPdf(cables, complect, filteredNodes)
    }
  }
}