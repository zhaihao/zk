/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk
import org.apache.zookeeper.data.Stat

/**
  * Status
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 19:30
  */
trait Status {
  def path:           String
  def czxid:          Long
  def mzxid:          Long
  def pzxid:          Long
  def ctime:          Long
  def mtime:          Long
  def version:        Int
  def cversion:       Int
  def aversion:       Int
  def ephemeralOwner: Long
  def dataLength:     Int
  def numChildren:    Int
}

private[zk] object Status {
  def apply(path: String, stat: Stat): Status = {
    val _path = path
    new Status {
      val path:           String = _path
      val czxid:          Long   = stat.getCzxid
      val mzxid:          Long   = stat.getMzxid
      val pzxid:          Long   = stat.getPzxid
      val ctime:          Long   = stat.getCtime
      val mtime:          Long   = stat.getMtime
      val version:        Int    = stat.getVersion
      val cversion:       Int    = stat.getCversion
      val aversion:       Int    = stat.getAversion
      val ephemeralOwner: Long   = stat.getEphemeralOwner
      val dataLength:     Int    = stat.getDataLength
      val numChildren:    Int    = stat.getNumChildren

      override def toString: String = {
        "Status(path=" + path + ",czxid=" + czxid + ",mzxid=" + mzxid + ",ctime=" + ctime + ",mtime=" +
          mtime + ",version=" + version + ",cversion=" + cversion + ",aversion=" + aversion + ",ephemeralOwner=" +
          ephemeralOwner + ",dataLength=" + dataLength + ",numChildren=" + numChildren + ")"
      }
    }
  }
}
