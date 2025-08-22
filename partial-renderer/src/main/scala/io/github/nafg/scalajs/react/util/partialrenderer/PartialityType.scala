package io.github.nafg.scalajs.react.util.partialrenderer

import scala.annotation.unused

import cats.implicits.catsSyntaxTuple2Semigroupal
import monocle.Iso

/** Law: pst.partialToFull(pst.fullToPartial(full)) == Right(full)
  *
  * Law: pst.partialToFull(partial).toOption.map(pst.fullToPartial) inSet (None, Some(partial))
  */
case class PartialityType[Partial, Full](
  default: Partial,
  partialToFull: Partial => Either[String, Full],
  fullToPartial: Full => Partial
) {
  private type TentativeType = Tentative[Partial, Full]

  lazy val iso: Iso[TentativeType, Partial] = Tentative.isoTentativePartialValue(this)

  def normalize(tentative: TentativeType): TentativeType = iso.modify(identity)(tentative)

  def xmapPartial[P2](iso: Iso[P2, Partial]): PartialityType[P2, Full] =
    PartialityType[P2, Full](iso.reverseGet(default))(partialToFull.compose(iso.get))(
      fullToPartial.andThen(iso.reverseGet)
    )

  def xmapFull[F2](iso: Iso[F2, Full]): PartialityType[Partial, F2] =
    PartialityType[Partial, F2](default)(partialToFull(_).map(iso.reverseGet))(f2 => fullToPartial(iso.get(f2)))

  def zip[P2, F2](that: PartialityType[P2, F2]): PartialityType[(Partial, P2), (Full, F2)] =
    PartialityType((this.default, that.default)) { case (p1, p2) =>
      (this.partialToFull(p1), that.partialToFull(p2)).tupled
    } { case (f1, f2) =>
      (this.fullToPartial(f1), that.fullToPartial(f2))
    }

  def withDefaultAsNone: PartialityType[Partial, Option[Full]] =
    PartialityType[Partial, Option[Full]](default) {
      case `default` => Right(None)
      case other     => partialToFull(other).map(Some(_))
    }(_.fold(default)(fullToPartial))
}
object PartialityType {
  def apply[Partial, Full](default: Partial)(
    partialToFull: Partial => Either[String, Full]
  )(fullToPartial: Full => Partial, @unused dummy: Null = null): PartialityType[Partial, Full] =
    new PartialityType(default, partialToFull, fullToPartial)

  implicit def option[Full]: PartialityType[Option[Full], Full] =
    PartialityType(Option.empty[Full])(_.toRight("Value is empty"))(Some(_))

  def full[Full](default: Full): PartialityType[Full, Full] =
    PartialityType(default)(Right(_))(identity)

  implicit lazy val unit: PartialityType[Unit, Unit] = full(())
}
