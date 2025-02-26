package server.cloud.tables

import io.circe.generic.JsonCodec
import server.DBManager
import server.DBManager.PostgresSQL
import org.slf4j.LoggerFactory
import slick.collection.heterogeneous.HNil
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{TableQuery, Tag}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.io.Source

trait CloudFilesTable {
  private val logger = LoggerFactory.getLogger(this.toString)

  @JsonCodec case class CloudFile(id: Int,
                                   file_name: String, file_path: String, url_path: String, kind: String, file_size: Long,
                                   date_created: Long, user_created: String, date_changed: Long, user_changed: String, removed: Int)
  case class CloudFileTable(tag: Tag) extends Table[CloudFile](tag, "cloud_files") {
    val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val file_name = column[String]("file_name")
    val file_path = column[String]("file_path")
    val url_path = column[String]("url_path")
    val kind = column[String]("kind")
    val file_size = column[Long]("file_size")
    val date_created = column[Long]("date_created")
    val user_created = column[String]("user_created")
    val date_changed = column[Long]("date_changed")
    val user_changed = column[String]("user_changed")
    val removed = column[Int]("removed")

    def * = (
      id :: file_name :: file_path :: url_path :: kind :: file_size ::
        date_created :: user_created :: date_changed :: user_changed :: removed :: HNil
      ).mapTo[CloudFile]
  }
  val cloudFilesTable = TableQuery[CloudFileTable]

  @JsonCodec case class CloudFileGet(id: Int,
                                  file_name: String, file_path: String, url_path: String, kind: String,
                                  child_count: Int, file_size: Long,
                                  date_created: Long, user_created: String, date_changed: Long, user_changed: String, removed: Int)

