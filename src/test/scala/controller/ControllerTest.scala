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

  test("getPlayers should correctly parse names and associate their deposits"):
    val simulatedInputs = Iterator("Elena, Chiara", "200", "150")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val players = getPlayers(2).unsafeRunSync()
    players.length shouldBe 2
    players.map(_.name) shouldBe List("Elena", "Chiara")
    players.map(_.balance.totalValue) shouldBe List(200.0, 150.0)

  test("Method initializeGame should coordinate view methods to build a Game with players"):
    val simulatedInputs = Iterator(game.players.size.toString, s"${player1.name}, ${player2.name}", player1.balance.totalValue.toString, player2.balance.totalValue.toString)
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualGame: Game = initializeGame.unsafeRunSync()
    actualGame.players.size shouldBe Game.MaxPlayersNum
    actualGame.players.take(2).map(_.name) shouldBe List(player1.name, player2.name)
    actualGame.dealer should not be null
    actualGame.players.take(2).map(_.balance.totalValue) shouldBe List(50.0, 100.0)

  test("Method initializeGame fills the remaining slots with BotPlayers after registering the human players"):
    val simulatedInputs = Iterator(game.players.size.toString, s"${player1.name}, ${player2.name}", player1.balance.totalValue.toString, player2.balance.totalValue.toString)
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualGame: Game = initializeGame.unsafeRunSync()
    actualGame.players.drop(2).size shouldBe Game.MaxPlayersNum - 2
    actualGame.players.drop(2).foreach(_.isInstanceOf[BotPlayer] shouldBe true)

  test("Method initializeGame does not add any bots when human players already fill all the slots"):
    val names = (1 to Game.MaxPlayersNum).map(i => s"P$i").mkString(", ")
    val deposits = List.fill(Game.MaxPlayersNum)("100")
    val allInputs = Game.MaxPlayersNum.toString :: names :: deposits
    val simulatedInputs = allInputs.iterator
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualGame: Game = initializeGame.unsafeRunSync()
    actualGame.players.size shouldBe Game.MaxPlayersNum
    actualGame.players.forall(_.isInstanceOf[NormalPlayer]) shouldBe true
    actualGame.players.forall(_.isInstanceOf[BotPlayer]) shouldBe false

  test("Method getBets should collect valid bets from all players and update the game state"):
    val simulatedInputs = Iterator("invalid_bet", "30", "40")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    getBets(game).unsafeRunSync()
    game.currentBets.map(_.amount) shouldBe List(30, 40)
    player1.balance.totalValue shouldBe 20.0
    player2.balance.totalValue shouldBe 60.0

  test("getBets should automatically process bot bets without triggering console I/O"):
    val healthyBot = BotPlayer(name = "Bot1", initialBalance = 100.0, bet = 20)
    val brokeBot = BotPlayer(name = "Bot2", initialBalance = 5.0, bet = 30)
    val botList = List(healthyBot, brokeBot)
    val game = Game(botList)
    implicit val mockConsole: Console[IO] = mockConsoleWith(() => "")
    Controller.getBets(game).unsafeRunSync()
    game.currentBets.map(_.amount) shouldBe List(20, 5)
    healthyBot.balance.totalValue shouldBe 80.0
    brokeBot.balance.totalValue shouldBe 0.0

  test("Method initializeHand should collect valid bets from all players, update the game and distribute 2 cards to each player"):
    val testDeck = Deck.testDeck(
      StandardCard(Suit.Hearts, Value.Ace),
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
    game.currentBets.map(_.amount) shouldBe List(30.0, 40.0)
    participants.foreach(_.cards.size shouldBe 2)

  test("initializeHand should prompt for insurance when the dealer has a face-up Ace"):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ace))
    val simulatedInputs = Iterator("10", "20", "P1")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    initializeHand(game).unsafeRunSync()
    val betP1 = Bet(player1, 15)
    val betP2 = Bet(player2, 20)
    game.currentBets should contain(betP1)
    game.currentBets should contain(betP2)
    outputMessages.exists(_.contains("Insurance")) shouldBe true

  test("initializeHand should not prompt for insurance when the dealer does not have a face-up Ace"):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.King))
    val simulatedInputs = Iterator("10", "20", "P1")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    initializeHand(game).unsafeRunSync()
    outputMessages.exists(_.contains("Insurance")) shouldBe false

  test("handleBlackjacksWinners processes blackjacks, issues payouts and prints updates"):
    val initialBalance1 = player1.balance.totalValue
    game.currentBets = List(Bet(player1, 20), Bet(player2, 20))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Six))
    player1.addCard(StandardCard(Suit.Hearts, Value.Ace))
    player1.addCard(StandardCard(Suit.Spades, Value.King))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleBlackjacksWinners(game).unsafeRunSync()
    player1.balance.totalValue shouldBe initialBalance1 + 2.5 * 20
    outputMessages.exists(_.contains("P1")) shouldBe true

  test("handlePlayerAction should render ShowCutCard message when CutCard is drawn"):
    val craftedDeck = Deck.testDeck(
      CutCard,
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Hearts, Value.Four)
    )
    val game = Game(List(player1), craftedDeck)
    val printedMessages = scala.collection.mutable.ListBuffer.empty[String]
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    val result = handlePlayerAction(game, player1, PlayerAction.DrawCard).unsafeRunSync()
    val hasCutCardMessage = outputMessages.exists(_.contains("CUT CARD HAS BEEN EXTRACTED!"))
    hasCutCardMessage shouldBe true

  test("startSinglePlayerTurn should execute bot turn and display cards without asking for interactive input"):
    val bot = BotPlayer(name = "Bot", initialBalance = 100.0, bet = 10)
    bot.addCard(StandardCard(Suit.Hearts, Value.Ten))
    val game = Game(List(bot))
    implicit val mockConsole: Console[IO] = mockConsoleWith(() => "")
    Controller.handlePlayersTurn(game).unsafeRunSync()
    bot.hasFinishedTurn shouldBe true
    bot.cards.size should be > 1
    outputMessages.exists(_.contains("Bot")) shouldBe true

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

  test("handlePlayersTurn should add a new SplitPlayer in the list of players of the game when the user ask to split (transfer balance correctly)"):
    game.currentBets = List(Bet(player1, 10))
    player1.addCard(StandardCard(Suit.Spades, Value.Six))
    player1.addCard(StandardCard(Suit.Hearts, Value.Six))
    given Console[IO] = mockConsoleWith(Iterator("P", "S", "S", "S").next)
    handlePlayersTurn(game).unsafeRunSync()
    game.players.size shouldBe 3
    game.players(1).name shouldBe "P1_split1"
    player1.balance.totalValue shouldBe 0.0 //TODO ???

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

  test("handlePlayersTurn should update to Blackjack the state of players that have done Blackjack after the split"):
    val deck = Deck.testDeck(
      StandardCard(Suit.Spades, Value.Queen),
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Spades, Value.Queen),
      StandardCard(Suit.Hearts, Value.Six),
      StandardCard(Suit.Spades, Value.Ace),
      StandardCard(Suit.Spades, Value.Ace)
    )
    val game = Game(List(player1), deck)
    game.currentBets = List(Bet(player1, 10))
    val simulatedInputs = Iterator("P")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    game.distributeCards()
    handlePlayersTurn(game).unsafeRunSync()
    game.players.map(_.state) shouldBe List(PlayerState.Blackjack, PlayerState.Blackjack)

  test("handlePlayersTurn should mark the player as busted when doubling down results in a bust"):
    val betAmount = 25
    val game = Game(List(player1), Deck.testDeck(StandardCard(Suit.Hearts, Value.King)))
    game.currentBets = List(Bet(player1, betAmount))
    player1.addCard(StandardCard(Suit.Hearts, Value.Ten))
    player1.addCard(StandardCard(Suit.Spades, Value.Five))
    val simulatedInputs = Iterator("O")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    player1.state shouldBe PlayerState.Busted
    game.currentBets shouldBe List(Bet(player1, betAmount * 2))

  test("handlePlayersTurn should no longer offer double down after the player has already drawn a card"):
    val betAmount = 25
    val game = Game(List(player1), Deck.testDeck(
      StandardCard(Suit.Hearts, Value.Two),
      StandardCard(Suit.Hearts, Value.Three)
    ))
    game.currentBets = List(Bet(player1, betAmount))
    player1.addCard(StandardCard(Suit.Hearts, Value.Six))
    player1.addCard(StandardCard(Suit.Spades, Value.Seven))
    val simulatedInputs = Iterator("D", "O", "S")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    handlePlayersTurn(game).unsafeRunSync()
    game.currentBets shouldBe List(Bet(player1, betAmount))
    player1.state shouldBe PlayerState.Standing

  test("handleDealerTurn should execute dealer's automatic AI and draw cards until threshold"):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Six))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.Five))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleDealerTurn(game).unsafeRunSync()
    game.dealer.cards.size shouldBe > (2)
    game.dealer.score.maxValue shouldBe >= (17)

  test("handleDealerTurn should notify and payout players if dealer hits Blackjack"):
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Ace))
    game.dealer.addCard(StandardCard(Suit.Spades, Value.King, isFaceUp = false))
    player1.hasInsurance = true
    game.currentBets = List(Bet(player1, 10), Bet(player2, 20))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleDealerTurn(game).unsafeRunSync()
    outputMessages.exists(_.contains("P1")) shouldBe true

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
    outputMessages.exists(_.contains("DEALER BUSTED")) shouldBe true

  test("endHand correctly removes broke players and voluntary leavers"):
    val player3 = NormalPlayer("P3", 0)
    val player4 = NormalPlayer("P4", 3) //is minor than the minimun bet
    val game = Game(List(player1, player3, player4))
    val simulatedInputs = Iterator("P1")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    endHand(game).unsafeRunSync()
    game.players shouldBe empty

  test("endHand prepares players and dealer for the next hand"):
    player1.addCard(StandardCard(Suit.Hearts, Value.Ten))
    player1.bust()
    player2.addCard(StandardCard(Suit.Hearts, Value.King))
    game.dealer.addCard(StandardCard(Suit.Hearts, Value.Nine))
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    endHand(game).unsafeRunSync()
    game.players.map(_.state) shouldBe List(PlayerState.Active, PlayerState.Active)
    game.players.map(_.cards) shouldBe List(List.empty, List.empty)
    game.dealer.cards shouldBe empty

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

  test("handleHands should terminate immediately if there are no players left"):
    game.removePlayer(player1)
    game.removePlayer(player2)
    game.players shouldBe empty
    given mockConsole: Console[IO] = mockConsoleWith(() => "")
    handleHands(game).unsafeRunSync()

  test("endGame should render gameover notice and show remaining players' balances"):
    given Console[IO] = mockConsoleWith(() => "")
    endGame(game).unsafeRunSync()
    outputMessages.exists(_.contains("The game is over!")) shouldBe true
    outputMessages.exists(_.contains("P1 ends the game with 50.0 €.")) shouldBe true
    outputMessages.exists(_.contains("P2 ends the game with 100.0 €.")) shouldBe true