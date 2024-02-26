package com.deliganli.mortgage.model

import scala.math.BigDecimal.RoundingMode

sealed trait Value

object Value {
  case class Flat(amount: Int)    extends Value
  case class Rate(amount: Double) extends Value

  extension (self: Value.Rate) def rate = self.amount / 100
}

case class Property(
  price: Value.Flat,
  appreciation: Value.Rate,
  costs: Value.Rate
)

case class Loan(
  downpayment: Value,
  interest: Value.Rate,
  term: Int
)

case class Rent(
  payment: Value.Flat,
  increase: Value.Rate
)

case class Opportunity(
  growth: Value.Rate,
  rent: Rent
)

case class Inquiry(
  currency: String,
  property: Property,
  loan: Loan,
  opportunity: Opportunity
)

val r = RoundingMode.HALF_EVEN

extension (x: Inquiry) {
  inline def downpayment = x.loan.downpayment match
    case Value.Flat(amount) => BigDecimal(amount).setScale(2, r)
    case v: Value.Rate      => BigDecimal(x.property.price.amount * v.rate).setScale(2, r)

  inline def principal           = x.property.price.amount - x.downpayment
  inline def installmentCount    = x.loan.term * 12
  inline def monthlyInterestRate = x.loan.interest.rate / 12

  inline def monthlyPayment = {
    val rOverN = Math.pow(1 + x.monthlyInterestRate, x.installmentCount)

    x.principal * ((x.monthlyInterestRate * rOverN) / (rOverN - 1))
  }

  inline def totalRepayment    = x.monthlyPayment * x.installmentCount
  inline def interestRepayment = (x.totalRepayment - x.principal).setScale(2, r)
}
