package controller

import controller.Controller.*
import cats.Show
import cats.effect.IO
import cats.effect.std.Console
import cats.effect.unsafe.implicits.global
import model.GameModule.{Game, Bet}
import model.PlayerModule.{Player, PlayerState}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

import java.nio.charset.Charset

class ControllerTest extends AnyFunSuite:

  val player1 = Player("P1", 50.0)
  val player2 = Player("P2", 100.0)
  val game = Game(List(player1, player2))

  def mockConsoleWith(readLineBehavior: () => String): Console[IO] = new Console[IO]:
    override def readLine: IO[String] = IO(readLineBehavior())
    override def readLineWithCharset(charset: Charset): IO[String] = readLine
    override def print[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def println[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def error[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def errorln[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit

  test("Method getPlayer should coordinate view methods to build a Player"):
    val simulatedInputs = Iterator(player1.name, player1.balance.totalValue.toString)
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualPlayer: Player = getPlayer.unsafeRunSync()
    actualPlayer.name shouldEqual player1.name
    actualPlayer.balance shouldEqual player1.balance

  test("Method initializeGame should coordinate view methods to build a Game with players"):
    val simulatedInputs = Iterator(game.players.size.toString, player1.name, player1.balance.totalValue.toString, player2.name, player2.balance.totalValue.toString)
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualGame: Game = initializeGame.unsafeRunSync()
    actualGame.players.size shouldEqual 2
    actualGame.dealer should not be null
    actualGame.players.head.name shouldEqual player1.name
    actualGame.players.head.balance.totalValue shouldEqual player1.balance.totalValue
    actualGame.players(1).name shouldEqual player2.name
    actualGame.players(1).balance.totalValue shouldEqual player2.balance.totalValue

  test("Method getBets should collect valid bets from all players and update the game state"):
    val simulatedInputs = Iterator("invalid_bet", "30", "40")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    getBets(game).unsafeRunSync()
    game.currentBets match
      case Bet(p1, b1) :: Bet(p2, b2) :: Nil =>
        p1 shouldBe player1
        b1 shouldBe 30.0
        p2 shouldBe player2
        b2 shouldBe 40.0
      case other =>
        fail(s"Expected exactly 2 bets in the list, but got: $other")

  test("Method initializeHand should collect valid bets from all players, update the game and distribute 2 cards to each player"):
    val participants = game.players :+ game.dealer
    val simulatedInputs = Iterator("30", "40")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    initializeHand(game).unsafeRunSync()
    game.currentBets.map(_.bet) shouldBe List(30.0, 40.0)
    participants.foreach(_.cards.size shouldBe 2)
    val expectedDrawnCards = participants.size * 2
    game.deck.size() shouldBe (52 - expectedDrawnCards)

  test("Method endHand should correctly remove from the game all the players that want to leave"):
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