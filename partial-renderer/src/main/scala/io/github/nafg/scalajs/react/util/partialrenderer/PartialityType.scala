package io.github.nafg.scalajs.react.util.partialrenderer

import cats.implicits.*
import monocle.Iso


/**
 * Law: pst.partialToFull(pst.fullToPartial(full)) == Some(full)
 *
 * Law: pst.partialToFull(partial).map(pst.fullToPartial) inSet (None, Some(partial))
 */
case class PartialityType[Partial, Full](default: Partial)
                                        (val partialToFull: Partial => Option[Full])
                                        (val fullToPartial: Full => Partial) {
  type E = Either[Partial, Full]

  def partialToEither(partial: Partial): E = partialToFull(partial).toRight(partial)

  def eitherToPartial(either: E): Partial = either.map(fullToPartial).merge

  lazy val iso = Iso(eitherToPartial)(partialToEither)

  def xmapPartial[P2](iso: Iso[P2, Partial]): PartialityType[P2, Full] =
    PartialityType[P2, Full](iso.reverseGet(default))(p => partialToFull(iso.get(p))) { f =>
      iso.reverseGet(fullToPartial(f))
    }

  def xmapFull[F2](iso: Iso[F2, Full]): PartialityType[Partial, F2] =
    PartialityType[Partial, F2](default)(partialToFull(_).map(iso.reverseGet))(f2 => fullToPartial(iso.get(f2)))

  def zip[P2, F2](that: PartialityType[P2, F2]): PartialityType[(Partial, P2), (Full, F2)] =
    PartialityType((this.default, that.default)) { case (p1, p2) =>
      (this.partialToFull(p1), that.partialToFull(p2)).tupled
    } { case (f1, f2) =>
      (this.fullToPartial(f1), that.fullToPartial(f2))
    }
}
object PartialityType {
  def option[Full]: PartialityType[Option[Full], Full] =
    PartialityType(Option.empty[Full])(identity)(Some(_))

  def full[Full](default: Full): PartialityType[Full, Full] =
    PartialityType(default)(Some(_))(identity)

  lazy val unit = full(())
}
