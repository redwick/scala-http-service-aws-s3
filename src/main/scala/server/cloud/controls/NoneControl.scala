package server.cloud.controls

import server.app.Envs.ErrorCodes
import server.http.AppMessage.{AppSender, CloudManagerMessage}
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory

trait NoneControl {

  private val logger = LoggerFactory.getLogger(this.toString)

  def noneControl: PartialFunction[(CloudManagerMessage, AppSender), Behavior[(CloudManagerMessage, AppSender)]] = {
    case _ =>
      logger.error(ErrorCodes.UnreachableCode)
      Behaviors.same
  }
}
