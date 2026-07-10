package model

import model.DeckModule.*
import model.DeckModule.Card.StandardCard
import model.DeckModule.Suit.Spades
import model.DeckModule.Value.{Ace, Six}
import model.GameModule.*
import model.PlayerModule.{SplitPlayer, *}
import model.ScoreModule.calculateScore
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

import scala.annotation.tailrec

class GameTest extends AnyFunSuite with BeforeAndAfterEach:

  var firstPlayer: Player = _
  var secondPlayer: Player = _
  var listPlayers: List[Player] = _
  val splitCard: StandardCard = StandardCard(Suit.Hearts, Value.Ace)
  val splitPlayer = SplitPlayer("Tina", splitCard)
  var game: Game = _
  val betAmount = 100
  val BlackjackPayoutMultiplier = 2.5
  val ace: StandardCard = StandardCard(Suit.Hearts, Value.Ace)
  val king: StandardCard = StandardCard(Suit.Spades, Value.King)
  val ten: StandardCard = StandardCard(Suit.Hearts, Value.Ten)
  val six: StandardCard = StandardCard(Suit.Spades, Value.Six)
  val hiddenCard: StandardCard = StandardCard(Suit.Hearts, Value.Queen, isFaceUp = false)

  override def beforeEach(): Unit =
    firstPlayer = Player("Alice", 200)
    secondPlayer = Player("Bob", 300)
    listPlayers = List(firstPlayer, secondPlayer)
    game = Game(listPlayers)

  test("isNameValid should return true if the player name exists in the game"):
    game.isNameValid("Alice") shouldBe true
    game.isNameValid("Chiara") shouldBe false

  test("isNameValid should be case sensitive when validating names"):
    game.isNameValid("alice") shouldBe false

  test("isBetValid should accept bets that are multiples of minBet and within player balance"):
    game.isBetValid(firstPlayer)(5.0) shouldBe true
    game.isBetValid(firstPlayer)(100.0) shouldBe true

  test("isBetValid should reject bets that are negative, zero, not multiples of minBet, or exceed balance"):
    game.isBetValid(firstPlayer)(0.0) shouldBe false
    game.isBetValid(firstPlayer)(-5.0) shouldBe false
    game.isBetValid(firstPlayer)(7.2) shouldBe false
    game.isBetValid(firstPlayer)(500.0) shouldBe false

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
    game.isOver shouldBe false

  test("game terminates correctly when no players are left"):
    game.removePlayer(firstPlayer)
    game.removePlayer(secondPlayer)
    game.isOver shouldBe true

  test("game terminates correctly when players are all in state 'LeftGame'"):
    val playersInGame = game.players
    game.removePlayer(firstPlayer)
    game.removePlayer(secondPlayer)
    game.isOver shouldBe true

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

  test("handleBlackjacks pays the winners instantly with the bet multiplied by the blackjack payout"):
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

  test("handleBlackjacks does not affect players that are not winners"):
    val secondPlayerBalance = secondPlayer.balance.totalValue
    game.currentBets = List(Bet(firstPlayer, betAmount), Bet(secondPlayer, betAmount))
    firstPlayer.addCard(ace)
    firstPlayer.addCard(king)
    secondPlayer.addCard(king)
    secondPlayer.addCard(six)
    game.handleBlackjacks(game.playersWithBlackjack())
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
    game.evaluatePlayerBust(firstPlayer) shouldBe true
    firstPlayer.state shouldBe PlayerState.Busted

  test("Dealer should be seen as busted only if its score is bigger then 21"):
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Five))
    game.evaluateDealerBust(game.dealer) shouldBe false
    game.dealer.addCard(StandardCard(Suit.Clubs, Value.Ten))
    game.evaluateDealerBust(game.dealer) shouldBe true

  test("computeDealerTurn keeps drawing when the high value busts but the low still below the standing threshold"):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ace, isFaceUp = false))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Five))
    game.dealer.addCard(StandardCard(Suit.Clubs, Value.Six))
    game.computeDealerTurn()
    game.dealer.score.playableValue should be >= 17

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

  test("Dealer does not draw cards when score is already 17 or higher"):
    game.dealer.addCard(king)
    game.dealer.addCard(ten)
    val initialCards = game.dealer.cards.size
    val messages = game.computeDealerTurn()
    game.dealer.cards.size shouldBe initialCards
    game.dealer.cards.calculateScore.maxValue shouldBe 20

  test("startNewHand clears hands of players and dealer"):
    game.dealer.addCard(king)
    firstPlayer.addCard(ten)
    secondPlayer.addCard(six)
    game.startNewHand()
    game.dealer.cards.size shouldBe 0
    firstPlayer.cards.size shouldBe 0
    secondPlayer.cards.size shouldBe 0

  test("drawStandardCard should catch CutCard, set isCutCardInDeck to false, and recursively return a StandardCard"):
   game.isCutCardInDeck shouldBe true
    @tailrec
    def flushUntilCutCard(): Unit =
      if game.isCutCardInDeck then
        game.drawStandardCard()
        flushUntilCutCard()
    flushUntilCutCard()
    game.isCutCardInDeck shouldBe false

  test("drawCard should transparently give a StandardCard to the player even if a CutCard is skipped underneath"):
    while game.isCutCardInDeck do
      game.drawCard(firstPlayer)
    firstPlayer.cards.forall(_.isInstanceOf[Card.StandardCard]) shouldBe true

  test("splitPlayer should create a SplitPlayer and draw a card for both players"):
    val expectedName = firstPlayer.name + "_split1"
    val expectedBet = 30
    val playerInitialBalance = firstPlayer.balance.totalValue
    game.currentBets = List(Bet(firstPlayer, expectedBet))
    firstPlayer.addCard(six)
    firstPlayer.addCard(six)
    val initialPlayers = game.players.size
    val result = game.splitPlayer(firstPlayer)
    result should not be empty
    game.players.size shouldBe initialPlayers + 1
    val splitPlayer = game.players.find(_.isInstanceOf[SplitPlayer])
    splitPlayer should not be empty
    splitPlayer.get.name shouldBe expectedName
    val splitPlayerBet = game.currentBets.find(_.player.name == expectedName)
    splitPlayerBet should not be empty
    splitPlayerBet.get.amount shouldBe expectedBet
    firstPlayer.balance.totalValue shouldBe playerInitialBalance - expectedBet
    game.players shouldBe List(firstPlayer, splitPlayer.get, secondPlayer)
    firstPlayer.cards.size shouldBe 2
    splitPlayer.get.cards.size shouldBe 2
    result.get._1 shouldBe firstPlayer.cards.last
    result.get._2 shouldBe splitPlayer.get.cards.last

  test("The splitting should not be done if the player has two ace and had already perform a split before"):
    val ace: StandardCard = StandardCard(Suit.Hearts, Ace)
    val splitPlayer = SplitPlayer(firstPlayer.name + "_split1", ace)
    val testGame = Game(List(firstPlayer, splitPlayer))
    firstPlayer.addCard(ace)
    firstPlayer.addCard(ace)
    splitPlayer.addCard(ace)
    game.currentBets = List(Bet(firstPlayer, 20), Bet(splitPlayer, 20))
    game.canSplit(firstPlayer) shouldBe false
    game.canSplit(splitPlayer) shouldBe false

  test("The splitting should not be done if the player has two different cards even if the first one is ace"):
    val ace: StandardCard = StandardCard(Suit.Hearts, Ace)
    val testGame = Game(List(firstPlayer))
    firstPlayer.addCard(ace)
    firstPlayer.addCard(StandardCard(Spades, Six))
    splitPlayer.addCard(ace)
    game.currentBets = List(Bet(firstPlayer, 20))
    game.canSplit(firstPlayer) shouldBe false

  test("The split can be done if the player has two cards of the same value"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.canSplit(firstPlayer) shouldBe true

  test("The split cannot be done if the player has multiple cards or two cards with different value"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Spades, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Clubs, Value.Ten))
    game.canSplit(firstPlayer) shouldBe false
    firstPlayer.clearHand()
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Spades, Value.Ace))
    game.canSplit(firstPlayer) shouldBe false

  test("The split cannot be done if the player does not have enough balance"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    val bet = 100
    firstPlayer.withdraw(bet)
    game.canSplit(firstPlayer) shouldBe false

  test("A split player should have the split card in its hand"):
    splitPlayer.cards.size shouldBe 2
    splitPlayer.balance.totalValue shouldBe 0.0
    splitPlayer.cards should contain(splitCard)

  test("A split player cannot split its card if it has an ace"):
    game.currentBets = List(Bet(splitPlayer, betAmount))
    game.canSplit(splitPlayer) shouldBe false









