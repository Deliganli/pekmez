package com.deliganli.mortgage
package view

import java.text.DecimalFormat

import calico.*
import calico.html.io.{*, given}
import calico.syntax.*
import cats.effect.IO
import cats.implicits.*
import fs2.concurrent.SignallingRef

import model.*
import fs2.concurrent.Signal
import scala.math.BigDecimal.RoundingMode

object Comparison {
  val df = new DecimalFormat("#,###.00")

  def apply(inq: SignallingRef[IO, Inquiry]) = {
    val dataSig = inq.map { x =>
      val hs = homeownership(
        x.loan.term,
        (x.installmentCount * x.monthlyPayment).toDouble,
        (x.monthlyPayment * 12).toDouble,
        x.property.costs,
        x.property.price.amount
      )(x.property.appreciation)

      val is = investment(
        x.loan.term,
        x.downpayment.toDouble,
        (x.monthlyPayment * 12).toDouble,
        x.opportunity.rent.payment.amount * 12
      )(x.opportunity.growth, x.opportunity.rent.increase)

      hs.zip(is)
    }

    div(
      h2("Comparison"),
      p(
        "Comparing buying a home with a loan versus renting and investing the funds. ",
        "Monthly budget for both scenarios are equal to highest affordable amount, the mortgage installment. "
      ),
      Charts.view(dataSig),
      div(
        cls := "row",
        div(
          cls := "col",
          h3(cls := "text-center", "Homeownership"),
          table(
            cls := "table table-striped",
            thead(
              titles("Year", "Debt", "Growth", "NAV")
            ),
            dataSig.map(_.map(_._1)).map { hs =>
              tbody {
                (
                  hs.headOption.map(v => "Start" -> v).toList
                    ++ hs.tail.zipWithIndex.map { case (v, i) => (i + 1).toString -> v }
                ).map(homeownershipView)
              }
            }
          )
        ),
        div(
          cls := "col",
          h3(cls := "text-center", "Renting & Investing"),
          table(
            cls := "table table-striped",
            thead(
              titles("Year", "Contribution", "Growth", "NAV")
            ),
            dataSig.map(_.map(_._2)).map { is =>
              tbody {
                (
                  is.headOption.map(v => "Start" -> v).toList
                    ++ is.tail.zipWithIndex.map { case (v, i) => (i + 1).toString -> v }
                ).map(investingView)
              }
            }
          )
        )
      )
    )
  }

  case class Investment(
    contribution: BigDecimal,
    growth: BigDecimal,
    nav: BigDecimal
  )

  def investment(
    years: Int,
    initial: BigDecimal,
    budget: BigDecimal,
    baselineRent: BigDecimal
  )(
    growth: Value.Rate,
    rentGrowth: Value.Rate
  ) = (1 to years).toList
    .scanLeft((baselineRent, Investment(0, 0, initial))) { case ((lastRent, last), year) =>
      val navGrowth    = last.nav * growth.rate
      val currentRent  = lastRent * (1 + rentGrowth.rate)
      val contribution = (budget - lastRent).max(0)
      val compoundCont = last.contribution + contribution
      val compoundNav  = last.nav + navGrowth + contribution

      (currentRent, Investment(compoundCont, navGrowth, compoundNav))
    }
    .map(_._2)

  case class Homeownership(
    appreciation: BigDecimal,
    debt: BigDecimal,
    nav: BigDecimal
  )

  def homeownership(
    years: Int,
    debt: Double,
    installment: Double,
    cost: Value.Rate,
    value: BigDecimal
  )(
    growth: Value.Rate
  ) = (1 to years).toList
    .scanLeft((value, Homeownership(0, debt, value - debt))) { case ((assetValue, last), year) =>
      val g             = (assetValue * growth.rate).setScale(2, RoundingMode.HALF_EVEN)
      val remainingDebt = BigDecimal(0).max(last.debt - installment).setScale(2, RoundingMode.HALF_EVEN)
      val cost1         = assetValue * cost.rate
      val nav           = (assetValue + g - remainingDebt - cost1).setScale(2, RoundingMode.HALF_EVEN)

      (assetValue + g, Homeownership(g, remainingDebt, nav))
    }
    .map(_._2)

  def investingView(year: String, v: Investment) =
    tr(
      td(cls := "text-end", year),
      td(cls := "text-end", df.format(v.contribution)),
      td(cls := "text-end", df.format(v.growth)),
      td(cls := "text-end", df.format(v.nav))
    )

  def homeownershipView(year: String, v: Homeownership) =
    tr(
      td(cls := "text-end", year),
      td(cls := "text-end", df.format(v.debt)),
      td(cls := "text-end", df.format(v.appreciation)),
      td(cls := "text-end", df.format(v.nav))
    )

  def titles(vs: String*) =
    tr(vs.toList.map(v => th(cls := "text-end", v)))

  def formulas =
    div(
      h3("Formulas"),
      table(
        cls := "table",
        tbody(
          tr(
            td("Homeownership"),
            td("Debt"),
            td("[Monthly Payment] * [Installment Count]")
          ),
          tr(
            td("Homeownership"),
            td("Growth"),
            td("[Compounding Property Price] * [Appreciation]")
          ),
          tr(
            td("Homeownership"),
            td("NAV"),
            td("[Compounding Property Price] + [Growth] - [Debt] - [Costs]")
          ),
          tr(
            td("Renting & Investing"),
            td("Contribution"),
            td("([Monthly Payment] - [Rent]) * 12")
          ),
          tr(
            td("Renting & Investing"),
            td("Growth"),
            td("[Compounding NAV] * [CAGR]")
          ),
          tr(
            td("Renting & Investing"),
            td("NAV"),
            td("[Compounding NAV] + [Growth] + [Contribution]")
          )
        )
      )
    )

  object Charts {
    import facade.ChartJS
    import facade.ChartJS.*
    import scalajs.js
    import scala.scalajs.js.JSConverters.*

    type Data = List[(Homeownership, Investment)]

    def view(inq: Signal[IO, Data]) =
      canvasTag(cls := "bg-body").flatTap { self =>
        val chart = ChartJS(self, chartConfig)

        inq.discrete
          .foreach(updateChart(chart))
          .compile
          .drain
          .background
      }

    def updateChart(chart: ChartJS)(x: Data) = IO.delay {
      val years = x.indices.map(v => s"Year $v")
      val hs    = x.map(_._1)
      val is    = x.map(_._2)

      chart.data.labels = years.map(_.toString).toJSArray
      chart.data.datasets.get(0).data = hs.map(_.nav.toInt.toDouble).toJSArray
      chart.data.datasets.get(1).data = is.map(_.nav.toInt.toDouble).toJSArray

      chart.update()
    }.void

    def chartConfig = new Configuration {
      `type` = "line"
      data = new ChartData {
        datasets = js.Array(
          new Dataset {
            label = "Homeownership"
            borderWidth = 5
            fill = false
            borderColor = "#689F38"
            backgroundColor = "#689F38"
          },
          new Dataset {
            label = "Renting & Investing"
            borderWidth = 5
            fill = false
            borderColor = "#039BE5"
            backgroundColor = "#039BE5"
          }
        )
      }
      options = new Options {
        scales = new Scales {
          y = new Axe {
            beginAtZero = true
          }
        }
      }
    }
  }
}
