package fr.acinq.sample

import java.sql._

import com.typesafe.scalalogging.LazyLogging
import fr.acinq.sample.Utils.Person

class Database(connection: Connection) extends LazyLogging {

  def createDb() = {
    logger.info(s"creating and populating database")
    using(connection.createStatement) { stmt =>
      stmt.executeUpdate("drop table if exists person")
      stmt.executeUpdate("create table person (id integer, firstname string, lastname string)")
      stmt.executeUpdate("insert into person values(1, 'Satoshi', 'Nakamoto')")
      stmt.executeUpdate("insert into person values(2, 'Hal', 'Finney')")
      stmt.executeUpdate("insert into person values(3, 'Adam', 'Back')")
      stmt.executeUpdate("insert into person values(4, 'Wei', 'Dai')")
    }
  }

  def using[A, B <: Statement](closeable: B)(f: B => A): A = {
    try {
      f(closeable)
    } finally {
      Option(closeable).foreach(_.close())
    }
  }

  def byName(name: String): Option[Person] = {
    logger.info(s"query=$name")
    using(connection.prepareStatement("select * from person where firstname=?")) { stmt =>
      stmt.setString(1, name)
      val rs = stmt.executeQuery()
      if (rs.next()) {
        Some(Person(
          firstName = rs.getString("firstname"),
          lastName = rs.getString("lastname"),
        ))
      } else {
        None
      }

    }
  }

}
