/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk

import java.net.InetSocketAddress
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language._

/**
  * Configuration
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 18:51
  */

trait Configuration {
  def servers:       String
  def path:          String
  def timeout:       Duration
  def watcher:       (StateEvent, Session) => Unit
  def allowReadOnly: Boolean
  def exec:          ExecutionContext
}

object Configuration {
  def apply(servers: String):               Configuration = new Builder(servers).build()
  def apply(servers: String, path: String): Configuration = new Builder(servers).withPath(path).build()
  def unapply(
      config: Configuration
  ): Option[(String, String, Duration, (StateEvent, Session) => Unit, Boolean, ExecutionContext)] = {
    if (config == null)
      None
    else
      Some((config.servers, config.path, config.timeout, config.watcher, config.allowReadOnly, config.exec))
  }

  private class Impl(
      val servers:       String,
      val path:          String,
      val timeout:       Duration,
      val watcher:       (StateEvent, Session) => Unit,
      val allowReadOnly: Boolean,
      val exec:          ExecutionContext
  ) extends Configuration

  class Builder private (
      servers:       String,
      path:          String,
      timeout:       Duration,
      watcher:       (StateEvent, Session) => Unit,
      allowReadOnly: Boolean,
      exec:          ExecutionContext
  ) {
    private[Configuration] def this(servers: String) =
      this(servers, "", 60.seconds, null, false, null)

    private[Configuration] def this(config: Configuration) =
      this(config.servers, config.path, config.timeout, config.watcher, config.allowReadOnly, config.exec)

    def withPath(path: String): Builder =
      new Builder(servers, path, timeout, watcher, allowReadOnly, exec)

    def withTimeout(timeout: Duration): Builder =
      new Builder(servers, path, timeout, watcher, allowReadOnly, exec)

    def withWatcher(watcher: (StateEvent, Session) => Unit): Builder =
      new Builder(servers, path, timeout, watcher, allowReadOnly, exec)

    def withAllowReadOnly(allowReadOnly: Boolean): Builder =
      new Builder(servers, path, timeout, watcher, allowReadOnly, exec)

    def withExec(exec: ExecutionContext): Builder =
      new Builder(servers, path, timeout, watcher, allowReadOnly, exec)

    def build(): Configuration = new Impl(servers, path, timeout, watcher, allowReadOnly, exec)
  }

  implicit def configToBuilder(config: Configuration): Builder       = new Builder(config)
  implicit def builderToConfig(builder: Builder):      Configuration = builder.build()
}
