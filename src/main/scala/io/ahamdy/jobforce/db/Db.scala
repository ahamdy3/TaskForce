package io.ahamdy.jobforce.db

import javax.sql.DataSource
import doobie.hikari.hikaritransactor.HikariTransactor
import doobie.imports._
import fs2.Task
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource

/** Storage of data in a relational database */
trait Db {
  def runCommand[A](command: ConnectionIO[A]): Task[A]
}

class DbImpl(transactor: Transactor[IOLite]) extends Db {
  def runCommand[A](command: ConnectionIO[A]): Task[A] = Task.delay(command.transact(transactor).unsafePerformIO)
}

object Db {

  def unsafeCreate(config: DbConfig): Db = unsafeCreate(
    s"jdbc:postgresql://${config.hostname}:${config.port}/${config.databaseName}", config.username, config.password)

  def unsafeCreate(jdbcUrl: String, username: String, password: String): Db = {
    val transactor = HikariTransactor[IOLite]("org.postgresql.Driver", jdbcUrl, username, password).unsafePerformIO
    new DbImpl(transactor)
  }

  def createDataSource(config: DbConfig): DataSource = {
    val dataSource = new PGSimpleDataSource
    dataSource.setServerName(config.hostname)
    dataSource.setPortNumber(config.port)
    dataSource.setDatabaseName(config.databaseName)
    dataSource.setUser(config.username)
    dataSource.setPassword(config.password)
    dataSource.setApplicationName("PSA")
    dataSource
  }

  def migrateSchema(dataSource: DataSource): Unit =
    new Flyway() {
      setDataSource(dataSource)
    }.migrate()
}

case class DbConfig(
  hostname: String,
  port: Int,
  databaseName: String,
  username: String,
  password: String
)
