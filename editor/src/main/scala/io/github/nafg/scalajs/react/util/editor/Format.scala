package io.github.nafg.scalajs.react.util.editor

import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.{LocalDate, LocalDateTime, LocalTime}

import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._

import cats.data.Validated
import monocle.Prism

import cats.implicits._


case class Format[A](format: A => String, parse: String => Validated[TagMod, A])

object Format {
  def apply[A](parse: String => Validated[TagMod, A])(format: A => String, dummy: Null = null): Format[A] =
    new Format(format, parse)

  def fromPrism[A](prism: Prism[String, A])(error: => TagMod) =
    new Format[A](prism.reverseGet, str => Validated.fromOption(prism.getOption(str), error))

  private def message(exception: Exception): TagMod = exception.getMessage

  implicit def maybe[A](implicit underlying: Format[A]): Format[Option[A]] =
    apply(s => if (s.isEmpty) None.valid else underlying.parse(s).map(Some(_)))(_.fold("")(underlying.format))

  implicit val string: Format[String] =
    apply(Validated.valid)(identity)
  implicit val int: Format[Int] =
    apply(str => Validated.catchOnly[NumberFormatException](str.toInt).leftMap(message))(_.toString)
  implicit val double: Format[Double] =
    apply(str => Validated.catchOnly[NumberFormatException](str.toDouble).leftMap(message))(_.toString)
  implicit val bigDecimal: Format[BigDecimal] =
    apply(str => Validated.catchOnly[NumberFormatException](BigDecimal(str)).leftMap(message))(_.toString)

  def localTime(dtf: DateTimeFormatter): Format[LocalTime] =
    apply(str => Validated.catchOnly[DateTimeParseException](LocalTime.parse(str, dtf)).leftMap(message))(dtf.format)
  def localDate(dtf: DateTimeFormatter): Format[LocalDate] =
    apply(str => Validated.catchOnly[DateTimeParseException](LocalDate.parse(str, dtf)).leftMap(message))(dtf.format)
  def localDateTime(dtf: DateTimeFormatter): Format[LocalDateTime] =
    apply(str => Validated.catchOnly[DateTimeParseException](LocalDateTime.parse(str, dtf)).leftMap(message))(dtf.format)
}
