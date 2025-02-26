package server.app

import org.slf4j.LoggerFactory

import scala.util.Properties

object Envs extends Codes {

  private val logger = LoggerFactory.getLogger("props")

  val aws_accessKey: String = Properties.envOrElse("aws_accessKey", {
    logger.error(EnvCodes.AWSAccessKeyNotSet)
    ""
  })
  val aws_secretKey: String = Properties.envOrElse("aws_secretKey", {
    logger.error(EnvCodes.AWSSecretKeyNotSet)
    ""
  })
  val pg_host: String = Properties.envOrElse("pg_host", {
    logger.error(EnvCodes.PGHostNotSet)
    ""
  })
  val pg_pass: String = Properties.envOrElse("pg_pass", {
    logger.error(EnvCodes.PGPassNotSet)
    ""
  })


  def check(): Boolean ={
    !List(aws_accessKey, aws_secretKey, pg_host, pg_pass).contains("")
  }

}
