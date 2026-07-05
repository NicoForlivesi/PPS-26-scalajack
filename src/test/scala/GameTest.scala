import model.GameModule.*
import model.PlayerModule.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.funsuite.AnyFunSuite
import org.mockito.Mockito;

class GameTest extends AnyFunSuite with BeforeAndAfterEach{

  var firstPlayer: Player = _
  var secondPlayer: Player = _
  var listPlayers: List[Player] = _
  var game: Game = _
  val betAmount = 100

  override def beforeEach(): Unit =
    firstPlayer = Mockito.mock(classOf[Player])
    secondPlayer = Mockito.mock(classOf[Player])
    listPlayers = List(firstPlayer, secondPlayer)
    game = Game(listPlayers)


  test("player's bet is computed correctly"):
    val name = "gigi"
    Mockito.when(firstPlayer.name).thenReturn(name)
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


}
