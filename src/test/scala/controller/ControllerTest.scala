package controller

import controller.Controller.*
import cats.Show
import cats.effect.IO
import cats.effect.std.Console
import cats.effect.unsafe.implicits.global
import model.GameModule.Game
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
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualPlayer: Player = getPlayer.unsafeRunSync()
    actualPlayer.name shouldEqual expectedPlayer.name
    actualPlayer.balance shouldEqual expectedPlayer.balance

  test("Method initializeGame should coordinate view methods to build a Game with players"):
    val simulatedInputs = Iterator("2", "Alice", "100", "Bob", "200")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualGame: Game = initializeGame.unsafeRunSync()
    actualGame.players.size shouldEqual 2
    actualGame.players.head.name shouldEqual "Alice"
    actualGame.players.head.balance.sum shouldEqual 100
    actualGame.players(1).name shouldEqual "Bob"
    actualGame.players(1).balance.sum shouldEqual 200

  test("Method initializeHand should collect bets from all players and update the game"):
    val player1 = Player("P1", 50)
    val player2 = Player("P2", 100)
    val game = Game(List(player1, player2))
    val simulatedInputs = Iterator("invalid_bet", "30", "40")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    initializeHand(game).unsafeRunSync()
    game.currentBets.size shouldEqual 2
    game.currentBets.head.player shouldEqual player1
    game.currentBets.head.bet shouldEqual 30
    game.currentBets(1).player shouldEqual player2
    game.currentBets(1).bet shouldEqual 40