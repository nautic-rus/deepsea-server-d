package domain.tables.Project

import domain.deepsea.DeepseaManager.Project
import io.circe.generic.JsonCodec
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{TableQuery, Tag}

//@JsonCodec case class Project(id: Int, name: String, status: Int)

class ProjectTable(tag: Tag) extends Table[Project](tag, "issue_projects") {
  val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
  val name = column[String]("name")
  val status = column[Int]("status")

  override def * = (id, name, status) <> (Project.tupled, Project.unapply)
}
