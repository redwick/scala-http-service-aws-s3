package server.cloud

import server.http.AppMessage.{AppSender, CloudManagerMessage}
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object CloudManager extends CloudManagerControls with CloudManagerTables {
  def apply(): Behavior[(CloudManagerMessage, AppSender)] = Behaviors.setup(context => {

    Behaviors.receiveMessagePartial(
        cloudFilesControl
        .orElse(noneControl)
    )

  })
}
