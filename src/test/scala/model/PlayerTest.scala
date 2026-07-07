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

  test("the deposit method should increase the player's balance by the deposited amount"):
    val depositAmount = 20
    player.deposit(depositAmount)
    player.balance.totalValue shouldBe startingAmount + depositAmount

  test("the withdraw method should work has expected when the bet amount is valid"):
    val bet = 45
    val invalidBet = 55
    player.withdraw(bet) shouldBe true
    player.balance.totalValue shouldBe startingAmount - bet

  test("the withdraw method should work has expected when the bet amount is not valid"):
    val invalidBet = 55
    player.withdraw(invalidBet) should not be true
    player.balance.totalValue shouldBe startingAmount

//  test("adding cards to the player should correctly update their hand"):
//    player.cards shouldBe List.empty
//    player.addCard(10)
//    player.addCard(11)
//    player.cards shouldBe List(10, 11)
//
//  test("player's initial score with no cards should be 0"):
//    player.score shouldBe 0
//
//  test("player's score should be the sum of the cards in hand"):
//    player.addCard(7)
//    player.addCard(9)
//    player.score shouldBe 16
//
//  test("player's score should update dynamically as new cards are added"):
//    player.addCard(10)
//    player.score shouldBe 10
//
//    player.addCard(5)
//    player.score shouldBe 15
//
//    player.addCard(3)
//    player.score shouldBe 18
//
//  test("player's score should reset or be empty when starting a new round"):
//    player.addCard(10)
//    player.addCard(8)
//    player.score shouldBe 18
//    player.startNewRound()
//    player.score shouldBe 0