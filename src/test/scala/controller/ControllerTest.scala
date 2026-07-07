package controller

import controller.Controller.*
import cats.Show
import cats.effect.IO
import cats.effect.std.Console
import cats.effect.unsafe.implicits.global
import model.GameModule.Game
import model.PlayerModule.{Player, PlayerState}
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
    actualGame.players.head.balance.totalValue shouldEqual 100
    actualGame.players(1).name shouldEqual "Bob"
    actualGame.players(1).balance.totalValue shouldEqual 200

  test("Method initializeHand should collect valid bets from all players, update the game and distribute 2 cards to each player"):
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
    player1.cards.size shouldEqual 2
    player2.cards.size shouldEqual 2
    game.deck.size() shouldBe (52 - 4)

  test("Method endHand should correctly remove from the game all the players that want to leave"):
    val player1 = Player("P1", 50)
    val player2 = Player("P2", 100)
    val game = Game(List(player1, player2))
    val simulatedInputs = Iterator("N", "Y")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    endHand(game).unsafeRunSync()
    game.players.length shouldBe 1
    game.players shouldEqual List(player1)

  test("endHand correctly removes broke players and voluntary leavers"):
    val brokePlayer = Player("Alice", 0)
    val leavingPlayer = Player("Bob", 200)
    val stayingPlayer = Player("Charlie", 500)
    val game = Game(List(brokePlayer, leavingPlayer, stayingPlayer))
    val simulatedInputs = Iterator("Y", "N")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    endHand(game).unsafeRunSync()
    game.players shouldBe List(stayingPlayer)
    brokePlayer.state shouldBe PlayerState.LeftGame
    leavingPlayer.state shouldBe PlayerState.LeftGame
    stayingPlayer.state shouldBe PlayerState.Active