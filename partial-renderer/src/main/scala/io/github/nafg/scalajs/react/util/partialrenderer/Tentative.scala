package io.github.nafg.scalajs.react.util.partialrenderer

import monocle.{Iso, Lens}


sealed trait Tentative[+P, +F] {
  def error: Option[String]
  def mapFull[F1 >: F, F2](f: F1 => F2): Tentative[P, F2]
  def mapPartial[P1 >: P, P2](f: P1 => P2): Tentative[P2, F]
  def fold[A](onPartial: P => A, onFull: F => A): A
  def partialValue[P1 >: P, F1 >: F](implicit partialityType: PartialityType[P1, F1]): P1
}

object Tentative {
  case class Partial[+P](partial: P, override val error: Option[String] = None) extends Tentative[P, Nothing] {
    override def toString = s"Tentative.Partial($partial, $error)"
    override def mapPartial[P1 >: P, P2](f: P1 => P2) = Partial(f(partial), error)
    override def mapFull[F1 >: Nothing, F2](f: F1 => F2) = this
    override def fold[A](onPartial: P => A, onFull: Nothing => A) = onPartial(partial)
    override def partialValue[P1 >: P, F1 >: Nothing](implicit partialityType: PartialityType[P1, F1]): P1 = partial
  }

  case class Full[+F](full: F) extends Tentative[Nothing, F] {
    override def toString = s"Tentative.Full($full)"
    override def error: Option[String] = None
    override def mapPartial[P1 >: Nothing, P2](f: P1 => P2) = this
    override def mapFull[F1 >: F, F2](f: F1 => F2) = Full(f(full))
    override def fold[A](onPartial: Nothing => A, onFull: F => A) = onFull(full)
    override def partialValue[P1 >: Nothing, F1 >: F](implicit partialityType: PartialityType[P1, F1]): P1 =
      partialityType.fullToPartial(full)
  }

  def fromOption[P, F](option: Option[F], partial: => P): Tentative[P, F] =
    option.fold[Tentative[P, F]](Partial(partial))(Full(_))

  def apply[P, F](partial: P)(implicit partialityType: PartialityType[P, F]): Tentative[P, F] =
    partialityType.partialToFull(partial) match {
      case Right(full) => Full(full)
      case Left(error) => Partial(partial, Some(error))
    }

  def default[P, F](implicit partialityType: PartialityType[P, F]): Tentative[P, F] =
    Tentative(partialityType.default)

  def isoTentativePartialValue[P, F](implicit partialityType: PartialityType[P, F]) =
    Iso[Tentative[P, F], P](_.partialValue)(Tentative(_))

  def lensTentative[P1, F1, P2, F2](lensPartial: Lens[P1, P2], lensFull: Lens[F1, F2])
                                   (implicit partialityTypeOuter: PartialityType[P1, F1],
                                    partialityTypeInner: PartialityType[P2, F2]
                                   ): Lens[Tentative[P1, F1], Tentative[P2, F2]] = {
    def replace(partialOuter: P1, partialInner: P2) =
      Tentative(lensPartial.replace(partialInner)(partialOuter))

    Lens[Tentative[P1, F1], Tentative[P2, F2]](_.mapPartial(lensPartial.get).mapFull(lensFull.get)) {
      case Full(fullInner)          => {
        case Full(fullOuter)          => Full(lensFull.replace(fullInner)(fullOuter))
        case Partial(partialOuter, _) => replace(partialOuter, partialityTypeInner.fullToPartial(fullInner))
      }
      case Partial(partialInner, _) => {
        case Partial(p1, _) => replace(p1, partialInner)
        case Full(f)        => replace(partialityTypeOuter.fullToPartial(f), partialInner)
      }
    }
  }
}
