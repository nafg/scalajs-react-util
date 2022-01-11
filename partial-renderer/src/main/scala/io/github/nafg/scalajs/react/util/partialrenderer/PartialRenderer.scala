package io.github.nafg.scalajs.react.util.partialrenderer

import japgolly.scalajs.react.vdom.VdomNode

import cats.kernel.Semigroup
import monocle.{Iso, Lens}


trait PartialRenderer[-Props, Partial, Full, +Out] { outer =>
  def partialityType: PartialityType[Partial, Full]
  def render(props: Props, in: PartialSettable[Partial, Full]): Out

  def xmapPartial[P1](iso: Iso[P1, Partial]): PartialRenderer[Props, P1, Full, Out] =
    new PartialRenderer[Props, P1, Full, Out] {
      override val partialityType = outer.partialityType.xmapPartial(iso)
      override def render(props: Props, settable: PartialSettable[P1, Full]) =
        outer.render(props, settable.xmapPartial(iso))
    }

  def xmapFull[F1](iso: Iso[F1, Full]): PartialRenderer[Props, Partial, F1, Out] =
    new PartialRenderer[Props, Partial, F1, Out] {
      override val partialityType = outer.partialityType.xmapFull(iso)
      override def render(props: Props, settable: PartialSettable[Partial, F1]) =
        outer.render(props, settable.xmapFull(iso))
    }

  def xmapBoth[P1, F1](isoPartial: Iso[P1, Partial], isoFull: Iso[F1, Full]): PartialRenderer[Props, P1, F1, Out] =
    xmapPartial(isoPartial).xmapFull(isoFull)

  def andThen[Out2](f: PartialSettable[Partial, Full] => Out => Out2) =
    new PartialRenderer[Props, Partial, Full, Out2] {
      override def partialityType = outer.partialityType
      override def render(props: Props, settable: PartialSettable[Partial, Full]) =
        f(settable)(outer.render(props, settable))
    }

  def mapOut[Out2](f: Out => Out2): PartialRenderer[Props, Partial, Full, Out2] = andThen(_ => f)

  def zip[Props2 <: Props, P2, F2, O2 >: Out : Semigroup](that: PartialRenderer[Props2, P2, F2, O2]): PartialRenderer[
    Props2,
    (Partial, P2),
    (Full, F2),
    O2
  ] =
    new PartialRenderer[Props2, (Partial, P2), (Full, F2), O2] {
      override val partialityType = outer.partialityType.zip(that.partialityType)
      override def render(props: Props2, settable: PartialSettable[(Partial, P2), (Full, F2)]) = {
        val (settable1, settable2) = PartialSettable.unzip(settable)(outer.partialityType, that.partialityType)
        Semigroup[O2].combine(
          outer.render(props, settable1),
          that.render(props, settable2)
        )
      }
    }

  def unzoomFull[F1](pt1: PartialityType[Partial, F1])
                    (get: F1 => Full)
                    (set: Full => F1 => F1): PartialRenderer[Props, Partial, F1, Out] =
    new PartialRenderer[Props, Partial, F1, Out] {
      override def partialityType = pt1
      override def render(props: Props, settable: PartialSettable[Partial, F1]) =
        outer.render(props, settable.zoom(outer.partialityType)(Iso.id[Partial], Lens(get)(set)))
    }

  def unzoomEither[P1, F1](pt1: PartialityType[P1, F1])
                          (lens: Lens[Either[P1, F1], Either[Partial, Full]]): PartialRenderer[Props, P1, F1, Out] =
    new PartialRenderer[Props, P1, F1, Out] {
      override def partialityType = pt1
      override def render(props: Props, settable: PartialSettable[P1, F1]) =
        outer.render(props, settable.zoomEither(outer.partialityType)(lens))
    }
}

object PartialRenderer {
  type Vdom[-Props, P, F] = PartialRenderer[Props, P, F, VdomNode]
  def noProps[Partial, Full, Out](pt: PartialityType[Partial, Full])
                                 (renderF: PartialSettable[Partial, Full] => Out) =
    new PartialRenderer[Any, Partial, Full, Out] {
      override def partialityType = pt
      override def render(props: Any, settable: PartialSettable[Partial, Full]) = renderF(settable)
    }
}
