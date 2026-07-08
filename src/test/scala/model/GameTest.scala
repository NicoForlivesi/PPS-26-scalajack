package model

import model.DeckModule.*
import model.DeckModule.Suit.*
import model.DeckModule.Value.*
import model.GameModule.*
import model.PlayerModule.*
import model.ScoreModule.calculateScore
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

class GameTest extends AnyFunSuite with BeforeAndAfterEach:

  var firstPlayer: Player = _
  var secondPlayer: Player = _
  var listPlayers: List[Player] = _
  var game: Game = _
  val betAmount = 100
  val BlackjackPayoutMultiplier = 2.5
  val ace = Card(Suit.Hearts, Value.Ace)
  val king = Card(Suit.Spades, Value.King)
  val ten = Card(Suit.Hearts, Value.Ten)
  val six = Card(Suit.Spades, Value.Six)
  val hiddenCard = Card(Suit.Hearts, Value.Queen, isFaceUp = false)

  override def beforeEach(): Unit =
    firstPlayer = Player("Alice", 200)
    secondPlayer = Player("Bob", 300)
    listPlayers = List(firstPlayer, secondPlayer)
    game = Game(listPlayers)

  test("player's bet is computed correctly"):
    val playerBet = Bet(firstPlayer, betAmount)
    playerBet.player shouldBe firstPlayer
    playerBet.amount shouldBe betAmount

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

  test("playersWithBlackjack returns the players who got a natural blackjack"):
    firstPlayer.addCard(ace)
    firstPlayer.addCard(king)
    secondPlayer.addCard(ten)
    secondPlayer.addCard(six)
    game.playersWithBlackjack() shouldBe List(firstPlayer)

  test("playersWithBlackjack returns an empty list when no player has blackjack"):
    firstPlayer.addCard(ten)
    firstPlayer.addCard(six)
    secondPlayer.addCard(ten)
    secondPlayer.addCard(six)
    game.playersWithBlackjack() shouldBe List.empty

  test("handleBlackjacks pays the winners with the bet multiplied by the blackjack payout"):
    val startingBalance = firstPlayer.balance.totalValue
    game.currentBets = List(Bet(firstPlayer, betAmount), Bet(secondPlayer, betAmount))
    firstPlayer.addCard(ace)
    firstPlayer.addCard(king)
    secondPlayer.addCard(ten)
    secondPlayer.addCard(six)
    game.handleBlackjacks(game.playersWithBlackjack())
    firstPlayer.balance.totalValue shouldBe startingBalance + betAmount * BlackjackPayoutMultiplier

  test("handleBlackjacks updates the state of every paid player to 'Blackjack'"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    firstPlayer.addCard(ace)
    firstPlayer.addCard(king)
    game.handleBlackjacks(List(firstPlayer))
    firstPlayer.state shouldBe PlayerState.Blackjack

  test("handleBlackjacks does not affect players outside the given list"):
    val secondPlayerBalance = secondPlayer.balance.totalValue
    game.currentBets = List(Bet(firstPlayer, betAmount), Bet(secondPlayer, betAmount))
    firstPlayer.addCard(ace)
    firstPlayer.addCard(king)
    secondPlayer.addCard(ace)
    secondPlayer.addCard(king)
    game.handleBlackjacks(List(firstPlayer))
    secondPlayer.balance.totalValue shouldBe secondPlayerBalance
    secondPlayer.state shouldBe PlayerState.Active

  test("drawCard should add a card to the participant and update the deck when cards are available"):
    val initialDeckSize = game.deck.size()
    val drawnCardOpt = game.drawCard(firstPlayer)
    drawnCardOpt shouldBe defined
    firstPlayer.cards.size shouldBe 1
    firstPlayer.cards.head shouldBe drawnCardOpt.get
    game.deck.size() shouldBe (initialDeckSize - 1)

  test("A player that is busted should be detected correctly by the game"):
    val bustedHand = List(ten, king, six)
    bustedHand.foreach(firstPlayer.addCard)
    game.evaluateBust(firstPlayer) shouldBe true
    firstPlayer.state shouldBe PlayerState.Busted

  test("After the turn of the Dealer the second drawn card should be visible"):
    game.dealer.addCard(ten)
    game.dealer.addCard(hiddenCard)
    game.computeDealerTurn()
    val numDealerCards = game.dealer.cards.size
    game.dealer.cards.count(c => c.isFaceUp) shouldBe numDealerCards

  test("Dealer draws cards until reaching at least 17"):
    game.dealer.addCard(ten)
    game.dealer.addCard(six)
    val initialCards = game.dealer.cards.size
    val messages = game.computeDealerTurn()
    game.dealer.cards.size should be > initialCards
    game.dealer.cards.calculateScore.maxValue should be >= 17
    messages.exists(_.contains("Dealer draws")) shouldBe true

  test("Dealer does not draw cards when score is already 17 or higher"):
    game.dealer.addCard(king)
    game.dealer.addCard(ten)
    val initialCards = game.dealer.cards.size
    val messages = game.computeDealerTurn()
    game.dealer.cards.size shouldBe initialCards
    game.dealer.cards.calculateScore.maxValue shouldBe 20
    messages.exists(_.contains("Dealer draws")) shouldBe false


