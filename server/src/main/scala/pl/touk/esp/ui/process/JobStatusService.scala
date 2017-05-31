package pl.touk.esp.ui.process

import akka.actor.ActorRef
import pl.touk.esp.ui.process.deployment.CheckStatus
import pl.touk.esp.ui.process.displayedgraph.ProcessStatus
import pl.touk.esp.ui.security.LoggedUser
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future

class JobStatusService(managerActor: ActorRef) {
  import scala.concurrent.duration._

  def retrieveJobStatus(processId: String)(implicit user: LoggedUser): Future[Option[ProcessStatus]] = {
    implicit val timeout = Timeout(1 minute)
    (managerActor ? CheckStatus(processId, user)).mapTo[Option[ProcessStatus]]
  }
}
