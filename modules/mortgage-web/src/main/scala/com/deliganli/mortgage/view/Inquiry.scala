package com.deliganli.mortgage
package view

import java.text.DecimalFormat

import com.deliganli.mortgage.codec.Decoder
import com.deliganli.mortgage.codec.Encoder

import calico.*
import calico.html.io.{*, given}
import calico.syntax.*
import cats.Monad
import cats.effect.*
import cats.effect.IO
import cats.implicits.*
import fs2.Stream
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import fs2.dom.*
import monocle.macros.GenLens
import monocle.syntax.all._

import model.*

object Inquiry {
  object L {
    val propPrice  = GenLens[Inquiry](_.property.price)
    val propCosts  = GenLens[Inquiry](_.property.costs)
    val propApp    = GenLens[Inquiry](_.property.appreciation)
    val down       = GenLens[Inquiry](_.loan.downpayment)
    val interest   = GenLens[Inquiry](_.loan.interest)
    val term       = GenLens[Inquiry](_.loan.term)
    val oppoGrowth = GenLens[Inquiry](_.opportunity.growth)
    val rentP      = GenLens[Inquiry](_.opportunity.rent.payment)
    val rentI      = GenLens[Inquiry](_.opportunity.rent.increase)
    val cur        = GenLens[Inquiry](_.currency)
  }

  val df = new DecimalFormat("#,###.00")

  given Encoder[Value] = v =>
    v match
      case v: Value.Rate => Encoder[Value.Rate].encode(v)
      case v: Value.Flat => Encoder[Value.Flat].encode(v)

  given Encoder[Value.Rate] = v => v.amount.toString
  given Decoder[Value.Rate] = v =>
    Either
      .catchNonFatal(v.toDouble)
      .map(Value.Rate.apply)
      .leftMap(_.getMessage)

  given Encoder[Value.Flat] = v => df.format(v.amount)
  given Decoder[Value.Flat] = v =>
    Either
      .catchNonFatal(v.replace(",", "").toDouble.toInt)
      .map(Value.Flat.apply)
      .leftMap(_.getMessage)

  def valueInput(
    init: Value,
    tag: String,
    lbl: String,
    column: String = "col"
  )(
    sig: SignallingRef[IO, Value],
    cur: Signal[IO, String]
  ) = {
    def encodeType(v: Value): String = v match
      case v: Value.Rate => Encoder[Value.Rate].encode(v)
      case v: Value.Flat => Encoder[Value.Flat].encode(v)

    def decodeType(t: String, v: String): Either[String, Value] = t match
      case "Rate" => Decoder[Value.Rate].decode(v)
      case "Flat" => Decoder[Value.Flat].decode(v)

    def selectWhen(t: String) = Option.when(init.getClass.getSimpleName == t)(selected := true)
    def optionOf(t: String)   = option(value := t, t, selectWhen(t))

    div(
      cls := column,
      label(
        cls   := "form-label",
        forId := s"input-$tag",
        lbl
      ),
      div(
        cls := "input-group mb-3",
        (
          select(
            cls := "input-group-text",
            optionOf("Rate"),
            optionOf("Flat")
          ),
          input(
            idAttr := s"input-$tag",
            tpe    := "text",
            cls    := "form-control",
            value  := Encoder[Value].encode(init)
          )
        ).tupled.flatTap { case (s, i) =>
          def reset = (
            s.value.get,
            i.value.get
          ).tupled.flatMap { case (st, iv) =>
            decodeType(st, iv)
              .traverseTap(v => sig.set(v))
              .flatTap(_.map(encodeType).traverse(i.value.set))
              .void
          }

          val setOnBlur = onBlur --> (_.foreach(_ => reset))

          (
            i.modify(setOnBlur),
            s.modify(setOnBlur)
          ).tupled
        },
        span {
          val indicator = (sig, cur).tupled.map { case (v, c) =>
            v match
              case v: Value.Rate => "%"
              case v: Value.Flat => c
          }

          (
            cls := "input-group-text",
            indicator
          )
        }
      )
    )
  }

  def labeledInput[T: Decoder: Encoder](
    init: T,
    tag: String,
    lbl: String,
    column: String = "col"
  )(
    sig: SignallingRef[IO, T]
  ) = div(
    cls := column,
    label(
      cls   := "form-label",
      forId := s"input-$tag",
      lbl
    ),
    input.withSelf { self =>
      def initValue = Encoder[T].encode(init)

      def reset = self.value.get
        .map(Decoder[T].decode)
        .flatMap(_.traverse(sig.set))

      (
        idAttr      := s"input-$tag",
        tpe         := "text",
        cls         := "form-control",
        placeholder := initValue,
        value       := initValue,
        onBlur --> (_.foreach(e => reset.void))
      )
    }
  )

