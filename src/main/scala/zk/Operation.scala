/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk
import org.apache.zookeeper.Op

/**
  * Operation
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 18:58
  */
sealed trait Operation {
  def path:           String
  private[zk] def op: Op
}

case class CreateOperation(path: String, data: Array[Byte], acl: Seq[ACL], disposition: Disposition) extends Operation {
  private[zk] val op: Op = disposition match {
    case TTL(ttl) =>
      Op.create(path, data, ACL.toZACL(acl), disposition.mode.toFlag, ttl.toMillis)
    case _ =>
      Op.create(path, data, ACL.toZACL(acl), disposition.mode.toFlag)
  }
}

case class DeleteOperation(path: String, version: Option[Int]) extends Operation {
  private[zk] val op: Op = Op.delete(path, version.getOrElse(-1))
}

case class CheckOperation(path: String, version: Option[Int]) extends Operation {
  private[zk] val op: Op = Op.check(path, version.getOrElse(-1))
}

case class SetOperation(path: String, data: Array[Byte], version: Option[Int]) extends Operation {
  private[zk] val op: Op = Op.setData(path, data, version.getOrElse(-1))
}

sealed trait Result {
  def op: Operation
}

case class CreateResult(op: CreateOperation, path: String) extends Result
case class DeleteResult(op: DeleteOperation) extends Result
case class SetResult(op: SetOperation, status: Status) extends Result
case class CheckResult(op: CheckOperation) extends Result

sealed trait Problem {
  def op:    Operation
  def error: Option[KeeperException]
}

case class CreateProblem(op: CreateOperation, error: Option[KeeperException]) extends Problem
case class DeleteProblem(op: DeleteOperation, error: Option[KeeperException]) extends Problem
case class SetProblem(op: SetOperation, error: Option[KeeperException]) extends Problem
case class CheckProblem(op: CheckOperation, error: Option[KeeperException]) extends Problem
