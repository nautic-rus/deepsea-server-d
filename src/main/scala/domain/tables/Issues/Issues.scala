//package domain.tables.Issues
//
//package domain.tables
//
//
//import slick.lifted.Tag
//import slick.jdbc.PostgresProfile.api._
//
//
//class IssueTable(tag: Tag) extends Table[Issue](tag, "issue") {
//  val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
//  val doc_number = column[String]("doc_number")
//  val issue_name = column[String]("issue_name")
//  val issue_type = column[String]("issue_type")
//  val project = column[String]("project")
//  val department = column[String]("department")
//  val contract = column[String]("contract")
//  val status = column[String]("status")
//  val revision = column[String]("revision")
//  val period = column[String]("period")
//  val contract_due_date = column[Long]("contract_due_date")
//  //    val contract_due_date = column[Long]("contract_due_date")
//  //    val due_date = column[Long]("due_date")
//  val issue_comment = column[String]("issue_comment")
//  val author_comment = column[String]("author_comment")
//  val removed = column[Int]("removed")
//
//  override def * = (id, doc_number, issue_name, issue_type, project, department, contract, status, revision, period, contract_due_date, issue_comment, author_comment, removed) <> (Issue.tupled, Issue.unapply)
//
//}
//
//class IssueTypesTable(tag: Tag) extends Table[IssueType](tag, "issue_types") {
//  val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
//  val type_name = column[String]("type_name")
//  val visibility_documents = column[Int]("visibility-documents")
//
//  override def * = (id, type_name, visibility_documents) <> (IssueType.tupled, IssueType.unapply)
//}
//
//class IssueStagesTable(tag: Tag) extends Table[IssueStages](tag, "issue_stages") {
//  val stage_name = column[String]("stage_name")
//  val stage_date = column[Long]("stage_date")
//  val id_project = column[Int]("id_project")
//  val issue_type = column[String]("issue_type")
//
//  override def * = (stage_name, stage_date, id_project, issue_type) <> (IssueStages.tupled, IssueStages.unapply)
//}
//
