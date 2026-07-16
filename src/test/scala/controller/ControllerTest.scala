package controller

import utils.GameUIExports.*
import utils.TestExports.*
import utils.ModelExports.*
import controller.Controller.*

import java.nio.charset.Charset

class ControllerTest extends AnyFunSuite with BeforeAndAfterEach:

  var player1: NormalPlayer = _
  var player2: NormalPlayer = _
  var game: Game = _
  var outputMessages: List[String] = _

  override def beforeEach(): Unit =
    player1 = NormalPlayer("P1", 50.0)
    player2 = NormalPlayer("P2", 100.0)
    game = Game(List(player1, player2))
    outputMessages = List.empty[String]

  def mockConsoleWith(readLineBehavior: () => String): Console[IO] = new Console[IO]:
    override def readLine: IO[String] = IO(readLineBehavior())
    override def readLineWithCharset(charset: Charset): IO[String] = readLine
    override def print[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def println[A](a: A)(using S: Show[A]): IO[Unit] = IO:
      outputMessages = outputMessages :+ S.show(a)
    override def error[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def errorln[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit

  test("Players count should match the requested number."):
    val simulatedInputs = Iterator("Elena, Chiara", "200", "150")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val players = getPlayers(2).unsafeRunSync()
    players.length shouldBe 2

  test("Players should have the correct names."):
    val simulatedInputs = Iterator("Elena, Chiara", "200", "150")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val players = getPlayers(2).unsafeRunSync()
    players.map(_.name) shouldBe List("Elena", "Chiara")

  test("Players should have the correct starting balance."):
    val simulatedInputs = Iterator("Elena, Chiara", "200", "150")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val players = getPlayers(2).unsafeRunSync()
    players.map(_.balance.totalValue) shouldBe List(200.0, 150.0)

  test("The game size should equal max players number."):
    val simulatedInputs = Iterator(
      game.players.size.toString,
      s"${player1.name}, ${player2.name}",
      player1.balance.totalValue.toString,
      player2.balance.totalValue.toString
    )
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualGame: Game = initializeGame.unsafeRunSync()
    actualGame.players.size shouldBe Game.MaxPlayersNum

  test("Human players should sit in the first seats."):
    val simulatedInputs = Iterator(
      game.players.size.toString,
      s"${player1.name}, ${player2.name}",
      player1.balance.totalValue.toString,
      player2.balance.totalValue.toString
    )
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualGame: Game = initializeGame.unsafeRunSync()
    actualGame.players.take(2).map(_.name) shouldBe List(player1.name, player2.name)

  test("A game should have a non null dealer."):
    val simulatedInputs = Iterator(
      game.players.size.toString,
      s"${player1.name}, ${player2.name}",
      player1.balance.totalValue.toString,
      player2.balance.totalValue.toString
    )
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualGame: Game = initializeGame.unsafeRunSync()
    actualGame.dealer should not be null

  test("Human players should keep their initial balance"):
    val simulatedInputs = Iterator(
      game.players.size.toString,
      s"${player1.name}, ${player2.name}",
      player1.balance.totalValue.toString,
      player2.balance.totalValue.toString
    )
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualGame: Game = initializeGame.unsafeRunSync()
    actualGame.players.take(2).map(_.balance.totalValue) shouldBe List(50.0, 100.0)

  test("Remaining slots should be filled with bot players"):
    val simulatedInputs = Iterator(
      game.players.size.toString,
      s"${player1.name}, ${player2.name}",
      player1.balance.totalValue.toString,
      player2.balance.totalValue.toString
    )
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualGame: Game = initializeGame.unsafeRunSync()
    actualGame.players.drop(2).foreach(_.isInstanceOf[BotPlayer] shouldBe true)

  test("The table size should equal the maximum number of players"):
    val names = (1 to Game.MaxPlayersNum).map(i => s"P$i").mkString(", ")
    val deposits = List.fill(Game.MaxPlayersNum)("100")
    val allInputs = Game.MaxPlayersNum.toString :: names :: deposits
    val simulatedInputs = allInputs.iterator
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualGame: Game = initializeGame.unsafeRunSync()
    actualGame.players.size shouldBe Game.MaxPlayersNum

  test("If the human players are up to the maximum number, no bots should be created."):
    val names = (1 to Game.MaxPlayersNum).map(i => s"P$i").mkString(", ")
    val deposits = List.fill(Game.MaxPlayersNum)("100")
    val allInputs = Game.MaxPlayersNum.toString :: names :: deposits
    val simulatedInputs = allInputs.iterator
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualGame: Game = initializeGame.unsafeRunSync()
    actualGame.players.forall(_.isInstanceOf[NormalPlayer]) shouldBe true

  test("Current bets should store the entered amounts."):
    val simulatedInputs = Iterator("invalid_bet", "30", "40")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    getBets(game).unsafeRunSync()
    game.currentBets.map(_.amount) shouldBe List(30, 40)

  test("A player balance should be decreased by bet amount"):
    val simulatedInputs = Iterator("invalid_bet", "30", "40")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    getBets(game).unsafeRunSync()
    player1.balance.totalValue shouldBe 20.0
    player2.balance.totalValue shouldBe 60.0

  test("Bot bets should be correctly recorded."):
    val healthyBot = BotPlayer(name = "Bot1", initialBalance = 100.0, bet = 20)
    val brokeBot = BotPlayer(name = "Bot2", initialBalance = 5.0, bet = 30)
    val botList = List(healthyBot, brokeBot)
    val game = Game(botList)
    implicit val mockConsole: Console[IO] = mockConsoleWith(() => "")
    Controller.getBets(game).unsafeRunSync()
    game.currentBets.map(_.amount) shouldBe List(20, 5)

  test("A whealty bot balance should be decreased by bet amount."):
    val healthyBot = BotPlayer(name = "Bot1", initialBalance = 100.0, bet = 20)
    val brokeBot = BotPlayer(name = "Bot2", initialBalance = 5.0, bet = 30)
    val botList = List(healthyBot, brokeBot)
    val game = Game(botList)
    implicit val mockConsole: Console[IO] = mockConsoleWith(() => "")
    Controller.getBets(game).unsafeRunSync()
    healthyBot.balance.totalValue shouldBe 80.0

  test("The amount of the bet of a broke bot should be fixed to its current balance."):
    val healthyBot = BotPlayer(name = "Bot1", initialBalance = 100.0, bet = 20)
    val brokeBot = BotPlayer(name = "Bot2", initialBalance = 5.0, bet = 30)
    val botList = List(healthyBot, brokeBot)
    val game = Game(botList)
    implicit val mockConsole: Console[IO] = mockConsoleWith(() => "")
    Controller.getBets(game).unsafeRunSync()
    brokeBot.bet shouldBe 5

  test("The bets of a hand should match the user inputs."):
    val testDeck = Deck.testDeck(
      StandardCard(Suit.Hearts, Value.Ace),
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Hearts, Value.Four),
      StandardCard(Suit.Hearts, Value.Seven),
      StandardCard(Suit.Clubs, Value.Four),
      StandardCard(Suit.Diamonds, Value.Six)
    )
    game = Game(List(player1, player2), deck = testDeck)
    val simulatedInputs = Iterator("30", "40")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    initializeHand(game).unsafeRunSync()
    game.currentBets.map(_.amount) shouldBe List(30.0, 40.0)

  test("Every participant should receive two cards."):
    val testDeck = Deck.testDeck(StandardCard(Suit.Hearts, Value.Ace),
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Hearts, Value.Four),
      StandardCard(Suit.Hearts, Value.Seven),
      StandardCard(Suit.Clubs, Value.Four),
      StandardCard(Suit.Diamonds, Value.Six)
    )
    game = Game(List(player1, player2), deck = testDeck)
    val participants = game.players :+ game.dealer
    val simulatedInputs = Iterator("30", "40")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    initializeHand(game).unsafeRunSync()
    participants.foreach(_.cards.size shouldBe 2)

  test("Insurance prompt should be printed when dealer has an ace."):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ace))
    val simulatedInputs = Iterator("10", "20", "P1")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    initializeHand(game).unsafeRunSync()
    outputMessages.exists(_.contains("Insurance")) shouldBe true

  test("A player's insurance bet should be recorded."):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ace))
    val simulatedInputs = Iterator("10", "20", "P1")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    initializeHand(game).unsafeRunSync()
    val betP1 = Bet(player1, 15)
    game.currentBets should contain(betP1)

  test("A player standard bet should remain unchanged if no insurance is made."):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ace))
    val simulatedInputs = Iterator("10", "20", "P1")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    initializeHand(game).unsafeRunSync()
    val betP2 = Bet(player2, 20)
    game.currentBets should contain(betP2)

  test("Insurance request should be skipped if dealer does not have an ace."):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.King))
    val simulatedInputs = Iterator("10", "20", "P1")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    initializeHand(game).unsafeRunSync()
    outputMessages.exists(_.contains("Insurance")) shouldBe false

  test("Blackjack payout should use the correct bonus multiplier."):
    val initialBalance1 = player1.balance.totalValue
    game.currentBets = List(Bet(player1, 20), Bet(player2, 20))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Six))
    player1.addCard(StandardCard(Suit.Hearts, Value.Ace))
    player1.addCard(StandardCard(Suit.Spades, Value.King))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleBlackjacksWinners(game).unsafeRunSync()
    player1.balance.totalValue shouldBe initialBalance1 + 2.5 * 20

  test("The name of a blackjack winner should be printed."):
    game.currentBets = List(Bet(player1, 20), Bet(player2, 20))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Six))
    player1.addCard(StandardCard(Suit.Hearts, Value.Ace))
    player1.addCard(StandardCard(Suit.Spades, Value.King))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleBlackjacksWinners(game).unsafeRunSync()
    outputMessages.exists(_.contains("P1")) shouldBe true

  test("The cut card alert should be printed when drawn."):
    val craftedDeck = Deck.testDeck(
      CutCard,
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Hearts, Value.Four)
    )
    val game = Game(List(player1), craftedDeck)
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    val result = handlePlayerAction(game, player1, PlayerAction.DrawCard).unsafeRunSync()
    val hasCutCardMessage = outputMessages.exists(_.contains("CUT CARD HAS BEEN EXTRACTED!"))
    hasCutCardMessage shouldBe true

  test("A bot should finish its turn automatically."):
    val bot = BotPlayer(name = "Bot", initialBalance = 100.0, bet = 10)
    bot.addCard(StandardCard(Suit.Hearts, Value.Ten))
    val game = Game(List(bot))
    implicit val mockConsole: Console[IO] = mockConsoleWith(() => "")
    Controller.handlePlayersTurn(game).unsafeRunSync()
    bot.hasFinishedTurn shouldBe true

  test("A bot should draw cards during its turn."):
    val bot = BotPlayer(name = "Bot", initialBalance = 100.0, bet = 10)
    bot.addCard(StandardCard(Suit.Hearts, Value.Ten))
    val game = Game(List(bot))
    implicit val mockConsole: Console[IO] = mockConsoleWith(() => "")
    Controller.handlePlayersTurn(game).unsafeRunSync()
    bot.cards.size should be > 1

  test("The name of a bot should be printed during its turn"):
    val bot = BotPlayer(name = "Bot", initialBalance = 100.0, bet = 10)
    bot.addCard(StandardCard(Suit.Hearts, Value.Ten))
    val game = Game(List(bot))
    implicit val mockConsole: Console[IO] = mockConsoleWith(() => "")
    Controller.handlePlayersTurn(game).unsafeRunSync()
    outputMessages.exists(_.contains("Bot")) shouldBe true

  test("A player hand size should increase after drawing."):
    val simulatedInputs = Iterator("D", "S", "S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    player1.cards.size shouldBe 1

  test("The deck size should decrease after drawing a card."):
    val simulatedInputs = Iterator("D", "S", "S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    game.deck.size() shouldBe 208

  test("A player state should become standing after stand choice"):
    val simulatedInputs = Iterator("D", "S", "S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    player1.state shouldBe PlayerState.Standing
    player2.state shouldBe PlayerState.Standing

  test("A player with natural blackjack should not draw cards."):
    player1.winBlackjack()
    val simulatedInputs = Iterator("S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    player1.cards.size shouldBe 0

  test("A player with natural blackjack should keep blackjack state."):
    player1.winBlackjack()
    val simulatedInputs = Iterator("S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    player1.state shouldBe PlayerState.Blackjack

  test("Subsequent players should still be able to stand."):
    player1.winBlackjack()
    val simulatedInputs = Iterator("S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    player2.state shouldBe PlayerState.Standing

  test("A player score should be exactly 21 when reaching 21."):
    val craftedDeck = Deck.testDeck(
      StandardCard(Suit.Hearts, Value.Ace),
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Hearts, Value.Four)
    )
    val testGame = Game(List(player1), craftedDeck)
    val simulatedInputs = Iterator("D", "D", "D")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(testGame).unsafeRunSync()
    player1.score.playableValue shouldBe 21

  test("A player state should become standing after automatic stop at 21."):
    val craftedDeck = Deck.testDeck(
      StandardCard(Suit.Hearts, Value.Ace),
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Hearts, Value.Four)
    )
    val testGame = Game(List(player1), craftedDeck)
    val simulatedInputs = Iterator("D", "D", "D")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(testGame).unsafeRunSync()
    player1.state shouldBe PlayerState.Standing

  test("A test deck should be empty after drawing all cards."):
    val craftedDeck = Deck.testDeck(
      StandardCard(Suit.Hearts, Value.Ace),
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Hearts, Value.Four))
    val testGame = Game(List(player1), craftedDeck)
    val simulatedInputs = Iterator("D", "D", "D")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(testGame).unsafeRunSync()
    testGame.deck.size() shouldBe 0

  test("The players list size should increase after a split"):
    game.currentBets = List(Bet(player1, 10))
    player1.addCard(StandardCard(Suit.Spades, Value.Six))
    player1.addCard(StandardCard(Suit.Hearts, Value.Six))
    val simulatedInputs = Iterator("P", "S", "S", "S")
    given Console[IO] = mockConsoleWith(() => simulatedInputs.next)
    handlePlayersTurn(game).unsafeRunSync()
    game.players.size shouldBe 3

  test("A split player should have a specific modified name."):
    game.currentBets = List(Bet(player1, 10))
    player1.addCard(StandardCard(Suit.Spades, Value.Six))
    player1.addCard(StandardCard(Suit.Hearts, Value.Six))
    val simulatedInputs = Iterator("P", "S", "S", "S")
    given Console[IO] = mockConsoleWith(() => simulatedInputs.next)
    handlePlayersTurn(game).unsafeRunSync()
    game.players(1).name shouldBe "P1_split1"

  test("The original player balance should decrease after split cost."):
    game.currentBets = List(Bet(player1, 10))
    player1.addCard(StandardCard(Suit.Spades, Value.Six))
    player1.addCard(StandardCard(Suit.Hearts, Value.Six))
    val simulatedInputs = Iterator("P", "S", "S", "S")
    given Console[IO] = mockConsoleWith(() => simulatedInputs.next)
    handlePlayersTurn(game).unsafeRunSync()
    player1.balance.totalValue shouldBe 0.0

  test("A player should receive a third card on double down"):
    val testGame = Game(List(player1), Deck.testDeck(StandardCard(Suit.Hearts, Value.Six)))
    testGame.currentBets = List(Bet(player1, 25))
    player1.addCard(StandardCard(Suit.Hearts, Value.Six))
    player1.addCard(StandardCard(Suit.Spades, Value.Seven))
    val simulatedInputs = Iterator("O")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(testGame).unsafeRunSync()
    player1.cards.size shouldBe 3

  test("A bet amount should double on double down."):
    val testGame = Game(List(player1), Deck.testDeck(StandardCard(Suit.Hearts, Value.Six)))
    testGame.currentBets = List(Bet(player1, 25))
    player1.addCard(StandardCard(Suit.Hearts, Value.Six))
    player1.addCard(StandardCard(Suit.Spades, Value.Seven))
    val simulatedInputs = Iterator("O")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(testGame).unsafeRunSync()
    testGame.currentBets shouldBe List(Bet(player1, 50))

  test("A player state should become standing after double down."):
    val testGame = Game(List(player1), Deck.testDeck(StandardCard(Suit.Hearts, Value.Six)))
    testGame.currentBets = List(Bet(player1, 25))
    player1.addCard(StandardCard(Suit.Hearts, Value.Six))
    player1.addCard(StandardCard(Suit.Spades, Value.Seven))
    val simulatedInputs = Iterator("O")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(testGame).unsafeRunSync()
    player1.state shouldBe PlayerState.Standing

  test("Both hands should get blackjack after split if lucky."):
    val deck = Deck.testDeck(
      StandardCard(Suit.Spades, Value.Queen),
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Spades, Value.Queen),
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Spades, Value.Ace),
      StandardCard(Suit.Spades, Value.Ace))
    val game = Game(List(player1), deck)
    game.currentBets = List(Bet(player1, 10))
    val simulatedInputs = Iterator("P")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    game.distributeCards()
    handlePlayersTurn(game).unsafeRunSync()
    game.players.map(_.state) shouldBe List(PlayerState.Blackjack, PlayerState.Blackjack)

  test("A player state should become busted if double down exceeds twenty one."):
    val game = Game(List(player1), Deck.testDeck(StandardCard(Suit.Hearts, Value.King)))
    game.currentBets = List(Bet(player1, 25))
    player1.addCard(StandardCard(Suit.Hearts, Value.Ten))
    player1.addCard(StandardCard(Suit.Spades, Value.Five))
    val simulatedInputs = Iterator("O")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    player1.state shouldBe PlayerState.Busted

  test("A bet should still double even if double down results in a bust."):
    val game = Game(List(player1), Deck.testDeck(StandardCard(Suit.Hearts, Value.King)))
    game.currentBets = List(Bet(player1, 25))
    player1.addCard(StandardCard(Suit.Hearts, Value.Ten))
    player1.addCard(StandardCard(Suit.Spades, Value.Five))
    val simulatedInputs = Iterator("O")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    game.currentBets shouldBe List(Bet(player1, 50))

  test("A bet should not double if hit is chosen before double down."):
    val game = Game(List(player1), Deck.testDeck(StandardCard(Suit.Hearts, Value.Two), StandardCard(Suit.Hearts, Value.Three)))
    game.currentBets = List(Bet(player1, 25))
    player1.addCard(StandardCard(Suit.Hearts, Value.Six))
    player1.addCard(StandardCard(Suit.Spades, Value.Seven))
    val simulatedInputs = Iterator("D", "O", "S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    game.currentBets shouldBe List(Bet(player1, 25))

  test("A player state should become standing after invalid double down path."):
    val game = Game(List(player1), Deck.testDeck(StandardCard(Suit.Hearts, Value.Two), StandardCard(Suit.Hearts, Value.Three)))
    game.currentBets = List(Bet(player1, 25))
    player1.addCard(StandardCard(Suit.Hearts, Value.Six))
    player1.addCard(StandardCard(Suit.Spades, Value.Seven))
    val simulatedInputs = Iterator("D", "O", "S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    player1.state shouldBe PlayerState.Standing

  test("The dealer should draw cards during their turn."):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Six))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Five))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleDealerTurn(game).unsafeRunSync()
    game.dealer.cards.size should be > 2

  test("The dealer final score should be at least seventeen."):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Six))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Five))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleDealerTurn(game).unsafeRunSync()
    game.dealer.score.maxValue should be >= 17

  test("An insured player should be notified on dealer blackjack."):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ace))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.King, isFaceUp = false))
    player1.hasInsurance = true
    game.currentBets = List(Bet(player1, 10), Bet(player2, 20))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleDealerTurn(game).unsafeRunSync()
    outputMessages.exists(_.contains("P1")) shouldBe true

  test("A player balance should increase after dealer bust."):
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

  test("The dealer busted message should be printed."):
    game.currentBets = List(Bet(player1, 20))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Ten))
    game.dealer.addCard(StandardCard(Suit.Clubs, Value.Five))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleHandWinners(game).unsafeRunSync()
    outputMessages.exists(_.contains("DEALER BUSTED")) shouldBe true

  test("Bankrupt or leaving players should be removed."):
    val player3 = NormalPlayer("P3", 0)
    val player4 = NormalPlayer("P4", 3)
    val game = Game(List(player1, player3, player4))
    val simulatedInputs = Iterator("P1")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    endHand(game).unsafeRunSync()
    game.players shouldBe empty

  test("Remaining players state should reset to active for next round."):
    player1.addCard(StandardCard(Suit.Hearts, Value.Ten))
    player1.bust()
    player2.addCard(StandardCard(Suit.Hearts, Value.King))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Nine))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    endHand(game).unsafeRunSync()
    game.players.map(_.state) shouldBe List(PlayerState.Active, PlayerState.Active)

  test("All players cards should be cleared after hand ends."):
    player1.addCard(StandardCard(Suit.Hearts, Value.Ten))
    player2.addCard(StandardCard(Suit.Hearts, Value.King))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Nine))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    endHand(game).unsafeRunSync()
    game.players.map(_.cards) shouldBe List(List.empty, List.empty)

  test("Dealer cards should be cleared after hand ends"):
    player1.addCard(StandardCard(Suit.Hearts, Value.Ten))
    player2.addCard(StandardCard(Suit.Hearts, Value.King))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Nine))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    endHand(game).unsafeRunSync()
    game.dealer.cards shouldBe empty

  test("The game should end when cut card is drawn."):
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
    testGame.isOver shouldBe true

  test("Game should end immediately if there are no human players left."):
    game.removePlayer(player1)
    game.removePlayer(player2)
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleHands(game).unsafeRunSync()
    game.isOver shouldBe true

  test("Game over message should be printed"):
    given Console[IO] = mockConsoleWith(() => "")
    endGame(game).unsafeRunSync()
    outputMessages.exists(_.contains("The game is over!")) shouldBe true

  test("The final balance of a player should be printed"):
    given Console[IO] = mockConsoleWith(() => "")
    endGame(game).unsafeRunSync()
    outputMessages.exists(_.contains("P1 ends the game with 50.0 €.")) shouldBe true
    outputMessages.exists(_.contains("P2 ends the game with 100.0 €.")) shouldBe true
