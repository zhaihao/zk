/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk

/**
  * Node
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 18:57
  */
/**
 * Represents a ''node'' in ZooKeeper.
 */
trait Node {

  def name:         String
  def path:         Path
  def parent:       Node
  def parentOption: Option[Node]
  def resolve(path:   String): Node
  def resolve(path:   Path): Node
  def create(data:    Array[Byte], acl: Seq[ACL], disp: Disposition): Node
  def delete(version: Option[Int]): Unit
  def get: (Array[Byte], Status)
  def get(fn:   PartialFunction[Event, Unit]): (Array[Byte], Status)
  def set(data: Array[Byte], version: Option[Int]): Status
  def exists: Option[Status]
  def exists(fn: PartialFunction[Event, Unit]): Option[Status]
  def children: Seq[Node]
  def children(fn: PartialFunction[Event, Unit]): Seq[Node]
  def getACL: (Seq[ACL], Status)
  def setACL(acl: Seq[ACL], version: Option[Int]): Status
}

object Node {
  def apply(path: String)(implicit zk: Zookeeper): Node = apply(Path(path))(zk)

  def apply(path: Path)(implicit zk: Zookeeper): Node = new Impl(zk.sync, path.normalize)

  def unapply(node: Node): Option[Path] = if (node == null) None else Some(node.path)

  private class Impl(zk: SynchronousZookeeper, val path: Path) extends Node {
    private implicit val _zk: SynchronousZookeeper = zk

    lazy val name:   String = path.name
    lazy val parent: Node   = Node(path.parent)

    lazy val parentOption: Option[Node] = path.parentOption match {
      case Some(p) => Some(Node(p))
      case _       => None
    }

    def resolve(path: String): Node = Node(this.path resolve path)
    def resolve(path: Path):   Node = resolve(path.path)

    def create(data: Array[Byte], acl: Seq[ACL], disposition: Disposition): Node = Node(
      zk.create(path.path, data, acl, disposition)
    )

    def delete(version: Option[Int]): Unit = zk.delete(path.path, version)

    def get: (Array[Byte], Status) = zk.get(path.path)

    def get(fn: PartialFunction[Event, Unit]): (Array[Byte], Status) = zk.watch(fn).get(path.path)

    def set(data: Array[Byte], version: Option[Int]): Status = zk.set(path.path, data, version)

    def exists: Option[Status] = zk.exists(path.path)

    def exists(fn: PartialFunction[Event, Unit]): Option[Status] = zk.watch(fn).exists(path.path)

    def children: Seq[Node] = zk.children(path.path).map { c => Node(path.resolve(c)) }

    def children(fn: PartialFunction[Event, Unit]): Seq[Node] =
      zk.watch(fn).children(path.path).map { c => Node(path.resolve(c)) }

    def getACL: (Seq[ACL], Status) = zk.getACL(path.path)

    def setACL(acl: Seq[ACL], version: Option[Int]): Status = zk.setACL(path.path, acl, version)
  }
}
