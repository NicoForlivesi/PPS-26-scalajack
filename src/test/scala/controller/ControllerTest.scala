package controller

import controller.Controller.*
import cats.Show
import cats.effect.IO
import cats.effect.std.Console
import cats.effect.unsafe.implicits.global
import model.PlayerModule.Player
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

import java.nio.charset.Charset

class ControllerTest extends AnyFunSuite:

  val expectedPlayer: Player = Player("mario123", 100)

  def mockConsoleWith(readLineBehavior: () => String): Console[IO] = new Console[IO]:
    override def readLine: IO[String] = IO(readLineBehavior())
    override def readLineWithCharset(charset: Charset): IO[String] = readLine
    override def print[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def println[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def error[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def errorln[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit

  test("Method getPlayer should coordinate view methods to build a Player"):
    val simulatedInputs = Iterator("mario123", "100")
    given Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualPlayer: Player = getPlayer.unsafeRunSync()
    actualPlayer.name shouldEqual expectedPlayer.name
    actualPlayer.balance shouldEqual expectedPlayer.balance

  test("Method initializeHand")

