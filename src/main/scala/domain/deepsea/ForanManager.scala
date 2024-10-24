package domain.deepsea

//import com.itextpdf.io.font.{FontProgramFactory, PdfEncodings}
//import com.itextpdf.kernel.font.PdfFontFactory
//import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy
//import com.itextpdf.kernel.pdf.{PdfDocument, PdfWriter}
//import com.itextpdf.layout.Document
//import com.itextpdf.layout.element.{Cell, Paragraph, Table}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import domain.DBManager.ForanDB
import domain.HttpManager.{HttpResponse, TextResponse}
import domain.deepsea.pdfGenerator.createPdf
import io.circe.generic.JsonCodec
import io.circe.syntax.EncoderOps
import org.slf4j.LoggerFactory
import slick.jdbc.GetResult
import slick.jdbc.OracleProfile.api._
import slick.lifted.{TableQuery, Tag}

import java.nio.file.Files
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ForanManager {
  private val logger = LoggerFactory.getLogger(this.toString)

  sealed trait ForanManagerMessage


  //foran oracle

  case class PrintCablesPdf(replyTo: ActorRef[HttpResponse]) extends ForanManagerMessage

  @JsonCodec case class Nodes(node: String, node_type: Int)
  @JsonCodec case class CableRoutes(cable_id: String, rout_sec: String)
  @JsonCodec case class CableNodes(cable_id: String, rout_area: String, node: String)

  case class GetCables(replyTo: ActorRef[HttpResponse]) extends ForanManagerMessage

  @JsonCodec case class Cables(seqid: Int, cable_id: String, routed_status: String, from_zone_id: String, from_zone_name: String, to_zone_id: String, to_zone_name: String, from_e_id: String, from_e_name: String, to_e_id: String, to_e_name: String, segregation: String, cable_spec: String, section: String, total_length: Float, system: String )

//  class CablesTable(tag: Tag) extends Table[Cables](tag, "V_CAB_DATA") {
//    val seqid = column[Int]("SEQID")
//    val cable_id = column[String]("CABLE_CODE")
//    val routed_status = column[String]("ROUT_STATUS")
//    val from_zone_id = column[String]("FR_ZONE_ID")
//    val from_zone_name = column[String]("FR_ZONE_DESC")
//    val to_zone_id = column[String]("TO_ZONE_ID")
//    val to_zone_name = column[String]("TO_ZONE_DESC")
//
//    val from_e_id = column[String]("FROM_E_ID")
//    val from_e_name = column[String]("FROM_E_DESCR")
//    val to_e_id = column[String]("TO_E_ID")
//    val to_e_name = column[String]("TO_E_DESCR")
//    val segregation = column[String]("SEGREGATION")
//    val cable_spec = column[String]("CABLE_SPEC")
//    val section = column[String]("NOM_SECTION")
//    val total_length = column[Float]("TOTAL_LENGTH")
//    val system = column[String]("SYSTEM_DESCR")

    class CablesTable(tag: Tag) extends Table[Cables](tag, "V_CABLE") {
    val seqid = column[Int]("SEQID")
    val cable_id = column[String]("CABLE_ID")
    val routed_status = column[String]("F_ROUT")
    val from_zone_id = column[String]("FROM_E_ZONE_NAME")
    val from_zone_name = column[String]("FROM_E_ZONE_DESCR")
    val to_zone_id = column[String]("TO_E_ZONE_NAME")
    val to_zone_name = column[String]("TO_E_ZONE_DESCR")
    val from_e_id = column[String]("FROM_E_ID")
    val from_e_name = column[String]("FROM_E_DESCR")
    val to_e_id = column[String]("TO_E_ID")
    val to_e_name = column[String]("TO_E_DESCR")
    val segregation = column[String]("SEGREGATION")
    val cable_spec = column[String]("CABLE_SPEC")
    val section = column[String]("NOM_SECTION")
    val total_length = column[Float]("TOTAL_LENGTH")
    val system = column[String]("SYSTEM_DESCR")

//        override def * = (seqid, cable_id, from_zone_id) <> (Cables.tupled, Cables.unapply)
    override def * = (seqid, cable_id, routed_status, from_zone_id, from_zone_name, to_zone_id, to_zone_name, from_e_id, from_e_name, to_e_id, to_e_name, segregation, cable_spec, section, total_length, system) <> (Cables.tupled, Cables.unapply)
  }

  lazy val CablesTable = TableQuery[CablesTable]

  def apply(): Behavior[ForanManagerMessage] = Behaviors.setup { context =>

    Behaviors.receiveMessage {
      //foran oracle
      case GetCables(replyTo) =>
        println("get cablesssss")
        getCables().onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same

      case PrintCablesPdf(replyTo) =>
        println("PrintCablesPdf cablesssss")
        printCablesPdf().onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same
    }
  }

  def getCables(): Future[Seq[Cables]] = {
    println("getCables")
    ForanDB.run(CablesTable.result)
//   val q = scala.io.Source.fromResource("queres/cables.sql").mkString
//    implicit val result = GetResult(r => Cables(r.nextInt, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextFloat, r.nextString))
//    ForanDB.run(sql"#$q".as[Cables])
  }

  def getCablesRoutes(): Future[Seq[CableRoutes]] = {
    val q = scala.io.Source.fromResource("queres/cablesRoutes.sql").mkString
    implicit val result = GetResult(r => CableRoutes(r.nextString, r.nextString()))
    ForanDB.run(sql"#$q".as[CableRoutes])
  }

  def getCablesNodes(): Future[Seq[CableNodes]] = {
    val q = scala.io.Source.fromResource("queres/cableNodes.sql").mkString
    implicit val result = GetResult(r => CableNodes(r.nextString, r.nextString(), r.nextString()))
    ForanDB.run(sql"#$q".as[CableNodes])
  }

//  def getFilteredCableNodes(): Future[Seq[CableNodes]] = {
//    println("getFilteredCableNodes")
//    val penetrationNodes = getNodesPenetration().map(_.map(_.node))
//    val allCableNodes = getCablesNodes()
//    for {
//      penetrationNodes <- penetrationNodes
//      cableNodes <- allCableNodes
//    } yield {
//      cableNodes.filter(cn =>
//        penetrationNodes.contains(cn.node) && cn.node.length == 7
//      )
//    }
//  }

  def getFilteredCableNodes() = {
    println("getFilteredCableNodes")
    val penetrationNodes = getNodesPenetration().map(_.map(_.node))
//    println(penetrationNodes);
    val allCableNodes = getCablesNodes()
    for {
      penetrationNodes <- penetrationNodes
      cableNodes <- allCableNodes
    } yield {
      cableNodes.filter(cn =>
        penetrationNodes.contains(cn.node) && cn.node.length == 7
      )
    }
  }



  private def printCablesPdf() = {
    println("SQL printCablesPdf")
//    getFilteredCableNodes().onComplete {
//      case Success(value) =>
//        println("SQL success ")
//        println(value)
//    }

    for {
      filteredNodes <- getFilteredCableNodes()
      cables <- getCables()
    } yield {
      createPdf(cables, filteredNodes)
    }



//    getFilteredCableNodes().onComplete {
//            case Success(value) =>
//              println("SQL success ")
//              println(value)
//    }
//    getCables().flatMap(cables => {
//      val a = cables
//      Future.successful(createPdf(a))
//    })
  }

  def getNodesPenetration(): Future[Seq[Nodes]] = {
    println("getNodesPenetration")
    val q = scala.io.Source.fromResource("queres/eleNodes.sql").mkString
    implicit val result = GetResult(r => Nodes(r.nextString, r.nextInt()))
    ForanDB.run(sql"#$q".as[Nodes])
  }

  def getRoutes(): Future[Seq[Nodes]] = {
    val q = scala.io.Source.fromResource("queres/eleNodes.sql").mkString
    implicit val result = GetResult(r => Nodes(r.nextString, r.nextInt()))
    ForanDB.run(sql"#$q".as[Nodes])
  }


}
