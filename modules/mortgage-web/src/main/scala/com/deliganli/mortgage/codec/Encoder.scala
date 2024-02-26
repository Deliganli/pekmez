package com.deliganli.mortgage.codec

trait Encoder[A] {
  def encode(a: A): String
}

object Encoder {
  def apply[A](implicit ev: Encoder[A]) = ev

  def instance[A](f: A => String) = new Encoder[A] {
    def encode(a: A): String = f(a)
  }

  given Encoder[String] = instance(identity)
  given Encoder[Int]    = instance(_.toString)
}
