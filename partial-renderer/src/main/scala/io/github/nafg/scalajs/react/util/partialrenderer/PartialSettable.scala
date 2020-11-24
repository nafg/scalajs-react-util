package io.github.nafg.scalajs.react.util.partialrenderer

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.extra.StateSnapshot
import io.github.nafg.scalajs.react.util.SnapshotUtils.Snapshot

import cats.implicits._
import monocle.function.fields
import monocle.{Iso, Lens}


case class PartialSettable[Partial, Full](partialityType: PartialityType[Partial, Full], value: Either[Partial, Full])
                                         (val modify: (Either[Partial, Full] => Either[Partial, Full]) => Callback) {
  def partialValue = value.fold(identity, partialityType.fullToPartial)
  def setCB(either: partialityType.E) = modify(_ => either)
  def setPartialCB(p: Partial) = setCB(partialityType.partialToEither(p))
  def setFullCB(full: Full) = setCB(Right(full))

  def modPartial(f: Partial => Partial) =
    modify(e => partialityType.partialToEither(f(partialityType.eitherToPartial(e))))

  def state: StateSnapshot[partialityType.E] =
    Snapshot(value)(setCB)

  def statePartial: StateSnapshot[Partial] =
    Snapshot(partialValue)(setPartialCB)

  def stateFull(implicit ev: Partial <:< Full): StateSnapshot[Full] =
    Snapshot(value.fold(ev, identity))(setFullCB)

  def zoomEither[P2, F2](pt2: PartialityType[P2, F2])(lens: Lens[partialityType.E, pt2.E]): PartialSettable[P2, F2] =
    PartialSettable(pt2, lens.get(value))(f => modify(lens.modify(f)))

  private def eitherLens[F2, P2](pt2: PartialityType[P2, F2])
                                (lensPartial: Lens[Partial, P2], lensFull: Lens[Full, F2]) =
    Lens[partialityType.E, pt2.E](_.bimap(lensPartial.get, lensFull.get)) {
      either2 =>
        either =>
          (either2, either) match {
            case (Left(p2), Left(partial))  => Left(lensPartial.set(p2)(partial))
            case (Left(p2), Right(full))    => Left(lensPartial.set(p2)(partialityType.fullToPartial(full)))
            case (Right(f2), Left(partial)) => Left(lensPartial.set(pt2.fullToPartial(f2))(partial))
            case (Right(f2), Right(full))   => Right(lensFull.set(f2)(full))
          }
    }

  def zoom[P2, F2](pt2: PartialityType[P2, F2])
                  (lensPartial: Lens[Partial, P2], lensFull: Lens[Full, F2]): PartialSettable[P2, F2] =
    zoomEither(pt2)(eitherLens(pt2)(lensPartial, lensFull))

  def xmapEither[P1, F1](pt2: PartialityType[P1, F1])
                        (iso: Iso[partialityType.E, pt2.E]): PartialSettable[P1, F1] =
    PartialSettable(pt2, iso.get(value))(f => modify(iso.modify(f)))

  def xmapFull[F1](iso: Iso[Full, F1]): PartialSettable[Partial, F1] =
    xmapEither(partialityType.xmapFull(iso.reverse))(iso.right)

  def xmapPartial[P1](iso: Iso[Partial, P1]): PartialSettable[P1, Full] =
    xmapEither(partialityType.xmapPartial(iso.reverse))(iso.left)
}
object PartialSettable {
  def unzip[P1, P2, F1, F2](settable: PartialSettable[(P1, P2), (F1, F2)])
                           (pt1: PartialityType[P1, F1],
                            pt2: PartialityType[P2, F2]): (PartialSettable[P1, F1], PartialSettable[P2, F2]) =
    (
      settable.zoom(pt1)(fields.first, fields.first),
      settable.zoom(pt2)(fields.second, fields.second)
    )
}
