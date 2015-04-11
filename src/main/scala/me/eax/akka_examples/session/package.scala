package me.eax.akka_examples

import akka.actor._
import akka.pattern.ask
import akka.event.LoggingReceive
import akka.routing.FromConfig
import scala.concurrent.duration._
import me.eax.akka_examples.commons._

package object session {

  case class SessionInfo(username: String, lastActivity: Long)

  object SessionManager {
    val name = "sessionManager"

    def props() = Props[SessionManager]
  }

  class SessionManager extends Actor with ActorLogging {

    val managerRouter = context.actorOf(Props.empty.withRouter(FromConfig), "router")

    override def receive = LoggingReceive {
      // сообщение от самого себя или другого менеджера
      case r@RoutedMsg(sid: Long, msg: Any) =>
        val actorName = s"session-$sid"
        context.child(actorName) getOrElse {
          context.actorOf(SessionActor.props(sid), actorName)
        } forward msg

      // сообщение с текущей ноды
      case msg: RoutedMsgWithId =>
        managerRouter forward RoutedMsg(msg.id, msg)
    }
  }

  object SessionActor {

    def props(sid: Long) = Props(new SessionActor(sid))

    case class GetSession(id: Long) extends RoutedMsgWithId
    case class UpdateSession(id: Long, sessionInfo: SessionInfo) extends RoutedMsgWithId

    case class AskExt(mngRef: ActorRef) {
      def getSession(sid: Long) = (mngRef ? GetSession(sid)).mapTo[Option[SessionInfo]]
      def updateSession(sid: Long, sessionInfo: SessionInfo) = (mngRef ? UpdateSession(sid, sessionInfo)).mapTo[Unit]
    }
  }

  class SessionActor(sid: Long) extends Actor with ActorLogging {
    import me.eax.akka_examples.session.SessionActor._

    private var optCurrentSession: Option[SessionInfo] = None

    private val actorLifetime = 1.minute // TODO: read from config

    override def preStart() {
      println(s"SessionActor($sid): started!")
      context.system.scheduler.scheduleOnce(actorLifetime, self, PoisonPill)
    }

    override def postStop() {
      println(s"SessionActor($sid): stopped!")
    }

    override def receive = LoggingReceive { case req =>
      println(s"SessionActor($sid): Request received: $req from ${sender().path.address }")

      req match {
        case r: GetSession =>
          sender ! optCurrentSession
        case r: UpdateSession =>
          optCurrentSession = Some(r.sessionInfo)
          sender ! ()
      }
    }
  }

}
