import model.GameModule.*
import model.PlayerModule.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.funsuite.AnyFunSuite

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


