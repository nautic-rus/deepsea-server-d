package domain.deepsea

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import domain.DBManager.PostgresSQL
import domain.HttpManager.{HttpResponse, TextResponse}
import domain.tables.Project.ProjectTable
import domain.tables.Users.UserTrustTable
import io.circe.generic.JsonCodec
import io.circe.parser._
import io.circe.syntax.EncoderOps
import org.slf4j.LoggerFactory
import shapeless.Lazy.apply
import slick.jdbc.GetResult
import slick.jdbc.PostgresProfile.api._
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


  @JsonCodec case class Project(id: Int, name: String, status: Int)
  @JsonCodec case class UserTrust(id: Int, main_user_id: Int, responsible_user_id: Int, trust_action_buttons: Int)
  @JsonCodec case class Filter(id: Int, user_id: Int, name: String, value: String, showCompleted: Int)
  @JsonCodec case class Issue(id: Int, doc_number: String, issue_name: String, issue_type: String, project: String, department: String, contract: String, status: String, revision: String, period: String, contract_due_date: Long, issue_comment: String, author_comment: String, removed: Int)
  @JsonCodec case class IssueType(id: Int, type_name: String, visibility_documents: Int)
  @JsonCodec case class IssueStages(stage_name: String, stage_date: Long, id_project: Int, issue_type: String)
  @JsonCodec case class IssueInDoclist(id: Int, doc_number: String, issue_name: String, issue_type: String, project: String, department: String, contract: String, status: String, revision: String, period: String, issue_comment: String, author_comment: String, contract_due_date: Long)
  @JsonCodec case class Weight(task_id: Int, doc_number: String, issue_name: String, department: String, project: String, status: String, room: Int, room_name: String, name: String, directory_id: Int, t_weight: Int, perc : Int, x_cog: Int, y_cog: Int, z_cog: Int, mx: Int, my: Int, mz: Int, date: Long, stock_code: String)
  @JsonCodec case class IssueProjects(id: Int, name: String)

  class ProjectTable(tag: Tag) extends Table[Project](tag, "issue_projects") {
    val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val name = column[String]("name")
    val status = column[Int]("status")

    override def * = (id, name, status) <> (Project.tupled, Project.unapply)
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
    //    val contract_due_date = column[Long]("contract_due_date")
    //    val due_date = column[Long]("due_date")
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

  //"Api entry point"
  lazy val IssueTable = TableQuery[IssueTable]
  lazy val FilterSavedTable = TableQuery[FilterSavedTable]
  lazy val ProjectTable = TableQuery[ProjectTable]
  lazy val UserTrustTable = TableQuery[UserTrustTable]
  lazy val IssueTypesTable = TableQuery[IssueTypesTable]
  lazy val IssueStagesTable = TableQuery[IssueStagesTable]

  def apply(): Behavior[DeepseaManagerMessage] = Behaviors.setup { context =>
    PostgresSQL.run(DBIO.seq(
      FilterSavedTable.schema.createIfNotExists,
    ))
    Behaviors.receiveMessage {
      case GetProjectNames(replyTo) =>
        println("case GetProjectNames")
        getProjectNames().onComplete {
          case Success(value) =>
            println("success case GetProjectNames")
            println(value.asJson.noSpaces)
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            println("failure case GetFiltersSaved(replyTo)")
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same

      case GetTrustedUsers(replyTo, userId) =>
        println("case GetTrustedUsers")
        getTrustedUsers(userId).onComplete {
          case Success(value) =>
            println("success case GetTrustedUsers")
            println(value.asJson.noSpaces)
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            println("failure case GetTrustedUsers")
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same

      case GetFilterSaved(replyTo, userId) =>
        println("case GetFiltersSaved(replyTo)")
        getFiltersSaved(userId).onComplete {
          case Success(value) =>
            println("success case GetFiltersSaved(replyTo)")
            println(value.asJson.noSpaces)
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            println("failure case GetFiltersSaved(replyTo)")
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same

      case SaveFilters(json, replyTo) =>
        println("postFiltersSaved")
        decode[Filter](json) match {
          case Right(value) =>
            println("right")
            postFilter(value).onComplete {
              case Success(value) =>
                replyTo.tell(TextResponse("success".asJson.noSpaces))
              case Failure(exception) =>
                logger.error(exception.toString)
                replyTo.tell(TextResponse("server error"))
            }
            replyTo.tell(TextResponse("success".asJson.noSpaces))
          case Left(value) => {
            println("wrong json data")
            replyTo.tell(TextResponse("wrong json data".asJson.noSpaces))
          }
        }
        Behaviors.same

      case DeleteFilterSaved(replyTo, id) =>
        println("case GetFiltersSaved(replyTo)")
        deleteFilterSaved(id).onComplete {
          case Success(value) =>
            println("success case deleteFilterSaved")
            println(value.asJson.noSpaces)
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            println("failure case deleteFilterSaved")
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same
      case Str(str, replyTo) =>
        println(str)
        replyTo.tell(TextResponse(str))
        Behaviors.same

      case GetProjectDoclist(replyTo, project) =>
        println("case GetFiltersSaved(replyTo)")
        getProjectDoclist(project).onComplete {
          case Success(value) =>
            println("success case GetProjectDoclist")
//            println(value.asJson.noSpaces)
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            println("failure case GetProjectDoclist")
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same

      case GetWeightData(replyTo, project) =>
        println("case GetWeightData(replyTo)")
        getWeightData(project).onComplete {
          case Success(value) =>
            println("success case GetWeightData")
            println(value.asJson.noSpaces)
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            println("failure case GetWeightData")
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same
      case GetIssueStages(replyTo, project_id) =>
        println("caseGetIssueStages(replyTo)")
        getIssueStages(project_id).onComplete {
          case Success(value) =>
            println("success case GetIssueStages")
            println(value.asJson.noSpaces)
            replyTo.tell(TextResponse(value.asJson.noSpaces))
          case Failure(exception) =>
            println("failure case GetIssueStages")
            logger.error(exception.toString)
            replyTo.tell(TextResponse("server error"))
        }
        Behaviors.same
    }
  }

  private def getIssueStages(project_id: Int): Future[Seq[IssueStages]] = {
    PostgresSQL.run(IssueStagesTable.filter(_.id_project === project_id).result)
  }
  private def getProjectNames(): Future[Seq[Project]] = {
    //    PostgresSQL.run(ProjectTable.filter(_.status === 0).result)
    PostgresSQL.run(ProjectTable.filter(_.status === 0).filter(_.name =!= "-").result)
  }

  private def getTrustedUsers(mainUserId: Int): Future[Seq[UserTrust]] = {
    PostgresSQL.run(UserTrustTable.filter(_.main_user_id === mainUserId).filter(_.trust_action_buttons === 1).result)
  }

  private def postFilter(value: Filter): Future[Int] = {
    println(value)
    PostgresSQL.run(TableQuery[FilterSavedTable].insertOrUpdate(value))
  }

  private def deleteFilterSaved(id: Int) = {
    PostgresSQL.run(FilterSavedTable.filter(_.id === id).delete)
  }

  private def getFiltersSaved(userId: Int): Future[Seq[Filter]] = {
    PostgresSQL.run(FilterSavedTable.filter(_.user_id === userId).result)
  }

  private def getProjectDoclist(project: String): Future[Seq[IssueInDoclist]] = {
//    val q = scala.io.Source.fromResource("queres/IssueInDoclist.sql").mkString
//    println(q)
//    implicit val resultIssueInDoclist = GetResult(r => IssueInDoclist(r.nextInt, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextLong))
//
//   PostgresSQL.run(sql"$q".as[IssueInDoclist])
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
       spec.room,
       foran_data.room_name,
       materials.name,
       materials.directory_id,
       spec."t_weight",
       round(spec."t_weight"/(select sum ("t_weight")from issue_esp)*100, 1) as "%",
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
where (task_id, date) in (select task_id, max(date) from issue_esp group by task_id)and issue.project = $project
order by spec.task_id""".as[(Int, String, String, String, String, String, Int, String, String, Int, Int, Int, Int, Int, Int, Int, Int, Int, Long, String)]).map { rows =>
      rows.map {
        case (
          task_id: Int, doc_number: String, issue_name: String, department: String, project: String, status: String, room: Int, room_name: String, name: String, directory_id: Int, t_weight: Int, perc : Int, x_cog: Int, y_cog: Int, z_cog: Int, mx: Int, my: Int, mz: Int, date: Long, stock_code: String
          ) =>
          Weight(task_id: Int, doc_number: String, issue_name: String, department: String, project: String, status: String, room: Int, room_name: String, name: String, directory_id: Int, t_weight: Int, perc : Int, x_cog: Int, y_cog: Int, z_cog: Int, mx: Int, my: Int, mz: Int, date: Long, stock_code: String)
      }
    }
  }
}
