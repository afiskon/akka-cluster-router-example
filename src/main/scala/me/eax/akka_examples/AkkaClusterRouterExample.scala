package me.eax.akka_examples

import me.eax.akka_examples.profile.{ProfileManager, ProfileActor}
import me.eax.akka_examples.session._

import akka.actor._

import scala.concurrent._
import scala.concurrent.duration._
import scala.compat.Platform
import scala.util.Random

object AkkaClusterRouterExample extends App {
  val system = ActorSystem("system")
  system.actorOf(Props[ClusterListener], name = "clusterListener")

  val sessionManager = SessionActor.AskExt(system.actorOf(SessionManager.props(), SessionManager.name))
  val profileManager = ProfileActor.AskExt(system.actorOf(ProfileManager.props(), ProfileManager.name))

  val fTerminated = system.whenTerminated

  while(!fTerminated.isCompleted) {
    Thread.sleep(1000L)
    val id = Random.nextInt(10).toLong
    val fResult: Future[Any] = Random.nextInt(3) match {
      case 0 =>
        sessionManager.getSession(id)
      case 1 =>
        val sessionInfo = SessionInfo(s"user-with-session-$id", Platform.currentTime)
        sessionManager.updateSession(id, sessionInfo)
      case 2 =>
        profileManager.getProfile(id)
    }
    try {
      val response = Await.result(fResult, 5.seconds)
      println(s"Response: $response")
    } catch {
      case e: Exception =>
        println(s"ERROR: $e")
    }
  }
}
