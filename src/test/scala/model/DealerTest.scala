package model

import model.DealerModule.*
import model.DeckModule.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

class DealerTest extends AnyFunSuite:

  val dealer = Dealer()

  test("The dealer should have zero profit initially"):
    val dealer = Dealer()
    dealer.name shouldBe "Dealer"
    dealer.totalProfit shouldBe 0.0

  test("The dealer should increase its profit when adding money"):
    val amountFirstHand = 50.0
    val amountSecondHand = 25.0
    dealer.addProfit(amountFirstHand)
    dealer.totalProfit shouldBe amountFirstHand
    dealer.addProfit(amountSecondHand)
    dealer.totalProfit shouldBe amountFirstHand + amountSecondHand

  test("The dealer should reveal all hidden cards"):
    val hiddenCard = Card(Suit.Hearts, Value.Ace, isFaceUp = false)
    val visibleCard = Card(Suit.Spades, Value.King)
    dealer.addCard(hiddenCard)
    dealer.addCard(visibleCard)
    dealer.revealCards()
    dealer.cards.count(_.isFaceUp ) shouldBe 2