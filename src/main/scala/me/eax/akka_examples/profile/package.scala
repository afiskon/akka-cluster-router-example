package me.eax.akka_examples

import akka.actor._
import akka.pattern.ask
import akka.event.LoggingReceive
import akka.routing.FromConfig
import scala.concurrent.duration._
import me.eax.akka_examples.commons._

package object profile {

  case class ProfileInfo(uid: Long, username: String, email: String)

  object ProfileManager {
    val name = "profileManager"

    def props() = Props[ProfileManager]
  }

  class ProfileManager extends Actor with ActorLogging {

    val managerRouter = context.actorOf(Props.empty.withRouter(FromConfig), "router")

    override def receive = LoggingReceive {
      // сообщение от самого себя или другого менеджера
      case r@RoutedMsg(uid: Long, msg: Any) =>
        val actorName = s"profile-$uid"
        context.child(actorName) getOrElse {
          context.actorOf(ProfileActor.props(uid), actorName)
        } forward msg

      // сообщение с текущей ноды
      case msg: RoutedMsgWithId =>
        managerRouter forward RoutedMsg(msg.id, msg)
    }
  }

  object ProfileActor {

    def props(uid: Long) = Props(new ProfileActor(uid))

    case class GetProfile(id: Long) extends RoutedMsgWithId

    case class AskExt(mngRef: ActorRef) {
      def getProfile(uid: Long) = (mngRef ? GetProfile(uid)).mapTo[ProfileInfo]
    }
  }

  class ProfileActor(uid: Long) extends Actor with ActorLogging {
    import me.eax.akka_examples.profile.ProfileActor._

    private val actorLifetime = 1.minute // TODO: read from config

    override def preStart() {
      println(s"ProfileActor($uid): started!")
      context.system.scheduler.scheduleOnce(actorLifetime, self, PoisonPill)
    }

    override def postStop() {
      println(s"ProfileActor($uid): stopped!")
    }

    override def receive = LoggingReceive { case req =>
      println(s"ProfileActor($uid): Request received: $req from ${sender().path.address }")

      req match {
        case r: GetProfile =>
          sender ! ProfileInfo(uid, s"user$uid", s"user$uid@gmail.com")
      }
    }
  }
}
