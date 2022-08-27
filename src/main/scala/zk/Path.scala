/*
 * Copyright (c) 2020-2022.
 * OOON.ME ALL RIGHTS RESERVED.
 * Licensed under the Mozilla Public License, version 2.0
 * Please visit <http://ooon.me> or mail to zhaihao@ooon.me
 */

package zk
import scala.annotation.tailrec
import scala.language._

/**
  * Path
  *
  * @author zhaihao
  * @version 1.0
  * @since 2022/7/16 19:29
  */
trait Path {
  def name:         String
  def path:         String
  def parent:       Path
  def parentOption: Option[Path]
  def parts:        Seq[String]
  def normalize:    Path
  def isAbsolute:   Boolean
  def resolve(path: String): Path
  def resolve(path: Path):   Path
}

object Path {
  def apply(path: String): Path = new Impl(compress(path))

  def unapplySeq(path: Path): Option[Seq[String]] = if (path == null) None else Some(path.parts)

  private def apply(parts: Seq[String]): Path = new Impl(parts.mkString("/"))

  private class Impl(val path: String) extends Path {
    lazy val name: String = parts.lastOption match {
      case Some(p) => p
      case _       => ""
    }

    lazy val parent: Path = parentOption match {
      case Some(p) => p
      case _       => throw new NoSuchElementException("no parent node")
    }

    lazy val parentOption: Option[Path] = {
      if (parts.size > 1) {
        val _parts = parts.dropRight(1)
        Some(Path(_parts.last match {
          case "" => "/"
          case _  => _parts.mkString("/")
        }))
      } else
        None
    }

    lazy val parts: Seq[String] = parse(path)

    def resolve(path: String): Path = {
      Path(path.headOption match {
        case None                   => this.path
        case Some('/')              => path
        case _ if this.path.isEmpty => path
        case _                      => this.path + "/" + path
      })
    }

    def resolve(path: Path): Path = resolve(path.path)

    lazy val normalize: Path = {
      @tailrec
      def reduce(parts: Seq[String], stack: List[String]): List[String] = {
        parts.headOption match {
          case Some(part) =>
            reduce(
              parts.tail,
              part match {
                case ".." =>
                  stack.headOption match {
                    case Some(top) => if (top == "") stack else if (top == "..") part :: stack else stack.tail
                    case _         => part :: stack
                  }
                case "." => stack
                case _   => part :: stack
              }
            )
          case _ => stack
        }
      }
      val stack = reduce(parse(path), List()).reverse
      Path(stack.headOption match {
        case None     => ""
        case Some("") => "/" + stack.tail.mkString("/")
        case _        => stack.mkString("/")
      })
    }

    lazy val isAbsolute: Boolean = path.headOption.contains('/')

    override def toString: String = path

    override def equals(that: Any): Boolean = that match {
      case _that: Path => _that.path == path
      case _ => false
    }

    override def hashCode: Int = path.hashCode
  }

  private def compress(path: String): String = {
    @tailrec
    def munch(path: Seq[Char]): Seq[Char] = {
      path.headOption match {
        case Some('/') => munch(path.tail)
        case _         => path
      }
    }
    @tailrec
    def collapse(path: Seq[Char], to: String): String = {
      path.headOption match {
        case Some(c) => collapse(if (c == '/') munch(path.tail) else path.tail, to + c)
        case _       => to
      }
    }
    val to = collapse(path, "")
    if (to.length > 1 && to.last == '/') to.dropRight(1) else to
  }

  private def parse(path: String): Seq[String] = {
    compress(path) match {
      case ""  => Seq()
      case "/" => Seq("")
      case p   => p.split('/').to(Seq)
    }
  }
}
