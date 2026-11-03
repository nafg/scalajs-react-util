package io.github.nafg.scalajs.react.util.partialrenderer

import japgolly.scalajs.react.{Callback, Reusability, Reusable}

import monocle.Focus

class PartialSettableTests extends munit.FunSuite {
  class VarSnapshot[P, F](implicit partialityType: PartialityType[P, F]) {
    var value: Tentative[P, F] = Tentative.Partial(partialityType.default)

    private val modifier =
      Reusable.byRef { (f: Tentative[P, F] => Tentative[P, F]) =>
        Callback {
          value = f(value)
        }
      }

    def settable = PartialSettable[P, F](value)(modifier)
  }

  private def reusable[A](x: A, y: A)(implicit r: Reusability[A]) = r.test(x, y)

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

  test("PartialityType.option is one instance") {
    assert(PartialityType.option[Int] eq PartialityType.option[Int])
    assert(PartialityType.option[Int].asInstanceOf[AnyRef] eq PartialityType.option[String])
  }

  test("PartialSettable reusability compares the settable and the partialityType") {
    val varSnapshot = new VarSnapshot[Option[Int], Int]
    assert(reusable(varSnapshot.settable, varSnapshot.settable))

    val other = new VarSnapshot[Option[Int], Int]
    assert(!reusable(varSnapshot.settable, other.settable), "equal values, different modifiers")

    varSnapshot.settable.setFullCB(1).runNow()
    val before = varSnapshot.settable
    varSnapshot.settable.setFullCB(2).runNow()
    assert(!reusable(before, varSnapshot.settable), "different values")

    val settable    = varSnapshot.settable
    val samePtAgain = PartialSettable(settable.settable)(PartialityType.option[Int])
    val differentPt = PartialSettable(settable.settable)(PartialityType.option[Int].xmapFull(monocle.Iso.id[Int]))
    assert(reusable(settable, samePtAgain))
    assert(!reusable(settable, differentPt), "different partialityType instances")
  }

  test("PartialSettable.unzip children stay reusable while the root modifier does") {
    implicit val partialityType: PartialityType[(Option[Int], Option[String]), (Int, String)] =
      PartialityType.option[Int].zip(PartialityType.option[String])
    val varSnapshot                                                                           = new VarSnapshot()

    def unzip = PartialSettable.unzip(varSnapshot.settable)(PartialityType.option[Int], PartialityType.option[String])

    val (first1, second1) = unzip
    val (first2, second2) = unzip
    assert(reusable(first1, first2))
    assert(reusable(second1, second2))

    // Two children of one parent each modify the latest state, not the value the parent captured
    first1.setPartialCB(Some(1)).runNow()
    second1.setPartialCB(Some("a")).runNow()
    assertEquals(varSnapshot.value, Tentative.Full((1, "a")))

    val (first3, _) = unzip
    assert(!reusable(first1, first3), "the value changed")

    val otherRoot  = new VarSnapshot()
    otherRoot.value = varSnapshot.value
    val (other, _) =
      PartialSettable.unzip(otherRoot.settable)(PartialityType.option[Int], PartialityType.option[String])
    assert(!reusable(unzip._1, other), "equal values, different root modifiers")
  }

  test("PartialSettable.zoom(lensPartial, lensFull) is keyed on the lenses") {
    case class P(a: Option[Int], b: Option[Int])
    case class F(a: Int, b: Int)
    implicit val partialityType: PartialityType[P, F] =
      PartialityType[P, F](P(None, None))(p => for (a <- p.a.toRight("a"); b <- p.b.toRight("b")) yield F(a, b))(f =>
        P(Some(f.a), Some(f.b))
      )
    val varSnapshot                                   = new VarSnapshot()
    val lensPartial                                   = Focus[P](_.a)
    val lensFull                                      = Focus[F](_.a)

    assert(reusable(varSnapshot.settable.zoom(lensPartial, lensFull), varSnapshot.settable.zoom(lensPartial, lensFull)))
    assert(
      !reusable(
        varSnapshot.settable.zoom(Focus[P](_.a), Focus[F](_.a)),
        varSnapshot.settable.zoom(Focus[P](_.a), Focus[F](_.a))
      ),
      "lenses created per call"
    )
  }
}
