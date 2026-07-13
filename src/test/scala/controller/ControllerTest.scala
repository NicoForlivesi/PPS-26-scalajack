package controller

import controller.Controller.*
import cats.Show
import cats.effect.IO
import cats.effect.std.Console
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterEach
import model.DeckModule.*
import model.DeckModule.Card.{CutCard, StandardCard}
import model.GameModule.{Bet, Game}
import model.PlayerModule.{Player, PlayerState, SplitPlayer}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*
import view.View.PlayerAction

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

  test("getPlayers should correctly parse names and associate their deposits"):
    val simulatedInputs = Iterator("Elena, Chiara", "200", "150")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val players = getPlayers(2).unsafeRunSync()
    players.length shouldBe 2
    players.head.name shouldBe "Elena"
    players.head.balance.totalValue shouldBe 200
    players(1).name shouldBe "Chiara"
    players(1).balance.totalValue shouldBe 150
    simulatedInputs.hasNext shouldBe false

  test("Method initializeGame should coordinate view methods to build a Game with players"):
    val simulatedInputs = Iterator(game.players.size.toString, s"${player1.name}, ${player2.name}", player1.balance.totalValue.toString, player2.balance.totalValue.toString)
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

  test("Method getBets should subtract the bet from the players deposit instantly"):
    val simulatedInputs = Iterator("30", "70")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    getBets(game).unsafeRunSync()
    player1.balance.totalValue shouldBe 20
    player2.balance.totalValue shouldBe 30

  test("handleBlackJacks should execute side-effects on model and render updates to view"):
    val bet = 20
    val initialBalance1 = player1.balance.totalValue
    val initialBalance2 = player2.balance.totalValue
    game.currentBets = List(Bet(player1, bet), Bet(player2, bet))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Six))
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

  test("handlePlayerAction should render ShowCutCard message when CutCard is drawn"):
    val craftedDeck = Deck.testDeck(
      CutCard,
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Hearts, Value.Four)
    )
    val testGame = Game(List(player1), craftedDeck)
    val printedMessages = scala.collection.mutable.ListBuffer.empty[String]
    given mockConsole: Console[IO] = new Console[IO]:
      override def readLine: IO[String] = IO.pure("D")
      override def readLineWithCharset(charset: Charset): IO[String] = readLine
      override def print[A](a: A)(implicit S: Show[A]): IO[Unit] = IO(printedMessages.append(S.show(a)))
      override def println[A](a: A)(implicit S: Show[A]): IO[Unit] = IO(printedMessages.append(S.show(a)))
      override def error[A](a: A)(implicit S: Show[A]): IO[Unit] = IO.unit
      override def errorln[A](a: A)(implicit S: Show[A]): IO[Unit] = IO.unit
    val result = handlePlayerAction(testGame, player1, PlayerAction.DrawCard).unsafeRunSync()
    testGame.isCutCardInDeck shouldBe false
    val hasCutCardMessage = printedMessages.exists(_.contains("CUT CARD HAS BEEN EXTRACTED!"))
    hasCutCardMessage shouldBe true

  test("handlePlayersTurn should add a new SplitPlayer in the list of players of the game when the user ask to split (transfer balance correctly)"):
    val expectedName = player1.name + "_split1"
    val expectedBalance = player1.balance.totalValue
    val betAmount = 10
    game.currentBets = List(Bet(player1, betAmount))
    val numInitialPlayers = game.players.size
    val splitCardValue = Value.Six
    player1.addCard(StandardCard(Suit.Spades, splitCardValue))
    player1.addCard(StandardCard(Suit.Hearts, splitCardValue))
    val simulatedInputs = Iterator("P", "S", "S", "S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    game.players.size shouldBe numInitialPlayers + 1
    game.players.count(_.name == player1.name) shouldBe 1
    game.players.count(_.name == expectedName) shouldBe 1
    val addedSplitPlayer = game.players(1)
    addedSplitPlayer.isInstanceOf[SplitPlayer] shouldBe true
    addedSplitPlayer.cards.exists(_.value == splitCardValue)
    player1.cards.exists(_.value == splitCardValue)
    addedSplitPlayer.cards.size shouldBe 2
    player1.cards.size shouldBe 2

    player1.balance.totalValue shouldBe 0.0 //forse da modificare quando si gestisce il fine turno che toglie le istanze di SplitPlayer e riassegna il balance al player originale
    addedSplitPlayer.balance.totalValue shouldBe expectedBalance - betAmount

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

  test("handleHands should terminate immediately after the first hand if CutCard is extracted"):
    val craftedDeck = Deck.testDeck(
      CutCard,
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Hearts, Value.Ten),
      StandardCard(Suit.Spades, Value.Queen),
      StandardCard(Suit.Clubs, Value.Ten),
      StandardCard(Suit.Spades, Value.Two)
    )
    val testGame = Game(List(player1), craftedDeck)
    val simulatedInputs = Iterator("10", "S", player1.name)
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handleHands(testGame).unsafeRunSync()
    testGame.isCutCardInDeck shouldBe false

  test("handleHands should terminate immediately if there are no players left"):
    game.removePlayer(player1)
    game.removePlayer(player2)
    game.players shouldBe empty
    val initialCutCardState = game.isCutCardInDeck
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleHands(game).unsafeRunSync()
    game.isCutCardInDeck shouldBe initialCutCardState

  test("handleHandWinners should trigger dealer bust message, execute payouts, and reset game for the next hand"):
    val betAmount = 20
    game.currentBets = List(Bet(player1, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Clubs, Value.Five))
    player1.addCard(StandardCard(Suit.Hearts, Value.Ten))
    player1.addCard(StandardCard(Suit.Clubs, Value.Eight))
    val startingBalance = player1.balance.totalValue
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleHandWinners(game).unsafeRunSync()
    player1.balance.totalValue shouldBe startingBalance + (betAmount * 2)

  test("endHand correctly removes broke players and voluntary leavers"):
    val brokePlayer = Player("Alice", 0)
    val leavingPlayer = Player("Bob", 200)
    val stayingPlayer = Player("Charlie", 500)
    val game = Game(List(brokePlayer, leavingPlayer, stayingPlayer))
    val simulatedInputs = Iterator("Bob")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    endHand(game).unsafeRunSync()
    game.players shouldBe List(stayingPlayer)
    stayingPlayer.state shouldBe PlayerState.Active

  test("endHand prepares players and dealer for the next hand"):
    player1.addCard(StandardCard(Suit.Hearts, Value.Ten))
    player1.bust()
    player2.addCard(StandardCard(Suit.Hearts, Value.King))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Nine))
    val simulatedInputs = Iterator("") // nessuno dei due lascia il tavolo
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    endHand(game).unsafeRunSync()
    player1.state shouldBe PlayerState.Active
    player1.cards shouldBe empty
    player2.state shouldBe PlayerState.Active
    player2.cards shouldBe empty
    game.dealer.cards shouldBe empty

  test("handlePlayersTurn should double the bet, draw one card, and automatically stand when the player doubles down"):
    val betAmount = 25
    val testGame = Game(List(player1), Deck.testDeck(StandardCard(Suit.Hearts, Value.Six)))
    testGame.currentBets = List(Bet(player1, betAmount))
    player1.addCard(StandardCard(Suit.Hearts, Value.Six))
    player1.addCard(StandardCard(Suit.Spades, Value.Seven))
    val simulatedInputs = Iterator("O")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(testGame).unsafeRunSync()
    player1.cards.size shouldBe 3
    testGame.currentBets shouldBe List(Bet(player1, betAmount * 2))
    player1.state shouldBe PlayerState.Standing
    testGame.deck.size() shouldBe 0

  test("handlePlayersTurn should set to Blackjack state players that has done blackjack after the split"):
    val betAmount = 25
    val bet = List(Bet(player1, betAmount))
    val card: StandardCard = StandardCard(Suit.Spades, Value.Queen)
    val six = StandardCard(Suit.Hearts, Value.Six)
    val secondCard = StandardCard(Suit.Spades, Value.Ace)
    val testGame = Game(List(player1), Deck.testDeck(card, six, card, six, secondCard, secondCard))
    testGame.currentBets = bet
    val cardsAfterSplit = List(card, secondCard)
    val simulatedInputs = Iterator("P")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    testGame.distributeCards()
    handlePlayersTurn(testGame).unsafeRunSync()
    val splitPlayer = testGame.players(1)
    player1.cards shouldBe cardsAfterSplit
    splitPlayer.cards shouldBe cardsAfterSplit
    player1.state shouldBe PlayerState.Blackjack
    splitPlayer.state shouldBe PlayerState.Blackjack

  test("handlePlayersTurn should mark the player as busted when doubling down results in a bust"):
    val betAmount = 25
    val testGame = Game(List(player1), Deck.testDeck(StandardCard(Suit.Hearts, Value.King)))
    testGame.currentBets = List(Bet(player1, betAmount))
    player1.addCard(StandardCard(Suit.Hearts, Value.Ten))
    player1.addCard(StandardCard(Suit.Spades, Value.Five))
    val simulatedInputs = Iterator("O")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(testGame).unsafeRunSync()
    player1.cards.size shouldBe 3
    player1.state shouldBe PlayerState.Busted
    testGame.currentBets shouldBe List(Bet(player1, betAmount * 2))

  test("handlePlayersTurn should no longer offer double down after the player has already drawn a card"):
    val betAmount = 25
    val testGame = Game(List(player1), Deck.testDeck(
      StandardCard(Suit.Hearts, Value.Two),
      StandardCard(Suit.Hearts, Value.Three)
    ))
    testGame.currentBets = List(Bet(player1, betAmount))
    player1.addCard(StandardCard(Suit.Hearts, Value.Six))
    player1.addCard(StandardCard(Suit.Spades, Value.Seven))
    val simulatedInputs = Iterator("D", "O", "S") // dopo il primo hit, player1 ha 3 carte e "O" non deve più essere accettato,
      // quindi si aspetta l'input successivo che sarà "S"
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(testGame).unsafeRunSync()
    testGame.currentBets shouldBe List(Bet(player1, betAmount)) // invariata
    player1.state shouldBe PlayerState.Standing

