package domain.deepsea

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import domain.DBManager.{ForanDB, PostgresSQL}
import domain.HttpManager.{HttpResponse, TextResponse}
import domain.deepsea.ForanManager.GetCables
import io.circe.generic.JsonCodec
import io.circe.parser._
import io.circe.syntax.EncoderOps
import org.slf4j.LoggerFactory
import slick.jdbc.PostgresProfile.api._
//import slick.jdbc.OracleProfile.api._
import slick.jdbc.GetResult
import slick.lifted.{TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object DeepseaManager {
  private val logger = LoggerFactory.getLogger(this.toString)

  sealed trait DeepseaManagerMessage

  case class GetProjectNames(replyTo: ActorRef[HttpResponse]) extends DeepseaManagerMessage
  case class GetTrustedUsers(replyTo: ActorRef[HttpResponse], userId: Int) extends DeepseaManagerMessage
  case class GetWeightData(replyTo: ActorRef[HttpResponse], project: String) extends DeepseaManagerMessage
  case class GetIssueStages(replyTo: ActorRef[HttpResponse], project_id: Int) extends DeepseaManagerMessage
  case class SaveFilters(json: String, replyTo: ActorRef[HttpResponse]) extends DeepseaManagerMessage
  case class GetFilterSaved(replyTo: ActorRef[HttpResponse], userId: Int) extends DeepseaManagerMessage
  case class DeleteFilterSaved(replyTo: ActorRef[HttpResponse], id: Int) extends DeepseaManagerMessage
  case class GetProjectDoclist(replyTo: ActorRef[HttpResponse], project: String) extends DeepseaManagerMessage
  case class Str(str: String, replyTo: ActorRef[HttpResponse]) extends DeepseaManagerMessage
  case class GetSpecMaterials(replyTo: ActorRef[HttpResponse]) extends DeepseaManagerMessage
  case class GetMaterialsDirectory(replyTo: ActorRef[HttpResponse], project_id: Int) extends DeepseaManagerMessage
  case class SaveHullEsp(json: String, replyTo: ActorRef[HttpResponse]) extends DeepseaManagerMessage


  //foran oracle
//  case class GetCables(replyTo: ActorRef[HttpResponse]) extends DeepseaManagerMessage
//
////  @JsonCodec case class Cables(seqid: Int, from_e: Int, to_e: Int)
//@JsonCodec case class Cables(seqid: Int, code: String)
//  class CablesTable(tag: Tag) extends slick.jdbc.OracleProfile.api.Table[Cables](tag, "cable") {
//    val seqid = column[Int]("seqid")
//    val code = column[String]("code")
////    val from_e = column[Int]("from_e")
////    val to_e = column[Int]("to_e")
//
//    override def * = (seqid, code) <> (Cables.tupled, Cables.unapply)
////    override def * = (seqid, from_e, to_e) <> (Cables.tupled, Cables.unapply)
//  }
//
//  lazy val CablesTable = TableQuery[CablesTable]



  @JsonCodec case class Project(id: Int, name: String, status: Int)
  @JsonCodec case class UserTrust(id: Int, main_user_id: Int, responsible_user_id: Int, trust_action_buttons: Int)
  @JsonCodec case class Filter(id: Int, user_id: Int, name: String, value: String, showCompleted: Int)
  @JsonCodec case class Issue(id: Int, doc_number: String, issue_name: String, issue_type: String, project: String, department: String, contract: String, status: String, revision: String, period: String, contract_due_date: Long, issue_comment: String, author_comment: String, removed: Int)
  @JsonCodec case class IssueType(id: Int, type_name: String, visibility_documents: Int)
  @JsonCodec case class IssueStages(stage_name: String, stage_date: Long, id_project: Int, issue_type: String)
  @JsonCodec case class IssueInDoclist(id: Int, doc_number: String, issue_name: String, issue_type: String, project: String, department: String, contract: String, status: String, revision: String, period: String, issue_comment: String, author_comment: String, contract_due_date: Long)
  @JsonCodec case class Weight(task_id: Int, doc_number: String, issue_name: String, department: String, project: String, status: String, room: String, name: String, directory_id: Int, directory_name: String, t_weight: Int, perc : Int, x_cog: Int, y_cog: Int, z_cog: Int, mx: Int, my: Int, mz: Int, modify_date: Long, stock_code: String)
  @JsonCodec case class IssueProjects(id: Int, name: String)
  @JsonCodec case class SpecMaterial(code: String, name: String, descr: String, units: String, weight: Double, statem_id: Int, dir_id: Int, user_id: Int, label: String, last_upd: Long, note: String, manufacturer: String, coef: Double, id: Int, removed: Int, supplier: String, supplier_id: Int, equ_id: Int)
  @JsonCodec case class MaterialsDirectory(id: Int, name: String, parent_id: Int, user_id: Int, date: Long, old_code: String, project_id: Int, removed: Int);
  @JsonCodec case class IssueEsp(id: Int, task_id: Int, user_id: Int, rev: String, label: String, qty: Long, t_weight: Long, foran_data_id: Int, materials_id: Int, date: Long)
  @JsonCodec case class ForanData(id: Int, x_cog: Long, y_cog: Long, z_cog: Long, note: String, room: String, part_type: String, category: Int, part_oid: String, length: Long, width: Long, symmetry: String);
  class ProjectTable(tag: Tag) extends Table[Project](tag, "issue_projects") {
    val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val name = column[String]("name")
    val status = column[Int]("status")

    override def * = (id, name, status) <> (Project.tupled, Project.unapply)
  }

  class MaterialsDirectoryTable(tag: Tag) extends Table[MaterialsDirectory](tag, "materials_directory") {
    val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val name = column[String]("name")
    val parent_id = column[Int]("parent_id")
    val user_id = column[Int]("user_id")
    val date = column[Long]("date")
    val old_code = column[String]("old_code")
    val project_id = column[Int]("project_id")
    val removed = column[Int]("removed")

    override def * = (id, name, parent_id, user_id, date, old_code, project_id, removed) <> (MaterialsDirectory.tupled, MaterialsDirectory.unapply)
  }

  class IssueStagesTable(tag: Tag) extends Table[IssueStages](tag, "issue_stages") {
    val stage_name = column[String]("stage_name")
    val stage_date = column[Long]("stage_date")
    val id_project = column[Int]("id_project")
    val issue_type = column[String]("issue_type")

    override def * = (stage_name, stage_date, id_project, issue_type) <> (IssueStages.tupled, IssueStages.unapply)
  }

  class UserTrustTable(tag: Tag) extends Table[UserTrust](tag, "user_trust") {
    val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val main_user_id = column[Int]("main_user_id")
    val responsible_user_id = column[Int]("responsible_user_id")
    val trust_action_buttons = column[Int]("trust_action_buttons")

    override def * = (id, main_user_id, responsible_user_id, trust_action_buttons) <> (UserTrust.tupled, UserTrust.unapply)
  }

  class FilterSavedTable(tag: Tag) extends Table[Filter](tag, "filters_saved") {
    val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val user_id = column[Int]("user_id")
    val name = column[String]("name")
    val value = column[String]("value")
    val showCompleted = column[Int]("show_completed")

    override def * = (id, user_id, name, value, showCompleted) <> (Filter.tupled, Filter.unapply)
  }


  class IssueTable(tag: Tag) extends Table[Issue](tag, "issue") {
    val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val doc_number = column[String]("doc_number")
    val issue_name = column[String]("issue_name")
    val issue_type = column[String]("issue_type")
    val project = column[String]("project")
    val department = column[String]("department")
    val contract = column[String]("contract")
    val status = column[String]("status")
    val revision = column[String]("revision")
    val period = column[String]("period")
    val contract_due_date = column[Long]("contract_due_date")
    val issue_comment = column[String]("issue_comment")
    val author_comment = column[String]("author_comment")
    val removed = column[Int]("removed")

    override def * = (id, doc_number, issue_name, issue_type, project, department, contract, status, revision, period, contract_due_date, issue_comment, author_comment, removed) <> (Issue.tupled, Issue.unapply)

  }

  class IssueTypesTable(tag: Tag) extends Table[IssueType](tag, "issue_types") {
    val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val type_name = column[String]("type_name")
    val visibility_documents = column[Int]("visibility-documents")

    override def * = (id, type_name, visibility_documents) <> (IssueType.tupled, IssueType.unapply)
  }

//  добавление спецификаций в постгресс
  class IssueEspTable(tag: Tag) extends Table[IssueEsp](tag, "issue_esp") {
    val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val task_id = column[Int]("task_id")
    val user_id = column[Int]("user_id")
    val rev = column[String]("rev")
    val label = column[String]("label")
    val qty = column[Long]("qty")
    val t_weight = column[Long]("t_weight")
    val foran_data_id = column[Int]("foran_data_id")
    val materials_id = column[Int]("materials_id")
    val date = column[Long]("date")

    override def * = (id, task_id, user_id, rev, label, qty, t_weight, foran_data_id, materials_id, date) <> (IssueEsp.tupled, IssueEsp.unapply)
  }

//  @JsonCodec case class ForanData(symmetry: String);
  class ForanDataTable(tag: Tag) extends Table[ForanData](tag, "foran_data") {
    val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val x_cog = column[Long]("x_cog")
    val y_cog = column[Long]("y_cog")
    val z_cog = column[Long]("z_cog")
    val note = column[String]("note")
    val room = column[String]("room")
    val part_type = column[String]("part_type")
    val category = column[Int]("category")
    val part_oid = column[String]("part_oid")
    val length = column[Long]("length")
    val width = column[Long]("width")
    val symmetry = column[String]("symmetry")

    override def * = (id, x_cog, y_cog, z_cog, note, room, part_type, category, part_oid, length, width, symmetry) <> (ForanData.tupled, ForanData.unapply)
  }

  //"Api entry point"
  lazy val IssueTable = TableQuery[IssueTable]
  lazy val FilterSavedTable = TableQuery[FilterSavedTable]
  lazy val ProjectTable = TableQuery[ProjectTable]
  lazy val UserTrustTable = TableQuery[UserTrustTable]
  lazy val IssueTypesTable = TableQuery[IssueTypesTable]
  lazy val IssueStagesTable = TableQuery[IssueStagesTable]
  lazy val MaterialsDirectoryTable = TableQuery[MaterialsDirectoryTable]
  lazy val IssueEspTable = TableQuery[IssueEspTable]
  lazy val ForanDataTable = TableQuery[ForanDataTable]

  def apply(): Behavior[DeepseaManagerMessage] = Behaviors.setup { context =>
    PostgresSQL.run(DBIO.seq(
      FilterSavedTable.schema.createIfNotExists,
    ))
//    ForanDB.run(DBIO.seq())
    Behaviors.receiveMessage {
      case GetProjectNames(replyTo) =>
        getProjectNames().onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same

      case GetTrustedUsers(replyTo, userId) =>
        getTrustedUsers(userId).onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same

      case GetFilterSaved(replyTo, userId) =>
        getFiltersSaved(userId).onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same

      case SaveFilters(json, replyTo) =>
        decode[Filter](json) match {
          case Right(value) =>
            postFilter(value).onComplete {
              case Success(value) =>
                replyTo.tell(TextResponse("success".asJson.noSpaces))
              case Failure(exception) =>
                logger.error(exception.toString)
                replyTo.tell(TextResponse("server error"))
            }
            replyTo.tell(TextResponse("success".asJson.noSpaces))
          case Left(value) => {
            replyTo.tell(TextResponse("wrong json data".asJson.noSpaces))
          }
        }
        Behaviors.same

      case DeleteFilterSaved(replyTo, id) =>
        deleteFilterSaved(id).onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same
      case Str(str, replyTo) =>
        replyTo.tell(TextResponse(str))
        Behaviors.same

      case GetProjectDoclist(replyTo, project) =>
        getProjectDoclist(project).onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same

      case GetWeightData(replyTo, project) =>
        getWeightData(project).onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same
      case GetIssueStages(replyTo, project_id) =>
        getIssueStages(project_id).onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same
      case GetSpecMaterials(replyTo) =>
        getSpecMaterials().onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same

      case GetMaterialsDirectory(replyTo, project_id) =>
        getMaterialsDirectory(project_id).onComplete {
          case Success(value) =>
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same

      case SaveHullEsp(json, replyTo) =>
        println(json)
        replyTo.tell(TextResponse("server error"))
//        decode[Filter](json) match {
//          case Right(value) =>
//            postFilter(value).onComplete {
//              case Success(value) =>
//                replyTo.tell(TextResponse("success".asJson.noSpaces))
//              case Failure(exception) =>
//                logger.error(exception.toString)
//                replyTo.tell(TextResponse("server error"))
//            }
//            replyTo.tell(TextResponse("success".asJson.noSpaces))
//          case Left(value) => {
//            replyTo.tell(TextResponse("wrong json data".asJson.noSpaces))
//          }
//        }
        Behaviors.same


        //foran oracle
//      case GetCables(replyTo) =>
//        println("get cablesssss")
//        getCables().onComplete {
//          case Success(value) =>
//            println(value)
//            replyTo.tell(TextResponse(value.asJson.noSpaces))
//          case Failure(exception) =>
//            logger.error(exception.toString)
//            replyTo.tell(TextResponse("server error"))
//        }
//        Behaviors.same


    }
  }

//  private def getCables(): Future[Seq[Cables]] = {
//    println("getCables")
//    ForanDB.run(CablesTable.result)
//  }

//  private def getCables(): Future[Seq[Cables]] = {
//    println("getCables")
//    ForanDB.run(CablesTable.map(row => Cables(row.seqid, row.code)).result)
//  }

  private def getIssueStages(project_id: Int): Future[Seq[IssueStages]] = {
    PostgresSQL.run(IssueStagesTable.filter(_.id_project === project_id).result)
  }

  def getSpecMaterials(): Future[Seq[SpecMaterial]] = {
    val q = scala.io.Source.fromResource("queres/getSpecMaterials.sql").mkString
    implicit val result = GetResult(r => SpecMaterial(r.nextString, r.nextString, r.nextString, r.nextString, r.nextDouble, r.nextInt,
      r.nextInt, r.nextInt, r.nextString, r.nextLong, r.nextString, r.nextString, r.nextDouble, r.nextInt, r.nextInt, Option(r.nextString).getOrElse(""), Option(r.nextInt).getOrElse(-1), Option(r.nextInt).getOrElse(-1)))
    PostgresSQL.run(sql"#$q".as[SpecMaterial])
  }

  private def getProjectNames(): Future[Seq[Project]] = {
    //    PostgresSQL.run(ProjectTable.filter(_.status === 0).result)
    PostgresSQL.run(ProjectTable.filter(_.status === 0).filter(_.name =!= "-").result)
  }

  private def getMaterialsDirectory(projectId: Int): Future[Seq[MaterialsDirectory]] = {
    PostgresSQL.run(MaterialsDirectoryTable.filter(_.removed =!= 1).filter(_.project_id === projectId).result)
  }

  private def getTrustedUsers(mainUserId: Int): Future[Seq[UserTrust]] = {
    PostgresSQL.run(UserTrustTable.filter(_.main_user_id === mainUserId).filter(_.trust_action_buttons === 1).result)
  }

  private def postFilter(value: Filter): Future[Int] = {
    PostgresSQL.run(TableQuery[FilterSavedTable].insertOrUpdate(value))
  }

  private def deleteFilterSaved(id: Int) = {
    PostgresSQL.run(FilterSavedTable.filter(_.id === id).delete)
  }

  private def getFiltersSaved(userId: Int): Future[Seq[Filter]] = {
    PostgresSQL.run(FilterSavedTable.filter(_.user_id === userId).result)
  }

  private def getProjectDoclist(project: String): Future[Seq[IssueInDoclist]] = {
    println("getProjectDoclist");
    PostgresSQL.run(sql"""SELECT issue.id, doc_number, issue_name, issue_type, project, department, contract, issue.status, revision, period, issue_comment, author_comment, (select stage_date as contract_due_date from issue_stages where issue_stages.issue_type = issue.issue_type and issue_stages.stage_name = period and issue_stages.id_project = ip.id)
                           FROM issue
                           LEFT JOIN issue_types ON issue.issue_type = issue_types.type_name
                           RIGHT JOIN issue_projects as ip ON project = ip.name
                           WHERE issue_types."visibility-documents" = 1 AND removed = 0 AND project = $project""".as[(Int, String, String, String, String, String, String, String, String, String, String, String, Long)]).map { rows =>
      rows.map {
        case (
          id, doc_number, issue_name, issue_type: String, project: String, department: String,  contract: String, status: String, revision: String, period: String, issue_comment: String, author_comment: String, contract_due_date: Long
          ) =>
          IssueInDoclist(id, doc_number, issue_name, issue_type: String, project: String, department: String,  contract: String, status: String, revision: String, period: String, issue_comment: String, author_comment: String, contract_due_date: Long)
      }
    }
  }

  private def getWeightData(project: String): Future[Seq[Weight]] = {
    PostgresSQL.run(sql"""select spec.task_id,
       issue.doc_number,
       issue.issue_name,
       issue.department,
       issue.project,
       issue.status,
       foran_data.room,
       materials.name,
       materials.directory_id,
       materials_directory.name,
       spec."t_weight",
       round(spec."t_weight"/(select sum ("t_weight")from issue_esp)*100, 1) as perc,
       foran_data.x_cog,
       foran_data.y_cog,
       foran_data.z_cog,
       spec."t_weight"*foran_data.x_cog as "Mx",
       spec."t_weight"*foran_data.y_cog as "My",
       spec."t_weight"*foran_data.z_cog as "Mz",
       spec.date,
       materials.stock_code
from issue_esp as spec
         inner join issue on issue.id = spec.task_id
         inner join materials on spec.materials_id = materials.id
         inner join foran_data on spec.foran_data_id = foran_data.id
         inner join materials_directory on materials.directory_id = materials_directory.id
where (spec.task_id, spec.date) in (select task_id, max(date) from issue_esp group by task_id)and issue.project = $project
order by spec.task_id""".as[(Int, String, String, String, String, String, String, String, Int, String, Int, Int, Int, Int, Int, Int, Int, Int, Long, String)]).map { rows =>
      rows.map {
        case (
          task_id: Int, doc_number: String, issue_name: String, department: String, project: String, status: String, room: String, name: String, directory_id: Int,  directory_name: String, t_weight: Int, perc : Int, x_cog: Int, y_cog: Int, z_cog: Int, mx: Int, my: Int, mz: Int, modify_date: Long, stock_code: String
          ) =>
          Weight(task_id: Int, doc_number: String, issue_name: String, department: String, project: String, status: String,  room: String, name: String, directory_id: Int, directory_name: String, t_weight: Int, perc : Int, x_cog: Int, y_cog: Int, z_cog: Int, mx: Int, my: Int, mz: Int, modify_date: Long, stock_code: String)
      }
    }
  }
}
