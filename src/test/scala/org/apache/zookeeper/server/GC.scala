/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package org.apache.zookeeper.server

import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.duration.Duration

/**
  * GC
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/22 19:45
  */
class GC(server: ZooKeeperServer) {
  private val fakeElapsed = new AtomicLong(0)

  server.setupRequestProcessors()

  private val containerManager = new ContainerManager(server.getZKDatabase, server.firstProcessor, 1, 100) {
    override def getElapsed(node: DataNode): Long = fakeElapsed.get()
  }

  /**
   * 默认zookeeper server 过去的时间
   * @param time server 流逝时间
   */
  def clean(time: Duration) = {
    fakeElapsed.set(time.toMillis)
    containerManager.checkContainers()
  }
}
