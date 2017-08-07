package io.ahamdy.taskforce.testing

import javax.sql.DataSource

import io.ahamdy.taskforce.db.Db
import org.specs2.main.{Arguments, ArgumentsShortcuts}
import org.specs2.specification.core.{SpecStructure, SpecificationStructure}
import org.specs2.specification.dsl.mutable.ArgumentsDsl
import org.specs2.specification.{AfterAll, AfterEach}

trait DatabaseSpec extends AfterAll with AfterEach with SpecificationStructure with ArgumentsDsl {
  this: ArgumentsShortcuts =>

  override def map(s: SpecStructure): SpecStructure =
    super.map(s.copy(arguments = s.arguments.overrideWith(Arguments("sequential"))))

  val databaseName = "psa"
  val username = "postgres"

  private val service = new DatabaseTestService(
    name = "PSA db for test",
    port = 0,
    afterStartScripts = List(s"""CREATE DATABASE \"$databaseName\" WITH OWNER = \"$username\"""")
  )

  lazy val dataSource: DataSource = {
    service.start()
    val dataSource = service.pg.getDatabase(username, databaseName)
    Db.migrateSchema(dataSource)
    dataSource
  }

  lazy val db: Db = {
    dataSource
    val url = service.pg.getJdbcUrl(username, databaseName)
    Db.unsafeCreate(url, username, "")
  }

  def tablesToTruncate: List[String]

  def unsafeTruncateTables(): Unit =
    tablesToTruncate.foreach(table => runSql(s"TRUNCATE TABLE $table"))

  protected def runSql(script: String): Unit = {
    val connection = dataSource.getConnection
    try {
      val statement = connection.createStatement
      statement.execute(script)
      statement.close()
    } finally connection.close()
  }

  override def afterAll: Unit = service.stop()

  override def after: Unit = unsafeTruncateTables()
}
