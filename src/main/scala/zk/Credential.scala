/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk
import org.apache.zookeeper.ZooKeeper
import scala.language._

/**
  * Credential
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 18:51
  */

trait Credential {
  def id:       Long
  def password: Array[Byte]
}

object Credential {

  def apply(id: Long, password: Array[Byte]): Credential = {
    val _id       = id
    val _password = password
    new Credential {
      val id:       Long        = _id
      val password: Array[Byte] = _password

      override def toString: String = "Credential(id=" + id + ",password=" + convert(password) + ")"

      private[this] def convert(a: Array[Byte]) = {
        if (a == null)
          "null"
        else
          a.foldLeft("") {
            case (buf, b) if buf.isEmpty => "[" + hex(b)
            case (buf, b)                => buf + " " + hex(b)
          } + "]"
      }

      private[this] def hex(b: Byte): String = {
        def hexChar(c: Int) = {
          (if (c < 10) '0' + c else 'a' + (c - 10)).toChar
        }
        hexChar((b >>> 4) & 0x0f).toString + hexChar(b & 0x0f)
      }
    }
  }

  def unapply(cred: Credential): Option[(Long, Array[Byte])] = if (cred == null) None else Some(cred.id, cred.password)

  private[zk] def apply(zk: ZooKeeper): Credential = apply(zk.getSessionId, zk.getSessionPasswd)
}