  def rightAnnotatedInput[T: Decoder: Encoder](
    init: T,
    tag: String,
    lbl: String,
    hlp: Option[String] = None,
    column: String = "col"
  )(
    sig: SignallingRef[IO, T],
    righHand: Signal[IO, String]
  ) =
    div(
      cls := column,
      label(
        cls   := "form-label",
        forId := s"input-$tag",
        lbl
      ),
      div(
        cls := "input-group mb-3 col",
        input.withSelf { self =>
          def initValue = Encoder[T].encode(init)

          def reset = self.value.get
            .map(Decoder[T].decode)
            .flatTap(_.traverse(sig.set))
            .flatMap(_.traverse(v => self.value.set(Encoder[T].encode(v))))

          (
            idAttr      := s"input-$tag",
            tpe         := "text",
            cls         := "form-control",
            placeholder := initValue,
            value       := initValue,
            onBlur --> (_.foreach(e => reset.void))
          )
        },
        span(
          cls := "input-group-text",
          righHand
        )
      ),
      div(
        idAttr := s"help-$tag",
        cls    := "form-text",
        hlp
      )
    )

  def apply(
    inq: SignallingRef[IO, Inquiry]
  )(
    rowc: String = "row g-3"
  ) = inq.get.toResource.flatMap { now =>
    val percent = Signal.constant[IO, String]("%")

    div(
      cls := rowc,
      div(
        cls := "d-md-flex flex-md-row align-items-center justify-content-between",
        h2(cls := "text-center", "Inquiry"),
        labeledInput(
          now.currency,
          "currency",
          "Currency",
          column = "col-lg-2"
        )(inq.zoom(L.cur))
      ),
      hr(""),
      form(
        fieldSet(
          cls := rowc,
          legend("Property"),
          rightAnnotatedInput(
            now.property.price,
            "property-price",
            "Price",
            hlp = "Full sale price".some,
            column = "col-lg-4"
          )(inq.zoom(L.propPrice), inq.zoom(L.cur)),
          rightAnnotatedInput(
            now.property.costs,
            "property-costs",
            "Costs",
            hlp = "Maintenance, Annual".some,
            column = "col"
          )(inq.zoom(L.propCosts), percent),
          rightAnnotatedInput(
            now.property.appreciation,
            "appreciation",
            "Appreciation",
            hlp = "Annual".some
          )(inq.zoom(L.propApp), percent)
        ),
        hr(""),
        fieldSet(
          cls := rowc,
          legend("Loan"),
          valueInput(
            now.loan.downpayment,
            "downpayment",
            "Downpayment",
            column = "col-lg-6"
          )(inq.zoom(L.down), inq.zoom(L.cur)),
          rightAnnotatedInput(
            now.loan.interest,
            "interest-rate",
            "Interest Rate",
            hlp = "Annual".some,
            column = "col-lg"
          )(inq.zoom(L.interest), percent),
          rightAnnotatedInput(
            now.loan.term,
            "loan-term",
            "Term",
            hlp = "How long loan is for".some,
            column = "col-lg"
          )(
            inq.zoom(L.term),
            inq.map(_.loan.term).map {
              case 1 => "Year"
              case _ => "Years"
            }
          )
        ),
        hr(""),
        fieldSet(
          cls := rowc,
          legend("Opportunity Cost"),
          rightAnnotatedInput(
            now.opportunity.growth,
            "opportunity-rate",
            "Growth Rate",
            hlp = "Annual CAGR".some,
            column = "col-lg-5"
          )(inq.zoom(L.oppoGrowth), percent),
          rightAnnotatedInput(
            now.opportunity.rent.payment,
            "rent",
            "Rent",
            hlp = "Current monthly rent".some,
            column = "col-lg-4"
          )(inq.zoom(L.rentP), inq.zoom(L.cur)),
          rightAnnotatedInput(
            now.opportunity.rent.increase,
            "rent",
            "Rent Increase",
            hlp = "Annual".some,
            column = "col-lg-3"
          )(inq.zoom(L.rentI), percent)
        )
      )
    )
  }
}
