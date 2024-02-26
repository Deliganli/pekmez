package com.deliganli.web.core
package store

import calico.html.io.*
import calico.html.io.given
import calico.syntax.*
import cats.Applicative
import cats.effect.implicits._
import cats.effect.kernel.Concurrent
import cats.effect.kernel.Resource
import cats.implicits.*
import cats.implicits._
import fs2.concurrent.*
import fs2.dom.*

import Store.*

trait Store[F[_], A] {
  def save(a: A): F[Unit]
  def signal: Signal[F, Option[A]]
}

object Store {
  import com.github.plokhotnyuk.jsoniter_scala.core._

  def create[F[_]: Concurrent, A: JsonValueCodec](
    key: String
  )(
    window: Window[F]
  )(implicit oa: JsonValueCodec[Option[A]]
  ): Resource[F, Store[F, A]] = {
    def decode(v: String) = Either.catchNonFatal(readFromString[A](v)).toOption

    for
      ref <- SignallingRef[F].of(Option.empty[A]).toResource
      _ <- window.localStorage
        .getItem(key)
        .map(_.flatMap(decode))
        .flatMap(ref.set)
        .toResource
      _ <- window.localStorage
        .events(window)
        .foreach {
          case Storage.Event.Updated(`key`, _, value, _) => ref.set(decode(value))
          case _                                         => Applicative[F].unit
        }
        .compile
        .drain
        .background
      _ <- ref.discrete
        .foreach(a => Concurrent[F].cede *> window.localStorage.setItem(key, writeToString(a)))
        .compile
        .drain
        .background
    yield new Store[F, A] {
      def save(a: A): F[Unit]          = ref.set(a.some)
      def signal: Signal[F, Option[A]] = ref
    }
  }

}
