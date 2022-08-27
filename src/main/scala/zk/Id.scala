/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk

import java.net.{InetAddress, Inet4Address, UnknownHostException}
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.data.{Id => ZId}
import scala.language._
import scala.util.{Failure, Success, Try}

/**
  * Id
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 18:56
  */

sealed trait Id {
  def scheme: String
  def id:     String
  def permit(permission: Int): ACL = ACL(this, permission)

  private[zk] def zid: ZId
}

abstract class BaseId(val scheme: String, val id: String) extends Id {
  private[zk] val zid = new ZId(scheme, id)
}


case object WorldId extends BaseId("world", "anyone")
case object AuthId extends BaseId("auth", "")
case class DigestId(username: String, password: String) extends BaseId("digest", username + ":" + password)
case class HostId(domain: String) extends BaseId("host", domain)
case class IpId(address: String, prefix: Int) extends BaseId("ip", address + "/" + prefix)

object Id {

  val Anyone: Id = Id(Ids.ANYONE_ID_UNSAFE)
  val Creator: Id = Id(Ids.AUTH_IDS)
  def apply(scheme: String, id: String): Id = apply(scheme + ":" + id)

  def apply(s: String): Id = parse(s) match {
    case Success(id) => id
    case Failure(e)  => throw e
  }

  private[zk] def apply(zid: ZId): Id = apply(zid.getScheme, zid.getId)

  def unapply(id: Id): Option[(String, String)] =
    if (id == null) None else Some(id.scheme, id.id)

  def parse(s: String): Try[Id] = Try {
    def error(message: String): Nothing =
      throw new IllegalArgumentException(s"$s: $message")

    s.split(":", 2) match {
      case Array("world", id) =>
        if (id == "anyone") WorldId
        else if (id == "") error("missing id 'anyone'")
        else error("id not recognized")
      case Array("world") =>
        error("missing id 'anyone'")
      case Array("auth", id) =>
        if (id == "") AuthId else error("id must be empty")
      case Array("auth") =>
        AuthId
      case Array("digest", id) =>
        id.split(":", 2) match {
          case Array(username, password) => DigestId(username, password)
          case _                         => error("missing password")
        }
      case Array("digest") =>
        error("missing username:password")
      case Array("host", id) =>
        if (id == "") error("missing domain") else HostId(id)
      case Array("host") =>
        error("missing domain")
      case Array("ip", id) =>
        if (id == "") error("missing address")
        else {
          def verify(address: String, prefix: Option[String]) = {
            val a =
              try InetAddress.getByName(address)
              catch {
                case _: UnknownHostException => error("unknown host")
              }
            val MaxPrefix = if (a.isInstanceOf[Inet4Address]) 32 else 128
            val p = prefix match {
              case Some(_prefix) =>
                try {
                  val p = _prefix.toInt
                  if (p < 0 || p > MaxPrefix) error("invalid prefix")
                  p
                } catch {
                  case _: NumberFormatException =>
                    if (_prefix == "") error("missing prefix")
                    else error("invalid prefix")
                }
              case None => MaxPrefix
            }
            (a.getHostAddress, p)
          }
          id.split("/", 2) match {
            case Array(address, prefix) =>
              val (a, p) = verify(address, Some(prefix))
              IpId(a, p)
            case Array(address) =>
              val (a, p) = verify(address, None)
              IpId(a, p)
          }
        }
      case Array("ip") =>
        error("missing address")
      case _ =>
        error("scheme not recognized")
    }
  }

  implicit def tupleToIdentity(id: (String, String)): Id = Id(id._1, id._2)
}
