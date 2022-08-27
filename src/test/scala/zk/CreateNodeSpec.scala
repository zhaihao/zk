/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.DurationInt

/**
  * CreateNodeSpec
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/20 18:11
  */
class CreateNodeSpec extends Spec with StrictLogging {
  val path1 = root + "/testA"

  "退出不删除" in {
    sync.create(path1, "hello".getBytes, ACL.AnyoneAll, Persistent)
    val (a, _) = sync.get(path1)
    new String(a) ==> "hello"

    reconnect
    new String(sync.get(path1)._1) ==> "hello"
  }

  "退出不删除并且带序列" in {
    sync.create(path1, "hello1".getBytes, ACL.AnyoneAll, PersistentSequential) ==> path1 + "0000000000"
    new String(sync.get(path1 + "0000000000")._1)                              ==> "hello1"

    sync.create(path1, "hello2".getBytes, ACL.AnyoneAll, PersistentSequential) ==> path1 + "0000000001"
    new String(sync.get(path1 + "0000000001")._1)                              ==> "hello2"

    reconnect
    new String(sync.get(path1 + "0000000000")._1) ==> "hello1"
    new String(sync.get(path1 + "0000000001")._1) ==> "hello2"
  }

  "退出不删除但有TTL" in {
    sync.create(path1, "hello".getBytes, ACL.AnyoneAll, PersistentTTL(10.seconds))
    gc.clean(8.seconds)
    new String(sync.get(path1)._1) ==> "hello"
    gc.clean(8.seconds)
    new String(sync.get(path1)._1) ==> "hello"
    gc.clean(18.seconds)
    assertThrows[NoNodeException] { new String(sync.get(path1)._1) ==> "hello" }
  }

  "退出后就删除的节点" in {
    sync.create(path1, "hello".getBytes(), ACL.AnyoneAll, Ephemeral)
    new String(sync.get(path1)._1) ==> "hello"
    reconnect
    assertThrows[NoNodeException] { sync.get(path1) }
  }

  "容器节点" in {
    sync.create(path1, null, ACL.AnyoneAll, Container)
    sync.create(path1 + "/abc", null, ACL.AnyoneAll, Persistent)
    sync.delete(path1 + "/abc")
    gc.clean(10.millis)
    assertThrows[NoNodeException] { sync.get(path1) }
  }
}
