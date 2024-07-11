package domain.tables.Users

import domain.deepsea.DeepseaManager.UserTrust
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

class UserTrustTable(tag: Tag) extends Table[UserTrust](tag, "user_trust") {
  val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
  val main_user_id = column[Int]("main_user_id")
  val responsible_user_id = column[Int]("responsible_user_id")
  val trust_action_buttons = column[Int]("trust_action_buttons")

  override def * = (id, main_user_id, responsible_user_id, trust_action_buttons) <> (UserTrust.tupled, UserTrust.unapply)
}
