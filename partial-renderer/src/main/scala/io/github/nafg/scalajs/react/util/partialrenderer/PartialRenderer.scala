package io.github.nafg.scalajs.react.util.partialrenderer

import japgolly.scalajs.react.vdom.VdomNode

import cats.kernel.Semigroup
import monocle.{Iso, Lens}

abstract class PartialRenderer[-Props, Partial, Full, +Out](implicit
  val partialityType: PartialityType[Partial, Full]
) { self =>

  def render(props: Props, in: PartialSettable[Partial, Full]): Out

  private def modSettable[P1, F1](
    f: PartialSettable[P1, F1] => PartialSettable[Partial, Full]
  )(implicit partialityType2: PartialityType[P1, F1]) =
    new PartialRenderer[Props, P1, F1, Out] {
      override def render(props: Props, settable: PartialSettable[P1, F1]) =
        self.render(props, f(settable))
    }

  def xmapPartial[P1](iso: Iso[P1, Partial]): PartialRenderer[Props, P1, Full, Out] =
    modSettable[P1, Full](_.xmapPartial(iso))(partialityType.xmapPartial(iso))

  def xmapFull[F1](iso: Iso[F1, Full]): PartialRenderer[Props, Partial, F1, Out] =
    modSettable[Partial, F1](_.xmapFull(iso))(partialityType.xmapFull(iso))

  def xmapBoth[P1, F1](isoPartial: Iso[P1, Partial], isoFull: Iso[F1, Full]): PartialRenderer[Props, P1, F1, Out] =
    xmapPartial(isoPartial).xmapFull(isoFull)

  def unzoomFull[F1](get: F1 => Full)(set: Full => F1 => F1)(implicit
    partialityType2: PartialityType[Partial, F1]
  ): PartialRenderer[Props, Partial, F1, Out] =
    modSettable[Partial, F1](_.zoom(Iso.id[Partial], Lens(get)(set)))

  def unzoom[P1, F1](lens: Lens[Tentative[P1, F1], Tentative[Partial, Full]])(implicit
    partialityType2: PartialityType[P1, F1]
  ): PartialRenderer[Props, P1, F1, Out] =
    modSettable[P1, F1](_.zoom(lens))

  def andThen[Out2](f: PartialSettable[Partial, Full] => Out => Out2) =
    new PartialRenderer[Props, Partial, Full, Out2] {
      override def render(props: Props, settable: PartialSettable[Partial, Full]) =
        f(settable)(self.render(props, settable))
    }

  def mapOut[Out2](f: Out => Out2): PartialRenderer[Props, Partial, Full, Out2] = andThen(_ => f)

  def zip[Props2 <: Props, P2, F2, O2 >: Out: Semigroup](
    other: PartialRenderer[Props2, P2, F2, O2]
  ): PartialRenderer[Props2, (Partial, P2), (Full, F2), O2] = {
    val zippedPartialityType = self.partialityType.zip(other.partialityType)
    new PartialRenderer[Props2, (Partial, P2), (Full, F2), O2]()(zippedPartialityType) {
      override def render(props: Props2, settable: PartialSettable[(Partial, P2), (Full, F2)]) = {
        val (thisSettable, thatSettable) =
          PartialSettable.unzip(settable)(self.partialityType, other.partialityType)
        Semigroup[O2].combine(self.render(props, thisSettable), other.render(props, thatSettable))
      }
    }
  }
}

object PartialRenderer {
  type Vdom[-Props, P, F] = PartialRenderer[Props, P, F, VdomNode]

  def noProps[Partial, Full, Out](pt: PartialityType[Partial, Full])(renderF: PartialSettable[Partial, Full] => Out) =
    new PartialRenderer[Any, Partial, Full, Out]()(pt) {
      override def render(props: Any, settable: PartialSettable[Partial, Full]) = renderF(settable)
    }
}
