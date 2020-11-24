package io.github.nafg.scalajs.react.util.partialrenderer

import japgolly.scalajs.react.Callback


class PartialSettableTests extends munit.FunSuite {
  class VarSnapshot[A](init: A) {
    var value = init
    def settable[F](pt: PartialityType[A, F]) =
      PartialSettable(pt, Left(value))(f => Callback(this.value = pt.eitherToPartial(f(Left(this.value)))))
  }

  test("PartialSettable.unzip") {
    val varSnapshot = new VarSnapshot((Option.empty[Int], Option.empty[String]))
    val stateB = PartialityType.option[Int]
    val stateC = PartialityType.option[String]
    val a = varSnapshot.settable(stateB.zip(stateC))

    a.setFullCB((10, "abc")).runNow()
    assertEquals(varSnapshot.value, (Some(10), Some("abc")))

    a.setPartialCB((None, None)).runNow()
    assertEquals(varSnapshot.value, (None, None))

    a.setPartialCB((Some(15), None)).runNow()
    assertEquals(varSnapshot.value, (Some(15), None))

    a.setPartialCB((None, Some("def"))).runNow()
    assertEquals(varSnapshot.value, (None, Some("def")))

    a.setPartialCB((Some(20), Some("xyz"))).runNow()
    assertEquals(varSnapshot.value, (Some(20), Some("xyz")))

    val unzipped = PartialSettable.unzip(a)(stateB, stateC)

    a.setPartialCB((None, None)).runNow()

    unzipped._1.setPartialCB(Some(1)).runNow()
    assertEquals(varSnapshot.value, (Some(1), None))

    unzipped._2.setPartialCB(Some("a")).runNow()
    assertEquals(varSnapshot.value, (Some(1), Some("a")))
  }
}
