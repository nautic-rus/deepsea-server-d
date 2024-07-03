package domain.tables.Project

import domain.deepsea.DeepseaManager.Project
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{TableQuery, Tag}

class ProjectTable(tag: Tag) extends Table[Project](tag, "issue_projects") {
  val name = column[String]("name")
  val status = column[Int]("status")
  override def * = (name, status) <> (Project.tupled, Project.unapply)
}
