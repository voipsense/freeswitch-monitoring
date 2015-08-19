package gr.gnostix.freeswitch.actors

import akka.actor.{Actor, ActorLogging}
import gr.gnostix.freeswitch.actors.CallRouter.{GetFailedCallsByDate, GetTotalFailedCalls, GetFailedCalls}
import org.scalatra.atmosphere.AtmosphereClient
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created by rebel on 17/8/15.
 */


class FailedCallsActor extends Actor with ActorLogging {

  var failedCalls: List[CallEnd] = List()
  val Tick = "tick"

  def receive: Receive = {
    case x @ CallEnd(uuid, eventName, fromUser, toUser, readCodec, writeCodec, fromUserIP, callUUID,
    callerChannelCreatedTime, callerChannelAnsweredTime, callerChannelHangupTime, freeSWITCHHostname,
    freeSWITCHIPv4, hangupCause, billSec, rtpQualityPerc) =>
      log info "-------> add an extra failed call"
      failedCalls ::= x
      val fCall = FailedCall("FAILED_CALL", x.fromUser, x.toUser, x.callUUID, x.freeSWITCHIPv4)
      AtmosphereClient.broadcast("/fs-moni/live/events", ActorsJsonProtocol.failedCallToJson(fCall))

    case x @ GetFailedCalls =>
      log info "returning the failed calls " + failedCalls
      sender ! failedCalls

    case x @ GetTotalFailedCalls =>
      log info "returning the failed calls size " + failedCalls.size
      sender ! Map("failedCallsNum" -> failedCalls.size)

    case x @ GetFailedCallsByDate(fromDate, toDate) =>
      sender ! failedCalls.filter(a => a.callerChannelHangupTime.after(fromDate)
                                                && a.callerChannelHangupTime.before(toDate))

    case Tick =>
      failedCalls = getLastsHeartBeats

  }

  context.system.scheduler.schedule(0 milliseconds,
    1000 milliseconds,
    self,
    Tick)

  def getLastsHeartBeats = {
    failedCalls.take(100)
  }

}
