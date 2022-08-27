/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk
import org.apache.zookeeper.KeeperException.Code
import org.apache.zookeeper.data.{Stat, ACL => ZACL}
import org.apache.zookeeper.ZooKeeper
import org.apache.zookeeper.ZooKeeper.States
import org.apache.zookeeper.{KeeperException => ZException, _}

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters._
import scala.language._

/**
  * Zookeeper
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 19:30
  */
trait Zookeeper {
  def wrapped: ZooKeeper
  def state:   States
  def sync:    SynchronousZookeeper
  def async:   AsynchronousZookeeper
  def close:   Unit
  def ensure(path: String): Future[Unit]
}

trait SynchronousZookeeper extends Zookeeper {
  def create(path:    String, data:    Array[Byte], acl:     Seq[ACL], disposition: Disposition): String
  def delete(path:    String, version: Option[Int] = None): Unit
  def deleteAll(path: String): Unit
  def get(path:       String): (Array[Byte], Status)
  def set(path:       String, data:    Array[Byte], version: Option[Int]): Status
  def exists(path:    String): Option[Status]
  def children(path:  String): Seq[String]
  def getACL(path:    String): (Seq[ACL], Status)
  def setACL(path:    String, acl:     Seq[ACL], version:    Option[Int]): Status
  def watch(fn:       PartialFunction[Event, Unit]): SynchronousWatchableZookeeper
  def transact(ops:   Seq[Operation]): Either[Seq[Problem], Seq[Result]]
}

trait SynchronousWatchableZookeeper extends Zookeeper {
  def get(path:      String): (Array[Byte], Status)
  def exists(path:   String): Option[Status]
  def children(path: String): Seq[String]
}

trait AsynchronousZookeeper extends Zookeeper {
  def create(path:    String, data:    Array[Byte], acl:     Seq[ACL], disposition: Disposition): Future[String]
  def delete(path:    String, version: Option[Int] = None): Future[Unit]
  def deleteAll(path: String): Future[Unit]
  def get(path:       String): Future[(Array[Byte], Status)]
  def set(path:       String, data:    Array[Byte], version: Option[Int]): Future[Status]
  def exists(path:    String): Future[Option[Status]]
  def children(path:  String): Future[(Seq[String], Status)]
  def getACL(path:    String): Future[(Seq[ACL], Status)]
  def setACL(path:    String, acl:     Seq[ACL], version:    Option[Int]): Future[Status]
  def watch(fn:       PartialFunction[Event, Unit]): AsynchronousWatchableZookeeper
}

trait AsynchronousWatchableZookeeper extends Zookeeper {
  def get(path:      String): Future[(Array[Byte], Status)]
  def exists(path:   String): Future[Option[Status]]
  def children(path: String): Future[(Seq[String], Status)]
}

private class BaseZK(zk: ZooKeeper, exec: ExecutionContext) extends Zookeeper {

  def state:   States                = zk.getState
  def wrapped: ZooKeeper             = zk
  def sync:    SynchronousZookeeper  = new SynchronousZK(zk, exec)
  def async:   AsynchronousZookeeper = new AsynchronousZK(zk, exec)
  def close:   Unit                  = zk.close()

  def ensure(path: String): Future[Unit] = {
    val p = Promise[Unit]()
    zk.sync(path, VoidHandler(p), null)
    p.future
  }

}

private class SynchronousZK(zk: ZooKeeper, exec: ExecutionContext) extends BaseZK(zk, exec) with SynchronousZookeeper {
  def create(path: String, data: Array[Byte], acl: Seq[ACL], disposition: Disposition): String = {
    val stat = new Stat
    disposition match {
      case TTL(ttl) =>
        zk.create(path, data, ACL.toZACL(acl), disposition.mode, stat, ttl.toMillis)
      case _ =>
        zk.create(path, data, ACL.toZACL(acl), disposition.mode, stat)
    }
  }

  def delete(path: String, version: Option[Int] = None): Unit = {
    zk.delete(path, version getOrElse -1)
  }

