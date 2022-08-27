/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk

import org.apache.zookeeper.ZooDefs.{Ids, Perms}
import org.apache.zookeeper.data.{ACL => ZACL}
import scala.jdk.CollectionConverters._
import scala.language._
import scala.util.{Failure, Success, Try}

/**
  * ACL
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 18:38
  */
case class ACL(id: Id, permission: Int) extends ZACL(permission, id.zid)
object ACL {
  val Read:       Int      = Perms.READ
  val Write:      Int      = Perms.WRITE
  val Create:     Int      = Perms.CREATE
  val Delete:     Int      = Perms.DELETE
  val Admin:      Int      = Perms.ADMIN
  val All:        Int      = Perms.ALL
  val AnyoneAll:  Seq[ACL] = ACL(Ids.OPEN_ACL_UNSAFE)
  val AnyoneRead: Seq[ACL] = ACL(Ids.READ_ACL_UNSAFE)
  val CreatorAll: Seq[ACL] = ACL(Ids.CREATOR_ALL_ACL)

  def apply(s: String): ACL = parse(s) match {
    case Success(acl) => acl
    case Failure(e)   => throw e
  }
  def parse(s: String): Try[ACL] = Try {
    def error(message: String): Nothing =
      throw new IllegalArgumentException(s"$s: $message")

    s.split("=", 2) match {
      case Array(id, permission) =>
        Id.parse(id) match {
          case Success(i) =>
            permission match {
              case Permission(p) => ACL(i, p)
              case _             => error("invalid permissions")
            }
          case Failure(e) => throw e
        }
      case _ => error("expecting permissions")
    }
  }

  private[zk] def apply(zacl: ZACL): ACL = ACL(Id(zacl.getId), zacl.getPerms)

  private[zk] def apply(zacl: java.util.List[ZACL]): Seq[ACL] =
    zacl.asScala.foldLeft(Seq[ACL]()) { case (acl, zacl) => acl :+ ACL(zacl) }

  private[zk] def toZACL(acl: Seq[ACL]): java.util.List[ZACL] =
    acl.foldLeft(Seq[ZACL]()) { case (zacl, acl) => zacl :+ toZACL(acl) } asJava

  private[zk] def toZACL(acl: ACL): ZACL = acl.asInstanceOf[ZACL]

  private object Permission {
    def apply(perms: Int): String = {
      (if ((perms & Read) == 0) "-" else "r") +
        (if ((perms & Write) == 0) "-" else "w") +
        (if ((perms & Create) == 0) "-" else "c") +
        (if ((perms & Delete) == 0) "-" else "d") +
        (if ((perms & Admin) == 0) "-" else "a")
    }

    def unapply(s: String): Option[Int] = {
      if (s == null) None
      else {
        val perms = s.foldLeft(0) { case (p, c) =>
          if (c == 'r') p | Read
          else if (c == 'w') p | Write
          else if (c == 'c') p | Create
          else if (c == 'd') p | Delete
          else if (c == 'a') p | Admin
          else if (c == '*') p | All
          else return None
        }
        Some(perms)
      }
    }
  }
}
