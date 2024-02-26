package com.deliganli.mortgage
package view

import java.text.DecimalFormat

import calico.*
import calico.html.Prop
import calico.html.io.{*, given}
import calico.syntax.*
import cats.effect.IO
import cats.implicits.*
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef

import model.*

object Summary {
  val df = new DecimalFormat("#,###.00")

  def apply(inq: SignallingRef[IO, Inquiry]) =
    div(
      table(
        cls := "table table-striped",
        inq.map { x =>
          tbody(
            List(
              ("Downpayment:", x.downpayment, None),
              ("Principal:", x.principal, None),
              ("Monthly Repayment:", x.monthlyPayment, Some("table-warning")),
              ("Total Interest Repayment:", x.interestRepayment, Some("table-danger")),
              ("Total Repayment:", x.totalRepayment, Some("table-info"))
            ).map { case (k, v, c) =>
              tr(
                c.map(c => cls := c),
                td(k),
                td(cls := "text-end", df.format(v)),
                td(cls := "text-center", x.currency)
              )
            }
          )
        }
      ),
      Charts.view(inq)
    )

  object Charts {
    import facade.ChartJS
    import facade.ChartJS.*
    import scalajs.js
    import scala.scalajs.js.JSConverters.*

    def view(inq: Signal[IO, Inquiry]) =
      canvasTag("").flatTap { self =>
        val chart = ChartJS(self, pieChartConfig)

        inq.discrete
          .foreach(updatePieChart(chart))
          .compile
          .drain
          .background
      }

    def pieChartConfig = new Configuration {
      `type` = "pie"
      data = new ChartData {
        labels = js.Array("Downpayment", "Principal", "Interest")
        datasets = js.Array(new Dataset {
          borderWidth = 1
          backgroundColor = js.Array(
            "#689F38",
            "#039BE5",
            "#F4511E"
          )
        })
      }
      options = new Options {
        layout = new Layout {
          padding = 20
        }
      }
    }

    def updatePieChart(chart: ChartJS)(x: Inquiry) = IO.delay {
      chart.data.datasets.get(0).data = Array(
        x.downpayment,
        x.principal,
        x.interestRepayment
      ).map(_.toDouble).toJSArray

      chart.update()
    }.void
  }
}
