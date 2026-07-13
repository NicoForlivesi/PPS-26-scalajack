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

  test("balances should correctly map players to tuples of (name, totalValue)"):
    val result = game.balances(List(firstPlayer, secondPlayer))
    result shouldBe List(("Alice", 200.0), ("Bob", 300.0))

  test("balances should return an empty list when given an empty list of players"):
    val result = game.balances(List.empty)
    result shouldBe List.empty

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
    game.initialBlackjackPlayers() shouldBe List(firstPlayer)

  test("playersWithBlackjack returns an empty list when no player has blackjack"):
    firstPlayer.addCard(ten)
    firstPlayer.addCard(six)
    secondPlayer.addCard(ten)
    secondPlayer.addCard(six)
    game.initialBlackjackPlayers() shouldBe List.empty

  test("handleBlackjacks pays the winners instantly (if the dealer can't has BJ) with the bet multiplied by the blackjack payout"):
    val startingBalance = firstPlayer.balance.totalValue
    game.currentBets = List(Bet(firstPlayer, betAmount), Bet(secondPlayer, betAmount))
    game.dealer.addCard(six)
    firstPlayer.addCard(ace)
    firstPlayer.addCard(king)
    secondPlayer.addCard(ten)
    secondPlayer.addCard(six)
    game.handleBlackjacks(game.initialBlackjackPlayers())
    firstPlayer.balance.totalValue shouldBe startingBalance + betAmount * BlackjackPayoutMultiplier

  test("handleBlackjacks updates the state of every player with BJ to 'Blackjack' also if the dealer could have BJ"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    game.dealer.addCard(ace)
    firstPlayer.addCard(ace)
    firstPlayer.addCard(king)
    game.handleBlackjacks(List(firstPlayer))
    firstPlayer.state shouldBe PlayerState.Blackjack

  test("handleBlackjacks does not affect players that are not winners"):
    val secondPlayerBalance = secondPlayer.balance.totalValue
    game.currentBets = List(Bet(firstPlayer, betAmount), Bet(secondPlayer, betAmount))
    game.dealer.addCard(six)
    firstPlayer.addCard(ace)
    firstPlayer.addCard(king)
    secondPlayer.addCard(king)
    secondPlayer.addCard(six)
    game.handleBlackjacks(game.initialBlackjackPlayers())
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
    game.evaluateDealerBust shouldBe false
    game.dealer.addCard(StandardCard(Suit.Clubs, Value.Ten))
    game.evaluateDealerBust shouldBe true

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
    game.handleHandEnd()
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

  test("canDoubleDown is true when the player has exactly two cards and enough balance"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Six))
    firstPlayer.addCard(StandardCard(Suit.Spades, Value.Seven))
    game.canDoubleDown(firstPlayer) shouldBe true

  test("canDoubleDown is false when the player has only one card"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Six))
    game.canDoubleDown(firstPlayer) shouldBe false

  test("canDoubleDown is false when the player has more than two cards"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Six))
    firstPlayer.addCard(StandardCard(Suit.Spades, Value.Seven))
    firstPlayer.addCard(StandardCard(Suit.Clubs, Value.Two))
    game.canDoubleDown(firstPlayer) shouldBe false

  test("canDoubleDown is false when the player doesn't have enough balance"):
    val notDoubleDownableBet = firstPlayer.balance.totalValue.toInt + 1
    game.currentBets = List(Bet(firstPlayer, notDoubleDownableBet))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Six))
    firstPlayer.addCard(StandardCard(Suit.Spades, Value.Seven))
    game.canDoubleDown(firstPlayer) shouldBe false

  test("doubleDown doubles the player's current bet"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Six))
    firstPlayer.addCard(StandardCard(Suit.Spades, Value.Seven))
    game.doubleDown(firstPlayer)
    game.currentBets shouldBe List(Bet(firstPlayer, betAmount * 2))

  test("doubleDown draws exactly one card for the player"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Six))
    firstPlayer.addCard(StandardCard(Suit.Spades, Value.Seven))
    game.doubleDown(firstPlayer)
    firstPlayer.cards.size shouldBe 3

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
    splitPlayer.deposit(2 * betAmount)
    splitPlayer.addCard(ace)
    game.canSplit(splitPlayer) shouldBe false

  test("transfer balance should transfer all the balance of the player to the splitPlayer"):
    val splitPlayer = SplitPlayer(firstPlayer.name + "_split1", ace)
    val secondSplit = SplitPlayer(firstPlayer.name + "_split2", ace)
    val testGame = Game(List(firstPlayer, splitPlayer, secondSplit))
    val expectedBalance = firstPlayer.balance.totalValue
    testGame.transferBalance(firstPlayer)
    firstPlayer.balance.totalValue shouldBe 0.0
    splitPlayer.balance.totalValue shouldBe expectedBalance

    testGame.transferBalance(splitPlayer)
    firstPlayer.balance.totalValue shouldBe 0.0
    splitPlayer.balance.totalValue shouldBe 0.0
    secondSplit.balance.totalValue shouldBe expectedBalance

    testGame.transferBalance(secondPlayer)
    firstPlayer.balance.totalValue shouldBe 0.0
    splitPlayer.balance.totalValue shouldBe 0.0
    secondSplit.balance.totalValue shouldBe expectedBalance

  test("transfer balance should not change the initial balance of players in case of no splits"):
    val testGame = Game(List(firstPlayer, secondPlayer))
    val expectedBalance1 = firstPlayer.balance.totalValue
    val expectedBalance2 = secondPlayer.balance.totalValue
    testGame.transferBalance(firstPlayer)
    testGame.transferBalance(secondPlayer)
    firstPlayer.balance.totalValue shouldBe expectedBalance1
    secondPlayer.balance.totalValue shouldBe expectedBalance2

  test("payOutHand pays the player double the bet when their score beats a non-busted dealer"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Six))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Clubs, Value.Eight))
    val startingBalance = firstPlayer.balance.totalValue
    game.handlePayout()
    firstPlayer.balance.totalValue shouldBe startingBalance + betAmount * 2

  test("payOutHand return the bet to the player when he push"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Eight))
    firstPlayer.addCard(StandardCard(Suit.Clubs, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Diamonds, Value.Eight))
    val startingBalance = firstPlayer.balance.totalValue
    game.handlePayout()
    firstPlayer.balance.totalValue shouldBe startingBalance + betAmount

  test("payOutHand takes the bet when the player loses to a higher dealer score"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Clubs, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Diamonds, Value.Eight))
    val startingBalance = firstPlayer.balance.totalValue
    game.handlePayout()
    firstPlayer.balance.totalValue shouldBe startingBalance

  test("payOutHand pays the player when the dealer busts, regardless of the player's own score"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Clubs, Value.Five)) // dealer sballa
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Four))
    firstPlayer.addCard(StandardCard(Suit.Diamonds, Value.Four))
    val startingBalance = firstPlayer.balance.totalValue
    game.handlePayout()
    firstPlayer.balance.totalValue shouldBe startingBalance + betAmount * 2

  test("payOutHand takes the bet from a busted player even when the dealer also busts"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Clubs, Value.Five)) // dealer sballa
    firstPlayer.addCard(ten)
    firstPlayer.addCard(king)
    firstPlayer.addCard(six)
    game.evaluatePlayerBust(firstPlayer) shouldBe true // player sballa
    val startingBalance = firstPlayer.balance.totalValue
    game.handlePayout()
    firstPlayer.balance.totalValue shouldBe startingBalance

  test("payOutHand credits the blackjack payout when the player has blackjack against a 21"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Six))
    game.dealer.addCard(StandardCard(Suit.Clubs, Value.Five))
    firstPlayer.addCard(ace)
    firstPlayer.addCard(king)
    firstPlayer.winBlackjack()
    val startingBalance = firstPlayer.balance.totalValue
    game.handlePayout()
    firstPlayer.balance.totalValue shouldBe startingBalance + betAmount * BlackjackPayoutMultiplier

  test("payOutHand pushes between two BJ"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    game.dealer.addCard(ace)
    game.dealer.addCard(king)
    firstPlayer.addCard(StandardCard(Suit.Diamonds, Value.Ace))
    firstPlayer.addCard(StandardCard(Suit.Clubs, Value.King))
    firstPlayer.winBlackjack()
    val startingBalance = firstPlayer.balance.totalValue
    game.handlePayout()
    firstPlayer.balance.totalValue shouldBe startingBalance + betAmount

  test("payOutHand makes the player lose against a BJ even if he has 21"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    game.dealer.addCard(ace)
    game.dealer.addCard(king)
    firstPlayer.addCard(StandardCard(Suit.Diamonds, Value.Ace))
    firstPlayer.addCard(StandardCard(Suit.Clubs, Value.Five))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Five))
    val startingBalance = firstPlayer.balance.totalValue
    game.handlePayout()
    firstPlayer.balance.totalValue shouldBe startingBalance

  test("payOutHand increases the dealer's profit by the lost bet when the player loses"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Clubs, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Diamonds, Value.Eight))
    val startingProfit = game.dealer.totalProfit
    game.handlePayout()
    game.dealer.totalProfit shouldBe startingProfit + betAmount

  test("payOutHand decreases the dealer's profit by the amount paid out when the player wins"):
    game.currentBets = List(Bet(firstPlayer, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Six))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Clubs, Value.Eight))
    val startingProfit = game.dealer.totalProfit
    game.handlePayout()
    game.dealer.totalProfit shouldBe startingProfit - betAmount

  test("payOutHand does handle a player who has already been paid for their blackjack"):
    game.currentBets = List(Bet(secondPlayer, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Six))
    firstPlayer.addCard(ace)
    firstPlayer.addCard(king)
    firstPlayer.winBlackjack()
    val Balance = firstPlayer.balance.totalValue
    game.handlePayout()
    firstPlayer.balance.totalValue shouldBe Balance

  test("payOutHand should credit the split hand's winnings and its remaining balance to the original player"):
    val splitPlayer = SplitPlayer(s"${firstPlayer.name}_1", ace, betAmount)
    game = Game(List(firstPlayer, splitPlayer))
    game.currentBets = List(Bet(firstPlayer, betAmount), Bet(splitPlayer, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Six))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Clubs, Value.Five))
    splitPlayer.addCard(StandardCard(Suit.Clubs, Value.Eight))
    val startingBalance = firstPlayer.balance.totalValue
    game.handlePayout()
    firstPlayer.balance.totalValue shouldBe startingBalance + betAmount * 3

  test("payOutHand handles scenario where both original and split hands win against the dealer"):
    val splitPlayer = SplitPlayer(s"${firstPlayer.name}_1", ace, betAmount)
    game = Game(List(firstPlayer, splitPlayer))
    game.currentBets = List(Bet(firstPlayer, betAmount), Bet(splitPlayer, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Seven))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Clubs, Value.Eight))
    splitPlayer.addCard(StandardCard(Suit.Diamonds, Value.Eight))
    val startingBalance = firstPlayer.balance.totalValue
    game.handlePayout()
    firstPlayer.balance.totalValue shouldBe startingBalance + (betAmount * 5)

  test("payOutHand updates dealer profit correctly considering multiple bets from a split player"):
    val splitPlayer = SplitPlayer(s"${firstPlayer.name}_1", ace, betAmount)
    game = Game(List(firstPlayer, splitPlayer))
    game.currentBets = List(Bet(firstPlayer, betAmount), Bet(splitPlayer, betAmount))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    firstPlayer.addCard(StandardCard(Suit.Clubs, Value.Seven))
    splitPlayer.addCard(StandardCard(Suit.Diamonds, Value.Six))
    val startingBalance = firstPlayer.balance.totalValue
    game.handlePayout()
    firstPlayer.balance.totalValue shouldBe startingBalance + betAmount

  test("A split player should be correctly removed from the list of players."):
    val splitPlayer = SplitPlayer(s"${firstPlayer.name}_1", ace, betAmount)
    game = Game(List(firstPlayer, splitPlayer))
    game.removeSplitPlayers()
    game.players.size shouldBe 1
    game.players shouldEqual List(firstPlayer)









