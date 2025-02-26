package server.cloud

import server.DBManager.PostgresSQL
import server.cloud.tables.CloudFilesTable
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

trait CloudManagerTables extends CloudFilesTable {

  PostgresSQL.run(DBIO.seq(
    cloudFilesTable.schema.createIfNotExists,
  ))
}
