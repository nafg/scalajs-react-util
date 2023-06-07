package io.github.nafg.scalajs.react.util.partialrenderer

import japgolly.scalajs.react.Callback


class PartialSettableTests extends munit.FunSuite {
  class VarSnapshot[P, F](implicit partialityType: PartialityType[P, F]) {
    var value: Tentative[P, F] = Tentative.Partial(partialityType.default)

    def settable =
      PartialSettable[P, F](value) { f =>
        Callback {
          value = f(value)
        }
      }
  }

  test("PartialSettable.unzip") {
    val stateB = PartialityType.option[Int]
    val stateC = PartialityType.option[String]

    val partialityType = stateB.zip(stateC)
    val varSnapshot    = new VarSnapshot()(partialityType)

    val a: PartialSettable[(Option[Int], Option[String]), (Int, String)] =
      varSnapshot.settable

    a.setFullCB((10, "abc")).runNow()
    assertEquals(varSnapshot.value, Tentative.Full((10, "abc")))

    a.setPartialCB((None, None)).runNow()
    assertEquals(varSnapshot.value, Tentative.Partial((None, None), Some("Value is empty")))

    a.setPartialCB((Some(15), None)).runNow()
    assertEquals(varSnapshot.value, Tentative.Partial((Some(15), None), Some("Value is empty")))

    a.setPartialCB((None, Some("def"))).runNow()
    assertEquals(varSnapshot.value, Tentative.Partial((None, Some("def")), Some("Value is empty")))

    a.setPartialCB((Some(20), Some("xyz"))).runNow()
    assertEquals(varSnapshot.value, Tentative.Full((20, "xyz")))

    val unzipped = PartialSettable.unzip(a)(stateB, stateC)

    a.setPartialCB((None, None)).runNow()

    unzipped._1.setPartialCB(Some(1)).runNow()
    assertEquals(varSnapshot.value, Tentative.Partial((Some(1), None), Some("Value is empty")))

    unzipped._2.setPartialCB(Some("a")).runNow()
    assertEquals(varSnapshot.value, Tentative.Full((1, "a")))
  }
}