  override final def deleteAll(path: String): Unit = {
    val list = children(path)
    if (list.isEmpty) {
      delete(path)
    } else {
      list.foreach(p => deleteAll(path + "/" + p))
      delete(path)
    }
  }

  def get(path: String): (Array[Byte], Status) = {
    val stat = new Stat
    val data = zk.getData(path, false, stat)
    (if (data == null) Array() else data, Status(path, stat))
  }

  def set(path: String, data: Array[Byte], version: Option[Int]): Status = {
    val stat = zk.setData(path, data, version.getOrElse(-1))
    Status(path, stat)
  }

  def exists(path: String): Option[Status] = {
    val stat = zk.exists(path, false)
    if (stat == null) None else Some(Status(path, stat))
  }

  def children(path: String): Seq[String] = {
    zk.getChildren(path, false).asScala.toList
  }

  def getACL(path: String): (Seq[ACL], Status) = {
    val stat = new Stat
    val zacl = zk.getACL(path, stat)
    (ACL(zacl), Status(path, stat))
  }

  def setACL(path: String, acl: Seq[ACL], version: Option[Int]): Status = {
    val stat = zk.setACL(path, ACL.toZACL(acl), version.getOrElse(-1))
    Status(path, stat)
  }

  def watch(fn: PartialFunction[Event, Unit]): SynchronousWatchableZookeeper = {
    new SynchronousWatchableZK(zk, exec, fn)
  }

  def transact(ops: Seq[Operation]): Either[Seq[Problem], Seq[Result]] = {
    try {
      val _ops    = ops.map { _.op }
      val results = zk.multi(_ops.asJava).asScala
      Right(ops.zip(results).map { case (op, result) =>
        op match {
          case _op: CreateOperation => CreateResult(_op, result.asInstanceOf[OpResult.CreateResult].getPath)
          case _op: DeleteOperation => DeleteResult(_op)
          case _op: SetOperation =>
            SetResult(_op, Status(_op.path, result.asInstanceOf[OpResult.SetDataResult].getStat))
          case _op: CheckOperation => CheckResult(_op)
        }
      })
    } catch {
      case e: KeeperException if e.getResults != null =>
        Left(ops.zip(e.getResults.asScala).map { case (op, result) =>
          val rc    = result.asInstanceOf[OpResult.ErrorResult].getErr
          val error = if (rc == 0) None else Some(ZException.create(Code.get(rc)))
          op match {
            case _op: CreateOperation => CreateProblem(_op, error)
            case _op: DeleteOperation => DeleteProblem(_op, error)
            case _op: SetOperation    => SetProblem(_op, error)
            case _op: CheckOperation  => CheckProblem(_op, error)
          }
        })
    }
  }

}

private class SynchronousWatchableZK(zk: ZooKeeper, exec: ExecutionContext, fn: PartialFunction[Event, Unit])
    extends BaseZK(zk, exec)
    with SynchronousWatchableZookeeper {
  private[this] val watcher = new Watcher {
    def process(event: WatchedEvent): Unit = {
      val e = Event(event)
      if (fn.isDefinedAt(e)) fn(e)
    }
  }

  def get(path: String): (Array[Byte], Status) = {
    val stat = new Stat
    val data = zk.getData(path, watcher, stat)
    (if (data == null) Array() else data, Status(path, stat))
  }

  def exists(path: String): Option[Status] = {
    val stat = zk.exists(path, watcher)
    if (stat == null) None else Some(Status(path, stat))
  }

  def children(path: String): Seq[String] = {
    zk.getChildren(path, watcher).asScala.toList
  }
}

