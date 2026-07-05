package view

import cats.Show
import cats.effect.IO
import cats.effect.std.Console
import cats.effect.unsafe.implicits.global
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*
import view.View.*

import java.nio.charset.Charset

class ViewTest extends AnyFunSuite:

  val expectedBalance: Int = 100

  test("The initial balance of the player should equal what is simulated in standard input"):

    given mockConsole: Console[IO] with
      override def readLine: IO[String] = IO.pure(expectedBalance.toString)
      override def readLineWithCharset(charset: Charset): IO[String] = readLine
      override def print[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
      override def println[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
      override def error[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
      override def errorln[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit

    val method: IO[Int] = getInitialBalance
    val actualBalance: Int = method.unsafeRunSync()

    actualBalance shouldEqual expectedBalance

  test("The view should retry until a valid positive integer is provided"):

    val simulatedInputs = Iterator("error", "-50", expectedBalance.toString)

    given mockConsole: Console[IO] with
      override def readLine: IO[String] = IO(simulatedInputs.next())
      override def readLineWithCharset(charset: Charset): IO[String] = readLine
      override def print[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
      override def println[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
      override def error[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
      override def errorln[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit

    val method: IO[Int] = getInitialBalance
    val actualBalance: Int = method.unsafeRunSync()

    actualBalance shouldEqual expectedBalance