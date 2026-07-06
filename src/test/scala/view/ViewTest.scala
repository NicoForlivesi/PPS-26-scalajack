package view

import cats.Show
import cats.effect.IO
import cats.effect.std.Console
import cats.effect.unsafe.implicits.global
import model.PlayerModule.Player
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*
import view.View.*

import java.nio.charset.Charset

class ViewTest extends AnyFunSuite:

  val expectedPlayerID = "mario123"
  val expectedBalance: Int = 100

  def mockConsoleWith(readLineBehavior: () => String): Console[IO] = new Console[IO]:
    override def readLine: IO[String] = IO(readLineBehavior())
    override def readLineWithCharset(charset: Charset): IO[String] = readLine
    override def print[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def println[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def error[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def errorln[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit

  test("The ID of the player should equal what is simulated in standard input"):
    given mockConsole: Console[IO] = mockConsoleWith(() => expectedPlayerID)
    val actualPlayerID: String = getPlayerID.unsafeRunSync()
    actualPlayerID shouldEqual expectedPlayerID

  test("The initial balance of the player should equal what is simulated in standard input"):
    given mockConsole: Console[IO] = mockConsoleWith(() => expectedBalance.toString)
    val actualBalance: Int = getInitialBalance.unsafeRunSync()
    actualBalance shouldEqual expectedBalance

  test("The view should retry until a valid positive integer is provided"):
    val simulatedInputs = Iterator("error", "-50", expectedBalance.toString)
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualBalance: Int = getInitialBalance.unsafeRunSync()
    actualBalance shouldEqual expectedBalance

  test("The bet of the player should equal what is simulated in standard input"):
    val player = Player(expectedPlayerID, expectedBalance)
    val expectedBet = 100
    given mockConsole: Console[IO] = mockConsoleWith(() => expectedBet.toString)
    val actualBet: Int = getBet(player).unsafeRunSync()
    actualBet shouldEqual expectedBet

  test("The numbers of players chosen by the user should equal what is simulated in standard input"):
    val expectedNumber = 4
    given mockConsole: Console[IO] = mockConsoleWith(expectedNumber.toString)
    val actualNumber: Int = getNumPlayers.unsafeRunSync()
    actualNumber shouldEqual expectedNumber
