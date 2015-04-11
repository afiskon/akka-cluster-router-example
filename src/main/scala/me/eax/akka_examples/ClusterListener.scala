package me.eax.akka_examples

import akka.actor._
import akka.event._
import scala.concurrent.duration._
import akka.cluster.ClusterEvent._
import akka.cluster._

class ClusterListener extends Actor with ActorLogging {
  val minClusterSize = 2 // TODO: read from config!
  val cluster = Cluster(context.system)
  var timerCancellable: Option[Cancellable] = None

  case class CheckClusterSize(msg: String)

  def checkClusterSize(msg: String) {
    val clusterSize = cluster.state.members.count { m =>
      m.status == MemberStatus.Up ||
        m.status == MemberStatus.Joining
    }
    log.info(s"[Listener] event: $msg, cluster size: $clusterSize " +
      s"(${cluster.state.members})")


    if(clusterSize < minClusterSize) {
      log.info(s"[Listener] cluster size is less than $minClusterSize" +
        ", shutting down!")
      context.system.shutdown()
    }
  }

  def scheduleClusterSizeCheck(msg: String) {
    timerCancellable.foreach(_.cancel())
    timerCancellable = Some(
      context.system.scheduler.scheduleOnce(
        1.second, self, CheckClusterSize(msg)
      )
    )
  }

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart() {
    log.info(s"[Listener] started!")
    cluster.subscribe(self, InitialStateAsEvents, classOf[MemberEvent],
      classOf[UnreachableMember])
  }

  override def postStop() {
    log.info("[Listener] stopped!")
    cluster.unsubscribe(self)
  }

  def receive = LoggingReceive {
    case msg: MemberEvent =>
      scheduleClusterSizeCheck(msg.toString)

    case msg: UnreachableMember =>
      scheduleClusterSizeCheck(msg.toString)

    case r: CheckClusterSize =>
      checkClusterSize(r.msg)
  }
}
