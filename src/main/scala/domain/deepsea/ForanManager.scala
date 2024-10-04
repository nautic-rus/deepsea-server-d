package domain.deepsea

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import domain.DBManager.{ForanDB}
import domain.HttpManager.{HttpResponse, TextResponse}
import io.circe.generic.JsonCodec
import io.circe.syntax.EncoderOps
import org.slf4j.LoggerFactory
import slick.jdbc.OracleProfile.api._
import slick.lifted.{TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ForanManager {
  private val logger = LoggerFactory.getLogger(this.toString)

  sealed trait ForanManagerMessage


  //foran oracle
  case class GetCables(replyTo: ActorRef[HttpResponse]) extends ForanManagerMessage

  @JsonCodec case class Cables(seqid: Int, cable_id: String, routed_status: String, from_zone_id: String, from_zone_name: String, to_zone_id: String, to_zone_name: String, from_e_id: String, from_e_name: String, to_e_id: String, to_e_name: String, segregation: String, cable_spec: String)
//@JsonCodec case class Cables(seqid: Int, cable_id: String, from_zone_id: String)
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

//    override def * = (seqid, cable_id, from_zone_id) <> (Cables.tupled, Cables.unapply)
    override def * = (seqid, cable_id, routed_status, from_zone_id, from_zone_name, to_zone_id, to_zone_name, from_e_id, from_e_name, to_e_id, to_e_name, segregation, cable_spec) <> (Cables.tupled, Cables.unapply)
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


    }
  }

  private def getCables(): Future[Seq[Cables]] = {
    println("getCables")
    ForanDB.run(CablesTable.result)
  }

  //  private def getCables(): Future[Seq[Cables]] = {
  //    println("getCables")
  //    ForanDB.run(CablesTable.map(row => Cables(row.seqid, row.code)).result)
  //  }


}
