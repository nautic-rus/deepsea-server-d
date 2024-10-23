package domain

import java.net.InetSocketAddress
import java.net.Socket
import java.sql.{DriverManager, Connection}
import org.slf4j.LoggerFactory
import slick.jdbc.JdbcBackend
import slick.jdbc.JdbcBackend.Database

object DBManager {

  private val logger = LoggerFactory.getLogger("database")
  // Конфигурация для Oracle

  lazy val PostgresSQL: JdbcBackend.Database = Database.forURL("jdbc:postgresql://192.168.1.26:5432/deepsea", "deepsea", driver = "org.postgresql.Driver")
  lazy val ForanDB: JdbcBackend.Database = Database.forURL("jdbc:oracle:thin:@192.168.1.12:1521:ORA3DB", "CN002", "Whatab0utus")

  def checkPort(host: String, port: Int): Boolean = {
    try {
      val socket = new Socket()
      val address = new InetSocketAddress(host, port)
      socket.connect(address, 1000)
      true
    } catch {
      case _: Throwable => false
    }
  }

  def checkPostgres(): Boolean = {
    try {
      if (!checkPort("192.168.1.26", 5432)) {
        return false
      }
      val sAddr = new InetSocketAddress("192.168.1.26", 5432)
      println(sAddr)
      val socket = new Socket()
      println(socket)
      socket.connect(sAddr, 1000)
      true
    } catch {
      case _: Throwable => false
    }
  }
  def checkOracle(): Boolean = {
    try {
      if (!checkPort("192.168.1.12", 1521)) {
        return false
      }
      val sAddr = new InetSocketAddress("192.168.1.12", 1521)
      println(sAddr)
      val socket = new Socket()
      println(socket)
      socket.connect(sAddr, 1000)
      true
    } catch {
      case _: Throwable => false
    }
  }

  def start(): Boolean = {
    try {
      if (checkPostgres && checkOracle) {
        println("checked")
        PostgresSQL
        ForanDB
        true
      } else {
        false
      }
    } catch {
      case _: Throwable =>
        logger.error("Error starting Database")
        false
    }
  }
}
