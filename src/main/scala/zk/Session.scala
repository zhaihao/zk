/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk
import org.apache.zookeeper.ZooKeeper
import scala.concurrent.duration._
import scala.language._

/**
  * Session
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 19:29
  */
trait Session {
  def credential: Credential
  def timeout:    Duration
}

object Session {
  def unapply(session: Session): Option[(Credential, Duration)] =
    if (session == null) None else Some(session.credential, session.timeout)

  private[zk] def apply(zk: ZooKeeper): Session = new Session {
    val credential: Credential = Credential(zk)
    val timeout:    Duration   = zk.getSessionTimeout.millis
    override def toString: String = s"Session(credential=$credential, timeout=$timeout)"
  }
}
