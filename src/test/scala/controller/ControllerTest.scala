package controller

import controller.Controller.*
import cats.Show
import cats.effect.IO
import cats.effect.std.Console
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterEach
import model.DeckModule.*
import model.DeckModule.Card.StandardCard
import model.GameModule.{Bet, Game}
import model.PlayerModule.{Player, PlayerState, SplittedPlayer}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

import java.nio.charset.Charset

class ControllerTest extends AnyFunSuite with BeforeAndAfterEach:

  var player1: Player = _
  var player2: Player = _
  var game: Game = _

  override def beforeEach(): Unit =
    player1 = Player("P1", 50.0)
    player2 = Player("P2", 100.0)
    game = Game(List(player1, player2))

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

  test("handleBlackJacks should execute side-effects on model and render updates to view"):
    val bet = 20
    val initialBalance1 = player1.balance.totalValue
    val initialBalance2 = player2.balance.totalValue
    game.currentBets = List(Bet(player1, bet), Bet(player2, bet))
    player1.addCard(StandardCard(Suit.Hearts, Value.Ace))
    player1.addCard(StandardCard(Suit.Spades, Value.King))
    player2.addCard(StandardCard(Suit.Clubs, Value.Five))
    player2.addCard(StandardCard(Suit.Diamonds, Value.Ten))
    handleBlackjacksWinners(game).unsafeRunSync()
    player1.balance.totalValue shouldBe initialBalance1 + 2.5 * bet

  test("handlePlayersTurn should allow a player to draw a card and then stand based on console inputs"):
    val simulatedInputs = Iterator("D", "S", "S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    player1.cards.size shouldBe 1
    player2.cards.size shouldBe 0
    game.deck.size() shouldBe 52
    player1.state shouldBe PlayerState.Standing
    player2.state shouldBe PlayerState.Standing

  test("handlePlayersTurn should not ask a player with Blackjack to draw or stand"):
    player1.winBlackjack()
    val simulatedInputs = Iterator("S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    player1.cards.size shouldBe 0
    player1.state shouldBe PlayerState.Blackjack
    player2.state shouldBe PlayerState.Standing

  test("handlePlayersTurn should automatically stand a player who reaches the winning value"):
    val craftedDeck = Deck.testDeck(
      StandardCard(Suit.Hearts, Value.Ace),
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Hearts, Value.Four)
    )
    val testGame = Game(List(player1), craftedDeck)
    val simulatedInputs = Iterator("D", "D", "D") // Asso -> asso + 6 -> asso + 6 + 4
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(testGame).unsafeRunSync()
    player1.cards.size shouldBe 3
    player1.score.playableValue shouldBe 21
    player1.state shouldBe PlayerState.Standing
    testGame.deck.size() shouldBe 0

  test("handlePlayersTurn should add a new SplittedPlayer in the list of players of the game when the user ask to split"):
    val numInitialPlayers = game.players.size
    val splittedCardValue = Value.Six
    player1.addCard(Card(Suit.Spades, splittedCardValue))
    player1.addCard(Card(Suit.Hearts, splittedCardValue))
    val simulatedInputs = Iterator("P", "S", "S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    game.players.size shouldBe numInitialPlayers + 1
    game.players.count(p => p.name == player1.name) shouldBe 2
    val addedSplittedPlayer = game.players(1)
    addedSplittedPlayer.isInstanceOf[SplittedPlayer] shouldBe true
    addedSplittedPlayer.cards.exists(c => c.value == splittedCardValue)
    player1.cards.exists(c => c.value == splittedCardValue)
    addedSplittedPlayer.cards.size shouldBe 2
    player1.cards.size shouldBe 2

  test("handleDealerTurn should execute dealer's automatic AI and draw cards until threshold"):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Six))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Five))
    game.dealer.cards.size shouldBe 2
    val initialDeckSize = game.deck.size()
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleDealerTurn(game).unsafeRunSync()
    game.dealer.cards.size shouldBe > (2)
    val expectedCardsDrawn = game.dealer.cards.size - 2
    game.deck.size() shouldBe (initialDeckSize - expectedCardsDrawn)
    game.dealer.score.maxValue shouldBe >= (17)

  test("Method initializeHand should collect valid bets from all players, update the game and distribute 2 cards to each player"):
    val participants = game.players :+ game.dealer
    game.players.foreach(player => player.clearHand())
    val simulatedInputs = Iterator("30", "40")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    initializeHand(game).unsafeRunSync()
    game.currentBets.map(_.amount) shouldBe List(30.0, 40.0)
    participants.foreach(_.cards.size shouldBe 2)
    val expectedDrawnCards = participants.size * 2
    game.deck.size() shouldBe (53 - expectedDrawnCards)

  test("handleHands should terminate immediately if there are no players left"):
    game.removePlayer(player1)
    game.removePlayer(player2)
    game.players shouldBe empty
    val initialCutCardState = game.isCutCardInDeck
    handleHands(game).unsafeRunSync()
    game.isCutCardInDeck shouldBe initialCutCardState

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