  def getCloudFiles(path: String): List[CloudFileGet] = {
    try {
      val result = ListBuffer.empty[CloudFileGet]
      val connection = DBManager.GetPGConnection
      val q = Source.fromResource("sql/cloud_files.sql").mkString.replace("&path", path)
      val stmt = connection.createStatement()
      val rs = stmt.executeQuery(q)
      while (rs.next()) {
        val user_created = Option(rs.getString("user_created")).getOrElse("")
        val user_changed = Option(rs.getString("user_changed")).getOrElse("")
        result += CloudFileGet(
          Option(rs.getInt("id")).getOrElse(0),
          Option(rs.getString("file_name")).getOrElse(""),
          Option(rs.getString("file_path")).getOrElse(""),
          Option(rs.getString("url_path")).getOrElse(""),
          Option(rs.getString("kind")).getOrElse(""),
          0,
          Option(rs.getLong("file_size")).getOrElse(0),
          Option(rs.getLong("date_created")).getOrElse(0),
          user_created,
          Option(rs.getLong("date_changed")).getOrElse(0),
          user_changed,
          Option(rs.getInt("removed")).getOrElse(0)
        )
      }
      rs.close()
      stmt.close()
      connection.close()
      result.toList
    }
    catch {
      case e: Throwable =>
        logger.error(e.toString)
        List.empty[CloudFileGet]
    }
  }
  def getCloudFilesWithCount(path: String): List[CloudFileGet] = {
    try {
      val result = ListBuffer.empty[CloudFileGet]
      val connection = DBManager.GetPGConnection
      val q = Source.fromResource("sql/cloud_files_with_count.sql").mkString.replace("&path", path)
      val stmt = connection.createStatement()
      val rs = stmt.executeQuery(q)
      while (rs.next()) {
        val user_created = Option(rs.getString("user_created")).getOrElse("")
        val user_changed = Option(rs.getString("user_changed")).getOrElse("")
        result += CloudFileGet(
          Option(rs.getInt("id")).getOrElse(0),
          Option(rs.getString("file_name")).getOrElse(""),
          Option(rs.getString("file_path")).getOrElse(""),
          Option(rs.getString("url_path")).getOrElse(""),
          Option(rs.getString("kind")).getOrElse(""),
          Option(rs.getInt("child_count")).getOrElse(0),
          Option(rs.getLong("file_size")).getOrElse(0),
          Option(rs.getLong("date_created")).getOrElse(0),
          user_created,
          Option(rs.getLong("date_changed")).getOrElse(0),
          user_changed,
          Option(rs.getInt("removed")).getOrElse(0)
        )
      }
      rs.close()
      stmt.close()
      connection.close()
      result.toList
    }
    catch {
      case e: Throwable =>
        logger.error(e.toString)
        List.empty[CloudFileGet]
    }
  }
  def getCloudFilesAll(path: String = ""): List[CloudFileGet] = {
    try {
      val result = ListBuffer.empty[CloudFileGet]
      val connection = DBManager.GetPGConnection
      val q = Source.fromResource("sql/cloud_files_all.sql").mkString + (if (path != "") s" and file_path like '$path%'" else "")
      val stmt = connection.createStatement()
      val rs = stmt.executeQuery(q)
      while (rs.next()) {
        val user_created = Option(rs.getString("user_created")).getOrElse("")
        val user_changed = Option(rs.getString("user_changed")).getOrElse("")
        result += CloudFileGet(
          Option(rs.getInt("id")).getOrElse(0),
          Option(rs.getString("file_name")).getOrElse(""),
          Option(rs.getString("file_path")).getOrElse(""),
          Option(rs.getString("url_path")).getOrElse(""),
          Option(rs.getString("kind")).getOrElse(""),
          0,
          Option(rs.getLong("file_size")).getOrElse(0),
          Option(rs.getLong("date_created")).getOrElse(0),
          user_created,
          Option(rs.getLong("date_changed")).getOrElse(0),
          user_changed,
          Option(rs.getInt("removed")).getOrElse(0)
        )
      }
      rs.close()
      stmt.close()
      connection.close()
      result.toList
    }
    catch {
      case e: Throwable =>
        logger.error(e.toString)
        List.empty[CloudFileGet]
    }
  }
  def postCloudFile(value: CloudFile): Future[Int] = {
    PostgresSQL.run((cloudFilesTable returning cloudFilesTable.map(_.id)) += value)
  }
  def postCloudFileCheck(filePath: String, fileName: String): Future[Boolean] = {
    PostgresSQL.run(cloudFilesTable.filter(x => x.file_path === filePath && x.file_name === fileName && x.removed === 0).exists.result)
  }
  def putCloudFiles(id: Int, file_name: String, file_path: String, url_path: String, u: String, d: Long): Future[Int] = {
    PostgresSQL.run(cloudFilesTable.filter(_.id === id).map(x => (x.file_name, x.file_path, x.url_path, x.user_changed, x.date_changed))
      .update((file_name, file_path, url_path, u, d)))
  }
  def putCloudFilesCheck(id: Int, d: Long): Future[Boolean] = {
    PostgresSQL.run(cloudFilesTable.filter(_.id === id).filter(_.date_changed === d).exists.result)
  }
  def setFileSize(id: Int, file_size: Long): Future[Int] = {
    PostgresSQL.run(cloudFilesTable.filter(_.id === id).map(x => (x.file_size))
      .update(file_size))
  }
  def deleteCloudFile(id: Int, u: String, d: Long): Unit = {
    getCloudFile(id) match {
      case Some(file) =>
        if (file.kind == "DIRECTORY"){
          val connection = DBManager.GetPGConnection
          val remove_path = file.file_path + file.file_name + "/"
          val q = s"update cloud_files set removed = 1, user_changed = '$u', date_changed = $d where file_path like '$remove_path%'"
          val stmt = connection.createStatement()
          stmt.execute(q)
          stmt.close()
          connection.close()
        }
        try{
          val connection = DBManager.GetPGConnection
          val q = s"update cloud_files set removed = 1, user_changed = '$u', date_changed = $d where id = $id"
          val stmt = connection.createStatement()
          stmt.execute(q)
          stmt.close()
          connection.close()
        }
        catch {
          case e: Throwable =>
            logger.error(e.toString)
        }

      case _ => None
    }
  }
  def getCloudFile(id: Int): Option[CloudFile] = {
    try {
      val result = ListBuffer.empty[CloudFile]
      val connection = DBManager.GetPGConnection
      val q = s"select * from cloud_files where id = $id"
      val stmt = connection.createStatement()
      val rs = stmt.executeQuery(q)
      while (rs.next()) {
        result += CloudFile(
          rs.getInt("id"),
          rs.getString("file_name"),
          rs.getString("file_path"),
          rs.getString("url_path"),
          rs.getString("kind"),
          rs.getLong("file_size"),
          rs.getLong("date_created"),
          rs.getString("user_created"),
          rs.getLong("date_changed"),
          rs.getString("user_changed"),
          rs.getInt("removed"),
        )
      }
      rs.close()
      stmt.close()
      connection.close()
      result.headOption
    }
    catch {
      case e: Throwable =>
        logger.error(e.toString)
        Option.empty[CloudFile]
    }
  }

}
