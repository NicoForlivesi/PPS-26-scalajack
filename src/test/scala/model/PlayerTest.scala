package model

import model.PlayerModule.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*


class PlayerTest extends AnyFunSuite with BeforeAndAfterEach:

  val startingAmount = 50
  val name = "gigi"
  // veriabile condivisa dai test
  var player: Player = _

  // Questo blocco viene eseguito prima di ciascun singolo test
  override def beforeEach(): Unit =
    player = Player(name, startingAmount)

  test("starting player's state should be always Active"):
    player.state shouldBe PlayerState.Active

  test("starting player's balance should be the exact amount defined by the user"):
    player.balance.totalValue shouldBe startingAmount

  test("player's name should be the one decided by the user"):
    player.name shouldBe name

  test("player's state change should work as expected"):
    player.bust()
    player.state shouldBe PlayerState.Busted
    player.stand()
    player.state shouldBe PlayerState.Standing
    player.leaveTable()
    player.state shouldBe PlayerState.LeftGame

  test("the withdraw method should work has expected when the bet amount is valid"):
    val bet = 45
    val invalidBet = 55
    player.withdraw(bet) shouldBe true
    player.balance.totalValue shouldBe startingAmount - bet

  test("the withdraw method should work has expected when the bet amount is not valid"):
    val invalidBet = 55
    player.withdraw(invalidBet) should not be true
    player.balance.totalValue shouldBe startingAmount