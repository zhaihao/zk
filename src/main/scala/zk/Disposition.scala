/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.CreateMode._
import scala.concurrent.duration.Duration

/**
  * Disposition
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 18:52
  */

sealed trait Disposition {
  private[zk] val mode: CreateMode
}
sealed trait TTL {
  val ttl: Duration
}

object TTL {
  def unapply(ttl: TTL): Option[Duration] =
    if (ttl == null) None else Some(ttl.ttl)
}

object Persistent extends Disposition {
  override def toString: String = "Persistent"
  private[zk] val mode = PERSISTENT
}

class PersistentTTL private (val ttl: Duration) extends Disposition with TTL {
  override def toString: String = s"PersistentTTL($ttl)"
  private[zk] val mode = PERSISTENT_WITH_TTL
}

object PersistentTTL {
  def apply(ttl: Duration) = new PersistentTTL(ttl)
}

object PersistentSequential extends Disposition {
  override def toString: String = "PersistentSequential"
  private[zk] val mode = PERSISTENT_SEQUENTIAL
}

class PersistentSequentialTTL private (val ttl: Duration) extends Disposition with TTL {
  override def toString: String = s"PersistentSequentialTTL($ttl)"
  private[zk] val mode = PERSISTENT_SEQUENTIAL_WITH_TTL
}

object PersistentSequentialTTL {
  def apply(ttl: Duration) = new PersistentSequentialTTL(ttl)
}

object Ephemeral extends Disposition {
  override def toString: String = "Ephemeral"
  private[zk] val mode = EPHEMERAL
}

object EphemeralSequential extends Disposition {
  override def toString: String = "EphemeralSequential"
  private[zk] val mode = EPHEMERAL_SEQUENTIAL
}

object Container extends Disposition {
  override def toString: String = "Container"
  private[zk] val mode = CONTAINER
}