private class AsynchronousZK(zk: ZooKeeper, exec: ExecutionContext)
    extends BaseZK(zk, exec)
    with AsynchronousZookeeper {

  def create(path: String, data: Array[Byte], acl: Seq[ACL], disposition: Disposition): Future[String] = {
    val p = Promise[String]()
    disposition match {
      case TTL(ttl) =>
        zk.create(path, data, ACL.toZACL(acl), disposition.mode, CreateHandler(p), null, ttl.toMillis)
      case _ =>
        zk.create(path, data, ACL.toZACL(acl), disposition.mode, CreateHandler(p), null)
    }
    p.future
  }

  def delete(path: String, version: Option[Int] = None): Future[Unit] = {
    val p = Promise[Unit]()
    zk.delete(path, version.getOrElse(-1), VoidHandler(p), null)
    p.future
  }

  override def deleteAll(path: String): Future[Unit] = {
    def deleteRec(path: String): Unit = {
      val list = zk.getChildren(path, false)
      if (list.isEmpty) {
        zk.delete(path, -1)
      } else {
        list.forEach(p => deleteRec(path + "/" + p))
        zk.delete(path, -1)
      }
    }

    zk.getChildren(path, false).forEach(deleteRec)
    val p = Promise[Unit]()
    zk.delete(path, -1, VoidHandler(p), null)
    p.future
  }

  def get(path: String): Future[(Array[Byte], Status)] = {
    val p = Promise[(Array[Byte], Status)]()
    zk.getData(path, false, DataHandler(p), null)
    p.future
  }

  def set(path: String, data: Array[Byte], version: Option[Int]): Future[Status] = {
    val p = Promise[Status]()
    zk.setData(path, data, version.getOrElse(-1), StatHandler(p), null)
    p.future
  }

  def exists(path: String): Future[Option[Status]] = {
    val p = Promise[Option[Status]]()
    zk.exists(path, false, ExistsHandler(p), null)
    p.future
  }

  def children(path: String): Future[(Seq[String], Status)] = {
    val p = Promise[(Seq[String], Status)]()
    zk.getChildren(path, false, ChildrenHandler(p), null)
    p.future
  }

  def getACL(path: String): Future[(Seq[ACL], Status)] = {
    val p = Promise[(Seq[ACL], Status)]()
    zk.getACL(path, new Stat, ACLHandler(p), null)
    p.future
  }

  def setACL(path: String, acl: Seq[ACL], version: Option[Int]): Future[Status] = {
    val p = Promise[Status]()
    zk.setACL(path, ACL.toZACL(acl), version.getOrElse(-1), StatHandler(p), null)
    p.future
  }

  def watch(fn: PartialFunction[Event, Unit]): AsynchronousWatchableZookeeper = {
    new AsynchronousWatchableZK(zk, exec, fn)
  }

}

private class AsynchronousWatchableZK(zk: ZooKeeper, exec: ExecutionContext, fn: PartialFunction[Event, Unit])
    extends BaseZK(zk, exec)
    with AsynchronousWatchableZookeeper {
  private[this] val watcher = new Watcher {
    def process(event: WatchedEvent): Unit = {
      val e = Event(event)
      if (fn.isDefinedAt(e)) fn(e)
    }
  }

  def get(path: String): Future[(Array[Byte], Status)] = {
    val p = Promise[(Array[Byte], Status)]()
    zk.getData(path, watcher, DataHandler(p), null)
    p.future
  }

  def exists(path: String): Future[Option[Status]] = {
    val p = Promise[Option[Status]]()
    zk.exists(path, watcher, ExistsHandler(p), null)
    p.future
  }

  def children(path: String): Future[(Seq[String], Status)] = {
    val p = Promise[(Seq[String], Status)]()
    zk.getChildren(path, watcher, ChildrenHandler(p), null)
    p.future
  }
}

private object CreateHandler {
  def apply(p: Promise[String]): AsyncCallback.Create2Callback =
    (rc: Int, _: String, _: Object, name: String, _: Stat) => {
      (Code.get(rc): @unchecked) match {
        case Code.OK => p.success(name)
        case code    => p.failure(ZException.create(code))
      }
    }
}

private object VoidHandler {
  def apply(p: Promise[Unit]): AsyncCallback.VoidCallback =
    (rc: Int, _: String, _: Object) => {
      (Code.get(rc): @unchecked) match {
        case Code.OK => p.success(())
        case code    => p.failure(ZException.create(code))
      }
    }
}

