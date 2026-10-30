package io.github.nafg.scalajs.react.util.partialrenderer

import org.scalajs.dom
import japgolly.scalajs.react.{Callback, Reusability, Reusable}
import japgolly.scalajs.react.ReactMonocle.MonocleReactExt_StateSnapshot
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.hooks.CustomHook

import monocle.{Focus, Iso, Lens}

case class PartialSettable[Partial, Full](settable: Settable[Tentative[Partial, Full]])(implicit
  val partialityType: PartialityType[Partial, Full]
) {
  if (!scalajs.LinkingInfo.productionMode) {
    val normalized = partialityType.normalize(value)
    if (normalized != value) {
      dom.console.error(s"\n  $value\nnormalizes to\n  $normalized")
    }
  }

  def value: Tentative[Partial, Full] = settable.value

  def error = value.error

  def partialValue: Partial = value.partialValue

  /** Checks if the current state is in a `Full` state.
    *
    * @return
    *   true if the value is in a full state, false otherwise.
    */
  def isFull = value.isFull

  private def setCB(tentative: Tentative[Partial, Full]): Callback = settable.set(tentative)

  def setPartialCB(p: Partial) = setCB(Tentative(p))

  def setFullCB(full: Full) = setCB(Tentative.Full(full))

  def modify = settable.modify _

  def modPartial(f: Partial => Partial) = modify(partialityType.iso.modify(f))

  def state: StateSnapshot[Tentative[Partial, Full]] = settable.toStateSnapshot

  def statePartial: StateSnapshot[Partial] = state.xmapStateL(partialityType.iso)

  /** Zooms through `lens`, normalizing the result with `partialityType2`.
    *
    * @param key
    *   Everything `lens` depends on. `partialityType2` is added to it.
    */
  private def zoomKeyed[P2, F2](lens: Lens[Tentative[Partial, Full], Tentative[P2, F2]], key: Reusable[Any])(implicit
    partialityType2: PartialityType[P2, F2]
  ): PartialSettable[P2, F2] = {
    val normalizingLens = lens.andThen(Iso.involuted(partialityType2.normalize))
    PartialSettable(
      settable.derive(normalizingLens.get(value), key *> Reusable.byRef(partialityType2))(normalizingLens.modify)
    )
  }

  /** The child's modifier is reusable while this one is and `lens` and `partialityType2` are the same instances. */
  def zoom[P2, F2](lens: Lens[Tentative[Partial, Full], Tentative[P2, F2]])(implicit
    partialityType2: PartialityType[P2, F2]
  ): PartialSettable[P2, F2] =
    zoomKeyed(lens, Reusable.byRef(lens))

  def zoom[P2, F2](get: Tentative[Partial, Full] => Tentative[P2, F2])(
    set: Tentative[P2, F2] => Tentative[Partial, Full] => Tentative[Partial, Full]
  )(implicit partialityType2: PartialityType[P2, F2]): PartialSettable[P2, F2] =
    PartialSettable(settable.zoom(get)(set))

  /** The child's modifier is reusable while this one is and `lensPartial`, `lensFull`, this `partialityType` and
    * `partialityType2` are the same instances.
    */
  def zoom[P2, F2](lensPartial: Lens[Partial, P2], lensFull: Lens[Full, F2])(implicit
    partialityType2: PartialityType[P2, F2]
  ): PartialSettable[P2, F2] =
    zoomKeyed(
      Tentative.lensTentative(lensPartial, lensFull),
      Reusable.byRef(lensPartial).tuple(Reusable.byRef(lensFull)) *> Reusable.byRef(partialityType)
    )

  def xmap[P2, F2](get: Tentative[Partial, Full] => Tentative[P2, F2])(
    reverseGet: Tentative[P2, F2] => Tentative[Partial, Full]
  )(implicit partialityType2: PartialityType[P2, F2]): PartialSettable[P2, F2] =
    PartialSettable(settable.xmap(get)(reverseGet))

  def xmapFull[F1](iso: Iso[Full, F1]): PartialSettable[Partial, F1] =
    xmap(_.mapFull(iso.get))(_.mapFull(iso.reverseGet))(partialityType.xmapFull(iso.reverse))

  def xmapPartial[P1](iso: Iso[Partial, P1]): PartialSettable[P1, Full] =
    xmap(_.mapPartial(iso.get))(_.mapPartial(iso.reverseGet))(partialityType.xmapPartial(iso.reverse))
}
object PartialSettable {

  /** Compares the settables, and the `partialityType`s by reference. */
  implicit def reusability[Partial, Full](implicit
    r: Reusability[Settable[Tentative[Partial, Full]]]
  ): Reusability[PartialSettable[Partial, Full]] =
    Reusability((x, y) => (x.partialityType eq y.partialityType) && r.test(x.settable, y.settable))

  def apply[Partial, Full](value: Tentative[Partial, Full])(
    modifier: Reusable[(Tentative[Partial, Full] => Tentative[Partial, Full]) => Callback]
  )(implicit partialityType: PartialityType[Partial, Full]) =
    new PartialSettable(Settable(partialityType.normalize(value))(modifier))

  // One instance each, so that zooms through them stay reusable
  private val firstAny                      = Focus[(Any, Any)](_._1)
  private val secondAny                     = Focus[(Any, Any)](_._2)
  private def first[A, B]: Lens[(A, B), A]  = firstAny.asInstanceOf[Lens[(A, B), A]]
  private def second[A, B]: Lens[(A, B), B] = secondAny.asInstanceOf[Lens[(A, B), B]]
  def unzip[P1, P2, F1, F2](
    settable: PartialSettable[(P1, P2), (F1, F2)]
  )(pt1: PartialityType[P1, F1], pt2: PartialityType[P2, F2]): (PartialSettable[P1, F1], PartialSettable[P2, F2]) =
    (settable.zoom(first, first)(pt1), settable.zoom(second, second)(pt2))

  def usePartialSettable[P, F](
    initial: Tentative[P, F]
  )(implicit partialityType: PartialityType[P, F]): CustomHook[Unit, PartialSettable[P, F]] =
    Settable
      .useSettable(initial)
      .map(PartialSettable(_))
}
