package com.deliganli.mortgage.facade

import scalajs.js
import fs2.dom.*
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.JSImport
import cats.effect.IO

import ChartJS.*

@js.native
@JSImport("chart.js/auto", "Chart")
class ChartJS(ctx: HtmlCanvasElement[IO], options: Configuration) extends js.Object {
  def data: ChartData = js.native
  def update(): Unit  = js.native
}

object ChartJS {
  trait Dataset extends js.Object {
    var data: js.UndefOr[js.Array[Double]]                     = js.undefined
    var borderWidth: js.UndefOr[Double]                        = js.undefined
    var backgroundColor: js.UndefOr[String | js.Array[String]] = js.undefined
    var label: js.UndefOr[String]                              = js.undefined
    var fill: js.UndefOr[Boolean]                              = js.undefined
    var borderColor: js.UndefOr[String]                        = js.undefined
  }

  trait ChartData extends js.Object {
    var labels: js.UndefOr[js.Array[String]]    = js.undefined
    var datasets: js.UndefOr[js.Array[Dataset]] = js.undefined
  }

  trait TimeOption extends js.Object {
    var unit: js.UndefOr[String] = js.undefined
  }

  trait Axe extends js.Object {
    var max: js.UndefOr[Double]          = js.undefined
    var beginAtZero: js.UndefOr[Boolean] = js.undefined
    var `type`: js.UndefOr[String]       = js.undefined
    var time: js.UndefOr[TimeOption]     = js.undefined
  }

  trait Ticks extends js.Object {}

  trait Scales extends js.Object {
    var x: js.UndefOr[Axe] = js.undefined
    var y: js.UndefOr[Axe] = js.undefined
  }

  trait Layout extends js.Object {
    var padding: js.UndefOr[Double] = js.undefined
  }

  trait Colors extends js.Object {
    var forceOverride: js.UndefOr[Boolean] = js.undefined
  }

  trait Plugins extends js.Object {
    var colors: js.UndefOr[Colors] = js.undefined
  }

  trait Options extends js.Object {
    var layout: js.UndefOr[Layout]   = js.undefined
    var scales: js.UndefOr[Scales]   = js.undefined
    var plugins: js.UndefOr[Plugins] = js.undefined
  }

  trait Configuration extends js.Object {
    var `type`: js.UndefOr[String]   = js.undefined
    var data: js.UndefOr[ChartData]  = js.undefined
    var options: js.UndefOr[Options] = js.undefined
  }
}
