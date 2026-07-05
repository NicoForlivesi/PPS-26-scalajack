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

  val expectedPlayerID = "mario123"
  val expectedBalance: Int = 100

  def mockConsoleWith(readLineBehavior: IO[String]): Console[IO] = new Console[IO]:
    override def readLine: IO[String] = readLineBehavior
    override def readLineWithCharset(charset: Charset): IO[String] = readLine
    override def print[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def println[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def error[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def errorln[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit

  test("The ID of the player should equal what is simulated in standard input"):
    given Console[IO] = mockConsoleWith(IO.pure(expectedPlayerID))
    val actualPlayerID: String = getPlayerID.unsafeRunSync()
    actualPlayerID shouldEqual expectedPlayerID

  test("The initial balance of the player should equal what is simulated in standard input"):
    given Console[IO] = mockConsoleWith(IO.pure(expectedBalance.toString))
    val actualBalance: Int = getInitialBalance.unsafeRunSync()
    actualBalance shouldEqual expectedBalance

  test("The view should retry until a valid positive integer is provided"):
    val simulatedInputs = Iterator("error", "-50", expectedBalance.toString)
    given Console[IO] = mockConsoleWith(IO(simulatedInputs.next()))// We use IO(...) instead of IO.pure to suspend the evaluation of iterator.next() until there is a new execution of readLine()
    val actualBalance: Int = getInitialBalance.unsafeRunSync()
    actualBalance shouldEqual expectedBalance