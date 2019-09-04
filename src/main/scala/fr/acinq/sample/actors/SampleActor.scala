package fr.acinq.sample.actors

import akka.actor.{Actor, ActorLogging, PoisonPill, Props, Terminated}
import fr.acinq.sample.actors.SampleActor.{Ping, Pong}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SampleActor extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.system.dispatcher

  context.system.scheduler.schedule(2 seconds, 4 seconds, self, Ping)
  val child = context.system.actorOf(Props(new ChildActor))
  context.watch(child)

  override def receive: Receive = initialState(Set.empty)

  def initialState(data: Set[String]): Receive = {
    case 'kill => child ! PoisonPill
    case Terminated(actor) => log.info(s"Child actor killed successfully")
    case Pong => log.info("received Pong while in 'initialState' state")
    case Ping =>
      log.info("received Ping, transitioning to 'afterPing'")
      context.system.scheduler.scheduleOnce(scala.util.Random.nextInt(3) seconds, self, Pong)
      context.become(afterPing(data))
  }

  def afterPing(data: Set[String]): Receive = {
    case 'kill => child ! PoisonPill
    case Terminated(actor) => log.info(s"Child actor killed successfully")
    case Ping => log.info("received Ping while in 'afterPing' state")
    case Pong =>
      log.info("received Pong, transitioning to 'initialState")
      context.become(initialState(data))
  }

}

object SampleActor {

  case object Ping
  case object Pong

}