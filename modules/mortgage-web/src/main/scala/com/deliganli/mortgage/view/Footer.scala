package com.deliganli.mortgage
package view

import calico.*
import calico.html.io.{*, given}
import calico.syntax.*
import cats.effect.IO
import cats.implicits.*

object Footer {

  def apply() = div(
    cls := "container mt-auto ",
    footerTag(
      cls := "d-flex flex-wrap justify-content-between align-items-center py-3 my-4 border-top",
      div(
        cls := "col-md-4 d-flex align-items-center",
        span(
          cls := "mb-3 mb-md-0 text-body-secondary",
          "© 2023 ",
          a(
            cls  := "text-body-secondary",
            href := "https://deliganli.com",
            "Sait Sami Kocatas"
          )
        )
      ),
      ul(
        cls := "nav col-md-4 justify-content-end list-unstyled d-flex",
        li(
          cls := "ms-3",
          a(
            cls  := "text-body-secondary",
            href := "https://github.com/Deliganli/pekmez",
            "GitHub"
          )
        )
      )
    )
  )
}
