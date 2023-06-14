package io.github.nafg.scalajs.react.util

import scala.util.control.NoStackTrace

import japgolly.scalajs.react.callback.AsyncCallback
import japgolly.scalajs.react.{CallbackOption, CallbackTo}


object confirm {
  case object ConfirmCanceled extends RuntimeException with NoStackTrace

  def apply[A](message: String)(callback: CallbackTo[A]): CallbackOption[A] =
    CallbackTo.confirm(message).requireCBO >> callback.toCBO

  def async(message: String) =
    CallbackTo.confirm(message).async.flatMap {
      case true  => AsyncCallback.unit
      case false => AsyncCallback.throwException(ConfirmCanceled)
    }
}
