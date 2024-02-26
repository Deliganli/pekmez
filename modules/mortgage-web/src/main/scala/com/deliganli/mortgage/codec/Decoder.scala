package com.deliganli.mortgage.codec
import cats.implicits._

trait Decoder[A] {
  def decode(v: String): Either[String, A]
}

object Decoder {
  def apply[A](implicit ev: Decoder[A]) = ev

  def instance[A](f: String => Option[A]) = new Decoder[A] {
    def decode(v: String): Either[String, A] = f(v).toRight(s"Cannot decode value: $v")
  }

  given Decoder[String] = instance(_.some)
  given Decoder[Int]    = instance(_.toIntOption)

}
