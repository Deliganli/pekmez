package com.deliganli.mortgage
package view

import calico.*
import calico.html.io.{*, given}
import calico.syntax.*
import cats.effect.*
import cats.effect.kernel.Resource
import cats.implicits.*
import fs2.dom.*

import cats.implicits.*

object Header {

  def apply() =
    headerTag(
      cls := "navbar navbar-expand-lg bg-primary bd-navbar sticky-top",
      navTag(
        cls := "container-md bd-gutter flex-wrap flex-lg-nowrap",
        a(
          cls  := "navbar-brand",
          href := "#",
          "Mortgage Calculator"
        ),
        div(
          cls := "offcanvas-lg offcanvas-end flex-grow-1"
        )
      )
    )
}
