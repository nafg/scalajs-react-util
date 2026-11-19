package io.github.nafg.scalajs.react.util

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.util.{Success, Try}

import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
import japgolly.scalajs.react.Callback

class FutureValueRunQueryTests extends munit.FunSuite {

  /** Completes after the tasks already scheduled on the event loop, including the handlers of completed futures */
  private def afterPendingTasks: Future[Unit] = {
    val promise = Promise[Unit]()
    js.timers.setTimeout(50)(promise.success(()))
    promise.future
  }

  test("runQuery passes the result on when the cleanup hasn't run") {
    val promise = Promise[Int]()
    var results = List.empty[Try[Int]]
    FutureValue.runQuery(promise.future)(result => Callback(results ::= result)).runNow()
    promise.success(1)
    afterPendingTasks.map(_ => assertEquals(results, List(Success(1))))
  }

  test("runQuery drops the result of a query whose cleanup ran before it completed") {
    val superseded = Promise[Int]()
    val current    = Promise[Int]()
    var results    = List.empty[Try[Int]]

    val cleanup = FutureValue.runQuery(superseded.future)(result => Callback(results ::= result)).runNow()
    cleanup.runNow()
    FutureValue.runQuery(current.future)(result => Callback(results ::= result)).runNow()

    current.success(2)
    superseded.success(1)
    afterPendingTasks.map(_ => assertEquals(results, List(Success(2))))
  }
}
