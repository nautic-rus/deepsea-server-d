import domain.deepsea.{DeepseaManager, ForanManager}
import domain.{DBManager, HttpManager}
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.scaladsl.{Behaviors, Routers}
import org.apache.pekko.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}
import org.slf4j.LoggerFactory
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {

  private val logger = LoggerFactory.getLogger("master")

  def main(args: Array[String]): Unit = {
    if (DBManager.start()){
      try {
        Await.result(ActorSystem(Main(), "Main").whenTerminated, Duration.Inf)
      } catch {
        case e: Throwable =>
          logger.error(e.toString)
          main(Array.empty[String])
      }
    }
  }
  def apply(): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val deepsea = context.spawn(Routers.pool(poolSize = 3) {
        Behaviors.supervise(DeepseaManager()).onFailure[Exception](SupervisorStrategy.restart)
      }, "deepsea")
      val foran = context.spawn(Routers.pool(poolSize = 3) {
        Behaviors.supervise(ForanManager()).onFailure[Exception](SupervisorStrategy.restart)
      }, "foran")
      HttpManager(context.system, deepsea, foran)
      Behaviors.empty
    }
  }
}
