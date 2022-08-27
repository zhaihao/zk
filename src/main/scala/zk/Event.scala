/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.Watcher.Event.{EventType, KeeperState}
import org.apache.zookeeper.Watcher.Event.EventType.{None => NodeNone}
import scala.language._
/**
  * Event
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 18:55
  */
sealed trait Event
trait StateEvent extends Event
case object Disconnected extends StateEvent
case object Connected extends StateEvent
case object AuthenticationFailed extends StateEvent
case object ConnectedReadOnly extends StateEvent
case object Authenticated extends StateEvent
case object Expired extends StateEvent
case object Closed extends StateEvent

private[zk] object StateEvent {
  private val events = Map(
    KeeperState.Disconnected.getIntValue -> Disconnected,
    KeeperState.SyncConnected.getIntValue -> Connected,
    KeeperState.AuthFailed.getIntValue -> AuthenticationFailed,
    KeeperState.ConnectedReadOnly.getIntValue -> ConnectedReadOnly,
    KeeperState.SaslAuthenticated.getIntValue -> Authenticated,
    KeeperState.Expired.getIntValue -> Expired,
    KeeperState.Closed.getIntValue -> Closed
  )

  def apply(state: KeeperState): StateEvent = apply(state.getIntValue)

  def apply(state: Int): StateEvent = events.get(state) match {
    case Some(event) => event
    case _ => new Unrecognized(state)
  }

  private class Unrecognized(state: Int) extends StateEvent {
    override def toString: String = "Unrecognized[" + state + "]"
  }
}

trait NodeEvent extends Event {
  def path: String
}

case class Created(path: String) extends NodeEvent
case class Deleted(path: String) extends NodeEvent
case class DataChanged(path: String) extends NodeEvent
case class ChildrenChanged(path: String) extends NodeEvent
case class ChildWatchRemoved(path: String) extends NodeEvent
case class DataWatchRemoved(path: String) extends NodeEvent

private[zk] object NodeEvent {
  private val events = Map[Int, String => NodeEvent](
    EventType.NodeCreated.getIntValue -> { p => Created(p) },
    EventType.NodeDeleted.getIntValue -> { p => Deleted(p) },
    EventType.NodeDataChanged.getIntValue -> { p => DataChanged(p) },
    EventType.NodeChildrenChanged.getIntValue -> { p => ChildrenChanged(p) },
    EventType.ChildWatchRemoved.getIntValue -> { p => ChildWatchRemoved(p) },
    EventType.DataWatchRemoved.getIntValue -> { p => DataWatchRemoved(p) }
  )

  def apply(event: EventType, path: String): NodeEvent = apply(event.getIntValue, path)

  def apply(event: Int, path: String): NodeEvent = events.get(event) match {
    case Some(fn) => fn(path)
    case _ => new Unrecognized(event, path)
  }

  private class Unrecognized(event: Int, val path: String) extends NodeEvent {
    override def toString: String = "Unrecognized[" + event + "](" + path + ")"
  }
}

private[zk] object Event {
  def apply(event: WatchedEvent): Event = event.getType match {
    case NodeNone => StateEvent(event.getState)
    case e @ _ => NodeEvent(e, event.getPath)
  }
}
