package model

import model.GameModule.*
import model.PlayerModule.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

class GameTest extends AnyFunSuite with BeforeAndAfterEach:

  var firstPlayer: Player = _
  var secondPlayer: Player = _
  var listPlayers: List[Player] = _
  var game: Game = _
  val betAmount = 100

  override def beforeEach(): Unit =
    firstPlayer = Player("Alice", 200)
    secondPlayer = Player("Bob", 300)
    listPlayers = List(firstPlayer, secondPlayer)
    game = Game(listPlayers)

  test("player's bet is computed correctly"):
    val playerBet = Bet(firstPlayer, betAmount)
    playerBet.player shouldBe firstPlayer
    playerBet.bet shouldBe betAmount

  test("game is initialized as expected"):
    game.players shouldBe listPlayers
    game.currentBets shouldBe List.empty

  test("players' bets are set correctly"):
    val bets = List(Bet(firstPlayer, betAmount), Bet(secondPlayer, betAmount))
    game.currentBets = bets
    game.currentBets.length shouldBe bets.length
    game.currentBets shouldBe bets

  test("distributeCards should give exactly 2 cards to each participant (players + dealer)"):
    val numParticipants = 3
    game.players.foreach(p => p.cards shouldBe List.empty)
    game.dealer.cards shouldBe empty
    val initialDeckSize = game.deck.size()
    val messages = game.distributeCards()
    game.players.foreach(p => p.cards.size shouldBe 2)
    game.dealer.cards.size shouldBe 2
    game.dealer.cards.head.isFaceUp shouldBe true
    game.dealer.cards(1).isFaceUp shouldBe false
    // The deck must be reduced by: Number of players * 2 rounds = 6 cards
    game.deck.size() shouldBe (initialDeckSize - numParticipants * 2)

  test("distributeCards should return a list of formatted strings for each card dealt"):
    val expectedMessageCount = game.players.size * 3
    val messages = game.distributeCards()
    messages.size shouldBe expectedMessageCount
    messages.count(_.contains("Bob")) shouldBe 2
    messages.count(_.contains("Alice")) shouldBe 2
    messages.count(_.contains("Dealer")) shouldBe 2

  test("players are correctly removed from the game"):
    game.removePlayer(firstPlayer)
    val remainingPlayers = List(secondPlayer)
    game.players.length shouldBe remainingPlayers.length
    game.players shouldEqual remainingPlayers
    firstPlayer.state shouldEqual PlayerState.LeftGame

  test("game do not terminate if there are still active players"):
    game.isOver() shouldBe false

  test("game terminates correctly when no players are left"):
    game.removePlayer(firstPlayer)
    game.removePlayer(secondPlayer)
    game.isOver() shouldBe true

  test("game terminates correctly when players are all in state 'LeftGame'"):
    val playersInGame = game.players
    game.removePlayer(firstPlayer)
    game.removePlayer(secondPlayer)
    game.isOver() shouldBe true



