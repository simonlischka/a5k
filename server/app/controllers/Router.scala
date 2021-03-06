package controllers
import java.util.Date
import javax.management.modelmbean.RequiredModelMBean
import akka.actor.{Actor, Props, ActorRef}
import controllers.Router.{RememberForReplay, ForwardToAllSessions, TestListReceivers}
import de.bht.lischka.adminTool5k.InternalMessages.{RequestReplay, SendMessage, RegisterListener}
import de.bht.lischka.adminTool5k.ModelX._
import de.bht.lischka.adminTool5k.Session
import de.bht.lischka.adminTool5k.Session.Replay

object Router {
  def props = Props(new Router())
  case object TestListReceivers
  case class ForwardToAllSessions(news: Any, ignoredReceiver: ActorRef)
  case class RememberForReplay(msg: Any)
}

class Router extends Actor {
  var registeredReceivers: List[ActorRef] = List()
  var replay: List[SendMessage] = List()
  var pidParser = context.actorOf(PsExecutor.props(self))

  def forwardMsgToAllSessions(message: Any, ignoredReceiver: ActorRef) = {
    registeredReceivers.filter(_ != ignoredReceiver).foreach(_ ! message)
  }

  override def receive: Actor.Receive = {
    case u: LoginUser => sender ! Replay(replay)

    case RegisterListener(newReceiver: ActorRef) =>
      registeredReceivers = newReceiver :: registeredReceivers

    case ForwardToAllSessions(msg, ignoredReceiver) => forwardMsgToAllSessions(msg, ignoredReceiver)

    case RememberForReplay(msg: SendMessage) =>
      replay = msg :: replay

    case wsMessage: WSMessage =>
      wsMessage match {

        case CommandResult(cmdResponse)  =>
          val msg = SendMessage(CommandResult(cmdResponse))
          //@TODO: Remove this way of satisfying function parameters
          self ! ForwardToAllSessions(msg, self)
          self ! RememberForReplay(msg)

        case ExecuteCommand(shellCommand) =>
          val msg = SendMessage(ExecuteCommand(shellCommand))
          self ! ForwardToAllSessions(msg, sender())
          self ! RememberForReplay(msg)
          val commandExecutor = context.actorOf(CommandExecutor.props(resultHandler = self))
          commandExecutor ! ExecuteCommand(shellCommand)

        case systemStatsUpdate: SystemStatsUpdateRaw =>
          forwardMsgToAllSessions(SendMessage(systemStatsUpdate), self)

        case anything => println(s"Triggered default case in Router, got ${anything}")
      }
  }
}