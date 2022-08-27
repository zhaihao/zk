/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk

import com.typesafe.scalalogging.StrictLogging
import org.apache.zookeeper.server.{GC, ServerCnxnFactory, ZooKeeperServer}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterEach}
import test.BaseSpecLike

import java.net.InetSocketAddress

/**
  * Spec
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 22:23
  */
trait Spec extends BaseSpecLike with BeforeAndAfterAll with BeforeAndAfterEach with StrictLogging {
  var server:     ZooKeeperServer       = _
  private var zk: Zookeeper             = _
  var sync:       SynchronousZookeeper  = _
  var async:      AsynchronousZookeeper = _
  var gc:         GC                    = _

  val root = "/test"

  /**
   * In versions 3.5+, a ZooKeeper server can use Netty instead of NIO (default option) by setting the environment
   * variable zookeeper.serverCnxnFactory to org.apache.zookeeper.server.NettyServerCnxnFactory; for the client,
   * set zookeeper.clientCnxnSocket to org.apache.zookeeper.ClientCnxnSocketNetty.
   */
  override def beforeAll(): Unit = {
    sys.props += "zookeeper.extendedTypesEnabled" -> "true"
    sys.props += "zookeeper.serverCnxnFactory"    -> "org.apache.zookeeper.server.NettyServerCnxnFactory"
    sys.props += "zookeeper.maxCnxns"             -> "20"
    val dir = os.temp.dir().toIO
    server = new ZooKeeperServer(dir, dir, ZooKeeperServer.DEFAULT_TICK_TIME)
    val conn = ServerCnxnFactory.createFactory(new InetSocketAddress(0), 5)
    gc = new GC(server)
    conn.startup(server)
    logger.info(s"server is running, ${server.getServerCnxnFactory.getLocalAddress}")

    reconnect
    util.retry(50, 10) {
      require(zk.state.isConnected, "client doesn't connect.")
    }
    logger.info("client is connected.")
  }

  def reconnect = {
    if (zk != null) zk.close
    val config = Configuration(s"localhost:${server.getClientPort}")
    zk = Zookeeper(config)
    sync = zk.sync
    async = zk.async
  }

  override def afterAll(): Unit = {
    zk.close
    logger.info("client close connection.")
    server.shutdown()
    logger.info("server is shutdown.")
  }

  override protected def beforeEach(): Unit = {
    sync.create(root, "TestRoot".getBytes(), ACL.AnyoneAll, Persistent)
  }

  override protected def afterEach(): Unit = {
    sync.deleteAll(root)
  }
}
