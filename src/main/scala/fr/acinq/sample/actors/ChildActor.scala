package fr.acinq.sample.actors

import akka.actor.{Actor, ActorLogging}

class ChildActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case sth => log.info(s"received $sth")
  }

}