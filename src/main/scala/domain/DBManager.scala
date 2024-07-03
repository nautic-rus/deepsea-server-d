package domain

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend
import slick.jdbc.JdbcBackend.Database

import java.net.{InetAddress, InetSocketAddress, Socket}

object DBManager {

  private val logger = LoggerFactory.getLogger("database")
//  private val config: Config = ConfigFactory.load()

//  lazy val PostgresSQL: JdbcBackend.Database = Database.forConfig("deepsea")
  lazy val PostgresSQL: JdbcBackend.Database = Database.forURL("jdbc:postgresql://192.168.1.26:5432/deepsea", "deepsea", driver = "org.postgresql.Driver")


  def start(): Boolean = {
    try {
      if (check()) {
        PostgresSQL
        true
      }
      else {
        false
      }
    }
    catch {
      case _: Throwable =>
        logger.error("Error starting Database")
        false
    }
  }

  private def check(): Boolean = {
    checkPostgres
  }

  private def checkPostgres: Boolean = {
    try {
      println("Try")
//      val sAddr = new InetSocketAddress(InetAddress.getByName("192.168.1.26"), 5432)
     val sAddr = new InetSocketAddress("192.168.1.26", 5432)
      println(sAddr)
      val socket = new Socket()
      println(socket)
      socket.connect(sAddr, 1000)
      true
    }
    catch {
      case _: Throwable =>
        logger.error("Could not establish connection to PostgresSQL")
        false
    }
  }

}
