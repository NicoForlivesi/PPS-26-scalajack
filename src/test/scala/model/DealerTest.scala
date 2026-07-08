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

  test("The dealer should be considered busted only when its score's minValue exceeds 21"):
    // Scenario 1: (minValue = 6, maxValue = 16)
    dealer.addCard(Card(Suit.Hearts, Value.Ace))
    dealer.addCard(Card(Suit.Spades, Value.Five))
    dealer.isBusted() shouldBe false
    // Scenario 2: (minValue = 16, maxValue = 26)
    dealer.addCard(Card(Suit.Clubs, Value.King))
    dealer.isBusted() shouldBe false
    // Scenario 3: (minValue = 26, maxValue = 36)
    dealer.addCard(Card(Suit.Diamonds, Value.Ten))
    dealer.isBusted() shouldBe true