/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */
import java.net.InetSocketAddress
import org.apache.zookeeper.KeeperException
import scala.language._

/**
  * package
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 18:59
  */
package object zk {
  type KeeperException                  = org.apache.zookeeper.KeeperException
  type APIErrorException                = KeeperException.APIErrorException
  type AuthFailedException              = KeeperException.AuthFailedException
  type BadArgumentsException            = KeeperException.BadArgumentsException
  type BadVersionException              = KeeperException.BadVersionException
  type ConnectionLossException          = KeeperException.ConnectionLossException
  type DataInconsistencyException       = KeeperException.DataInconsistencyException
  type EphemeralOnLocalSessionException = KeeperException.EphemeralOnLocalSessionException
  type InvalidACLException              = KeeperException.InvalidACLException
  type InvalidCallbackException         = KeeperException.InvalidCallbackException
  type MarshallingErrorException        = KeeperException.MarshallingErrorException
  type NewConfigNoQuorum                = KeeperException.NewConfigNoQuorum
  type NoAuthException                  = KeeperException.NoAuthException
  type NoChildrenForEphemeralsException = KeeperException.NoChildrenForEphemeralsException
  type NodeExistsException              = KeeperException.NodeExistsException
  type NoNodeException                  = KeeperException.NoNodeException
  type NotEmptyException                = KeeperException.NotEmptyException
  type NotReadOnlyException             = KeeperException.NotReadOnlyException
  type NoWatcherException               = KeeperException.NoWatcherException
  type OperationTimeoutException        = KeeperException.OperationTimeoutException
  type ReconfigDisabledException        = KeeperException.ReconfigDisabledException
  type ReconfigInProgress               = KeeperException.ReconfigInProgress
  type RequestTimeoutException          = KeeperException.RequestTimeoutException
  type RuntimeInconsistencyException    = KeeperException.RuntimeInconsistencyException
  type SessionExpiredException          = KeeperException.SessionExpiredException
  type SessionMovedException            = KeeperException.SessionMovedException
  type SystemErrorException             = KeeperException.SystemErrorException
  type UnimplementedException           = KeeperException.UnimplementedException
  type UnknownSessionException          = KeeperException.UnknownSessionException

  implicit def tupleToInetSocketAddress(address: (String, Int)): InetSocketAddress =
    new InetSocketAddress(address._1, address._2)
}
