package domain.deepsea

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


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ForanManager {
  private val logger = LoggerFactory.getLogger(this.toString)

  sealed trait ForanManagerMessage


  //foran oracle

  case class CablesPdfURL(replyTo: ActorRef[HttpResponse]) extends ForanManagerMessage
  case class PrintCablesPdf(replyTo: ActorRef[HttpResponse], url: String) extends ForanManagerMessage
  case class GetCables(replyTo: ActorRef[HttpResponse]) extends ForanManagerMessage


  @JsonCodec case class Nodes(node: String, node_type: Int)
  @JsonCodec case class CableNodes(cable_id: String, rout_area: String, node: String)
  @JsonCodec case class CablesPdf(seqid: Long, cable_id: String, routed_status: String, from_zone_id: String, from_zone_name: String, to_zone_id: String, to_zone_name: String, from_e_id: String, from_e_name: String, to_e_id: String, to_e_name: String, segregation: String, cable_spec: String, section: String, total_length: Float, system: String, cable_spec_short: String )
  @JsonCodec case class Cables(seqid: Long, cable_id: String, routed_status: String, from_zone_id: String, from_zone_name: String, to_zone_id: String, to_zone_name: String, from_e_id: String, from_e_name: String, to_e_id: String, to_e_name: String, segregation: String, cable_spec: String, section: String, total_length: Float, system: String)
  @JsonCodec case class CableRoutesList(cable_cod: String, rout_sec: String)

  //  рабочий код
    class CablesTable(tag: Tag) extends Table[Cables](tag, "V_CABLE") {
    val seqid = column[Long]("SEQID")
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
    override def * = (seqid, cable_id, routed_status, from_zone_id, from_zone_name, to_zone_id, to_zone_name, from_e_id, from_e_name, to_e_id, to_e_name, segregation, cable_spec, section, total_length, system) <> (Cables.tupled, Cables.unapply)
  }

  lazy val CablesTable = TableQuery[CablesTable]

  def apply(): Behavior[ForanManagerMessage] = Behaviors.setup { context =>

    Behaviors.receiveMessage {
      case GetCables(replyTo) =>
        println("get cablesssss")
        getCables().onComplete {
          case Success(value) =>
            if (value == null) {
              logger.error("null")
              replyTo.tell(TextResponse("server error"))
            } else {
              println(value.length)
              replyTo.tell(TextResponse(value.asJson.noSpaces))
            }
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same

      case CablesPdfURL(replyTo) =>  //загружаю файл на сервер и возвращаю ссылку на загруженный файл
        println("PrintCablesPdf cablesssss URL")
        getCablesPdfURL().onComplete {
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
  }
  //получаю данные по кабелям через sql чтобы присоединить последний столбец с коротким вариантом марки кабеля
  def getCablesPdf(): Future[Seq[CablesPdf]] = {
    println("getCablesPdf")
    val q = scala.io.Source.fromResource("queres/cables.sql").mkString
    implicit val result = GetResult(r => CablesPdf(r.nextLong, Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse(""), r.nextFloat, Option(r.nextString).getOrElse(""), Option(r.nextString).getOrElse("")))
    ForanDB.run(sql"#$q".as[CablesPdf])
  }

  def getCablesNodes(): Future[Seq[CableNodes]] = {
    val q = scala.io.Source.fromResource("queres/cableNodes.sql").mkString
    implicit val result = GetResult(r => CableNodes(r.nextString, r.nextString(), r.nextString()))
    ForanDB.run(sql"#$q".as[CableNodes])
  }

  def getCablesRoutesList(): Future[Seq[CableRoutesList]] = {   ////
    val q = scala.io.Source.fromResource("queres/cablesRoutes.sql").mkString
    implicit val result = GetResult(r => CableRoutesList(r.nextString, r.nextString))
    ForanDB.run(sql"#$q".as[CableRoutesList])
  }

  //провреяю явлется ли нода кабеля типа пенетрейшн и длина = 7 символам (правило  от электриков)
  def getFilteredCableNodes() = {
    println("getFilteredCableNodes")
    val penetrationNodes = getNodesPenetration().map(_.map(_.node))  //получаю кабели типа пенетрейшн
    val allCableNodes = getCablesNodes()  //получаю все ноды кабеля (сам кабель, его ноду и ...)
    for {
      penetrationNodes <- penetrationNodes
      cableNodes <- allCableNodes
    } yield {
      cableNodes.filter(cn =>
        penetrationNodes.contains(cn.node) && cn.node.length == 7
      )
    }
  }



  private def getCablesPdfURL() = {
    println("URL printCablesPdf")
    for {
      filteredNodes <- getFilteredCableNodes()
      cables <- getCablesPdf()
      cablesRoutesList <- getCablesRoutesList()
    } yield {
      createPdf(cables, filteredNodes, cablesRoutesList)
    }
  }

  def getNodesPenetration(): Future[Seq[Nodes]] = {  //получаю кабели из таблицы пенетрейшн, чтобы в дальнейшем проверить входит ли роут туда (является ли типа пенетрейшн)
    println("getNodesPenetration")
    val q = scala.io.Source.fromResource("queres/eleNodes.sql").mkString
    implicit val result = GetResult(r => Nodes(r.nextString, r.nextInt()))
    ForanDB.run(sql"#$q".as[Nodes])
  }
}
