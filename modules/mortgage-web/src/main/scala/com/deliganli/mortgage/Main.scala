package com.deliganli.mortgage

import com.deliganli.web.core.store.Store

import calico.*
import calico.IOWebApp
import calico.html.io.{*, given}
import calico.syntax.*
import cats.effect.*
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.implicits.*
import fs2.concurrent.SignallingRef
import fs2.dom.*

import json.given
import model.*

object Main extends IOWebApp {

  def defaultInquiry = Inquiry(
    "€",
    Property(
      Value.Flat(600000),
      Value.Rate(4),
      Value.Rate(2)
    ),
    Loan(
      Value.Rate(20),
      Value.Rate(5.54),
      30
    ),
    Opportunity(
      Value.Rate(4),
      Rent(
        Value.Flat(2000),
        Value.Rate(3)
      )
    )
  )

  def render: Resource[IO, HtmlElement[IO]] =
    Store
      .create[IO, Inquiry]("inquiry")(Window[IO])
      .mproduct { store =>
        store.signal.get
          .map(_.getOrElse(defaultInquiry))
          .flatMap(SignallingRef[IO].of)
          .toResource
      }
      .flatTap { case (store, inq) =>
        inq.discrete
          .foreach(v => store.save(v))
          .compile
          .drain
          .background
      }
      .flatMap { case (store, inq) =>
        div(
          cls := "d-flex flex-column min-vh-100",
          view.Header(),
          div(
            cls := "container-md mt-3",
            div(
              cls := "row",
              view.Inquiry(inq)().flatTap(_.modify(cls := "col-sm")),
              div(
                cls := "col",
                h2(cls := "text-center", "Summary"),
                view.Summary(inq)
              )
            ),
            hr(""),
            view.Comparison(inq),
            hr(""),
            view.Comparison.formulas
          ),
          view.Footer()
        )
      }
}