private object DataHandler {
  def apply(p: Promise[(Array[Byte], Status)]): AsyncCallback.DataCallback =
    (rc: Int, path: String, _: Object, data: Array[Byte], stat: Stat) => {
      (Code.get(rc): @unchecked) match {
        case Code.OK => p.success(if (data == null) Array() else data, Status(path, stat))
        case code    => p.failure(ZException.create(code))
      }
    }
}

private object ChildrenHandler {
  def apply(p: Promise[(Seq[String], Status)]): AsyncCallback.Children2Callback =
    (rc: Int, path: String, _: Object, children: java.util.List[String], stat: Stat) => {
      (Code.get(rc): @unchecked) match {
        case Code.OK => p.success(children.asScala.toList, Status(path, stat))
        case code    => p.failure(ZException.create(code))
      }
    }
}

private object ACLHandler {
  def apply(p: Promise[(Seq[ACL], Status)]): AsyncCallback.ACLCallback =
    (rc: Int, path: String, _: Object, zacl: java.util.List[ZACL], stat: Stat) => {
      (Code.get(rc): @unchecked) match {
        case Code.OK => p.success(ACL(zacl), Status(path, stat))
        case code    => p.failure(ZException.create(code))
      }
    }
}

private object StatHandler {
  def apply(p: Promise[Status]): AsyncCallback.StatCallback =
    (rc: Int, path: String, _: Object, stat: Stat) => {
      (Code.get(rc): @unchecked) match {
        case Code.OK => p.success(Status(path, stat))
        case code    => p.failure(ZException.create(code))
      }
    }
}

private object ExistsHandler {
  def apply(p: Promise[Option[Status]]): AsyncCallback.StatCallback =
    (rc: Int, path: String, _: Object, stat: Stat) => {
      (Code.get(rc): @unchecked) match {
        case Code.OK => p.success(if (stat == null) None else Some(Status(path, stat)))
        case code    => p.failure(ZException.create(code))
      }
    }
}

object Zookeeper {

  def apply(config: Configuration): Zookeeper =
    apply(config, null)

  def apply(config: Configuration, cred: Credential): Zookeeper = {
    val path = (if (config.path startsWith "/") "" else "/") + config.path
    val timeout = {
      val millis = config.timeout.toMillis
      if (millis < 0) 0 else if (millis > Int.MaxValue) Int.MaxValue else millis.asInstanceOf[Int]
    }
    val watcher = {
      val fn = if (config.watcher == null) (_: StateEvent, _: Session) => () else config.watcher
      new ConnectionWatcher(fn)
    }
    val exec = if (config.exec == null) ExecutionContext.global else config.exec
    val zk = cred match {
      case null => new ZooKeeper(config.servers + path, timeout, watcher, config.allowReadOnly)
      case _    => new ZooKeeper(config.servers + path, timeout, watcher, cred.id, cred.password, config.allowReadOnly)
    }
    watcher.associate(zk)
    new BaseZK(zk, exec)
  }

  private class ConnectionWatcher(fn: (StateEvent, Session) => Unit) extends Watcher {
    private[this] val ref = new AtomicReference[ZooKeeper]

    def process(e: WatchedEvent): Unit = {
      @tailrec
      def waitfor(zk: ZooKeeper): ZooKeeper = {
        if (zk == null) waitfor(ref.get()) else zk
      }
      val zk = waitfor(ref.get())
      Event(e) match {
        case event: StateEvent => fn(event, Session(zk))
        case event => throw new AssertionError("unexpected event: " + event)
      }
    }

    def associate(zk: ZooKeeper): Unit = {
      if (!ref.compareAndSet(null, zk))
        throw new AssertionError("zookeeper instance already associated")
    }
  }
}

object SynchronousZookeeper {
  def apply(config: Configuration):                   SynchronousZookeeper = Zookeeper(config).sync
  def apply(config: Configuration, cred: Credential): SynchronousZookeeper = Zookeeper(config, cred).sync
}

object AsynchronousZookeeper {
  def apply(config: Configuration):                   AsynchronousZookeeper = Zookeeper(config).async
  def apply(config: Configuration, cred: Credential): AsynchronousZookeeper = Zookeeper(config, cred).async
}
