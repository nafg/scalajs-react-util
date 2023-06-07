package io.github.nafg.scalajs.react.util.partialrenderer

import scala.util.Try

import monocle.Iso


class PartialityTypeTests extends munit.FunSuite {
  test("PartialStateType.full") {
    val a = PartialityType.full("hello")

    assert(a.default == "hello")
    assert(a.fullToPartial("hello").contains("hello"))
    assert(a.partialToFull("hello").contains("hello"))
  }

  test("PartialStateType.option") {
    val a = PartialityType.option[String]

    assert(a.default.isEmpty)
    assert(a.fullToPartial("hello").contains("hello"))
    assert(a.partialToFull(Some("hello")).contains("hello"))
  }

  test("PartialStateType#zip") {
    val a = PartialityType.option[Int].zip(PartialityType.option[Int])

    assertEquals(a.default, (None, None))
    assertEquals(a.fullToPartial((1, 2)), (Some(1), Some(2)))
    assertEquals(a.partialToFull((Some(1), Some(2))), Right((1, 2)))
    assert(a.partialToFull((None, Some(2))).isLeft)
    assert(a.partialToFull((Some(1), None)).isLeft)
  }

  test("PartialStateType#xmapFull") {
    val a = PartialityType.full(false)
    val b = a.xmapFull(Iso[Option[Unit], Boolean](_.isDefined)(if (_) Some(()) else None))

    assertEquals(b.default, false)
    assertEquals(b.fullToPartial(Some(())), true)
    assertEquals(b.fullToPartial(None), false)
    assertEquals(b.partialToFull(true), Right(Some(())))
    assertEquals(b.partialToFull(false), Right(None))

    val c = PartialityType("")(s => Try(s.toInt).toEither.left.map(_.toString))(_.toString)
    val d = c.xmapFull(Iso[Integer, Int](_.intValue)(Integer.valueOf))

    assertEquals(d.default, "")
    assertEquals(d.fullToPartial(1), "1")
    assertEquals(d.partialToFull("1"), Right(1: Integer))
    assert(d.partialToFull("a").isLeft)
  }
}
