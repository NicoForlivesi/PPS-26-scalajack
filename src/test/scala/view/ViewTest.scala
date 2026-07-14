package view

import cats.Show
import cats.effect.IO
import cats.effect.std.Console
import cats.effect.unsafe.implicits.global
import model.GameModule.Game
import model.DeckModule.*
import model.DeckModule.Card.StandardCard
import model.PlayerModule.{NormalPlayer, Player}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*
import view.View.*

import java.nio.charset.Charset

class ViewTest extends AnyFunSuite with BeforeAndAfterEach:

  var player: Player = _
  val ace: StandardCard = StandardCard(Suit.Hearts, Value.Ace)
  val six: StandardCard = StandardCard(Suit.Hearts, Value.Six)

  override def beforeEach(): Unit =
    player = NormalPlayer("Elena", 500)

  def mockConsoleWith(readLineBehavior: () => String): Console[IO] = new Console[IO]:
    override def readLine: IO[String] = IO(readLineBehavior())
    override def readLineWithCharset(charset: Charset): IO[String] = readLine
    override def print[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def println[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def error[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
    override def errorln[A](a: A)(using S: Show[A]): IO[Unit] = IO.unit
  
  test("The view should keep asking for the number of players until a valid value is entered"):
    val expectedPlayers = 4
    val simulatedInputs = Iterator("error", "-1", "0", expectedPlayers.toString)
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualPlayers = getNumPlayers.unsafeRunSync()
    actualPlayers shouldEqual expectedPlayers

  test("getPlayersNames should correctly parse a valid comma-separated string"):
    val simulatedInputs = Iterator("Elena, Chiara, Tommaso")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val result = getPlayersNames(3).unsafeRunSync()
    result shouldBe List("Elena", "Chiara", "Tommaso")
    simulatedInputs.hasNext shouldBe false

  test("getPlayersNames should retry until the input contains the correct number of unique names"):
    val simulatedInputs = Iterator("Elena, Elena", "Elena, Chiara", "Elena_, Chiara, Mattia", "Elena, Chiara, Mattia")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val result = getPlayersNames(3).unsafeRunSync()
    result shouldBe List("Elena", "Chiara", "Mattia")
    simulatedInputs.hasNext shouldBe false

  test("The initial balance of the player should equal what is simulated in standard input"):
    val targetBalance = 200.0
    val simulatedInputs = Iterator(targetBalance.toString)
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualBalance = getInitialDeposit("Elena", Game.isInitialDepositValid).unsafeRunSync()
    actualBalance shouldBe targetBalance

  test("The view should retry until a valid positive integer is provided"):
    val targetBalance = 200.0
    val simulatedInputs = Iterator("error", "-50", targetBalance.toString)
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val actualBalance = getInitialDeposit("Elena", Game.isInitialDepositValid).unsafeRunSync()
    actualBalance shouldBe targetBalance

  test("The bet of the player should equal what is simulated in standard input"):
    val game: Game = Game(List(player))
    val expectedBet = 100
    given mockConsole: Console[IO] = mockConsoleWith(() => expectedBet.toString)
    val actualBet: Int = getBet(player, game.isBetValid(player)).unsafeRunSync()
    actualBet shouldEqual expectedBet

  test("The numbers of players chosen by the user should equal what is simulated in standard input"):
    val expectedNumber = 4
    given mockConsole: Console[IO] = mockConsoleWith(expectedNumber.toString)
    val actualNumber: Int = getNumPlayers.unsafeRunSync()
    actualNumber shouldEqual expectedNumber

  test("getInsurancePlayers should correctly delegate to promptForPlayerList for a valid input"):
    val simulatedInputs = Iterator("Elena")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val testIsNameValid: String => Boolean = List("Elena", "Chiara").contains
    val result = getInsurancePlayers(testIsNameValid).unsafeRunSync()
    result shouldBe List("Elena")
    simulatedInputs.hasNext shouldBe false

  test("getLeavingPlayers should return an empty list when input is empty (everyone stays)"):
    val simulatedInputs = Iterator("")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val testIsNameValid: String => Boolean = List("Elena", "Chiara").contains
    val result = getLeavingPlayers(testIsNameValid).unsafeRunSync()
    result shouldBe List.empty
    simulatedInputs.hasNext shouldBe false

  test("getLeavingPlayers should correctly parse valid names of leaving players"):
    val simulatedInputs = Iterator("Elena, Chiara")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val testIsNameValid: String => Boolean = List("Elena", "Chiara").contains
    val result = getLeavingPlayers(testIsNameValid).unsafeRunSync()
    result shouldBe List("Elena", "Chiara")
    simulatedInputs.hasNext shouldBe false

  test("getLeavingPlayers should retry if an entered name is not valid in the game"):
    val simulatedInputs = Iterator("Elena, Mario", "Elena")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val testIsNameValid: String => Boolean = List("Elena", "Chiara").contains
    val result = getLeavingPlayers(testIsNameValid).unsafeRunSync()
    result shouldBe List("Elena")
    simulatedInputs.hasNext shouldBe false

  test("When the user inputs an invalid action during his turn the view should retry until a valid action is entered"):
    val simulatedInputs = Iterator("X", "invalid", "DS", "D")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val result = getPlayerAction(player, _ => false, _ => false).unsafeRunSync()
    result shouldBe PlayerAction.DrawCard

  test("A user cannot decide to double down if it's not allowed"):
    player.addCard(six)
    player.addCard(six)
    val simulatedInputs = Iterator("O", "D")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val result = getPlayerAction(player, _ => false, _ => false).unsafeRunSync()
    result shouldBe PlayerAction.DrawCard

  test("A user should be asked to double down only if it's allowed"):
    player.addCard(six)
    player.addCard(six)
    val simulatedInputs = Iterator("X", "invalid", "O")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val result = getPlayerAction(player, _ => true, _ => false).unsafeRunSync()
    result shouldBe PlayerAction.DoubleDown

  test("A user cannot decide to split if he has multiple cards"):
    player.addCard(ace)
    player.addCard(ace)
    player.addCard(six)
    val simulatedInputs = Iterator("X", "invalid", "P", "D")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val result = getPlayerAction(player, _ => false, _ => false).unsafeRunSync()
    result shouldBe PlayerAction.DrawCard

  test("A user should be asked to split only if his two cards are of the same value"):
    player.addCard(ace)
    player.addCard(ace)
    val simulatedInputs = Iterator("X", "invalid", "P", "D")
    given mockConsole: Console[IO] = mockConsoleWith(() => simulatedInputs.next())
    val result = getPlayerAction(player,  _ => true, _ => true).unsafeRunSync()
    result shouldBe PlayerAction.Split


