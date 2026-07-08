package model

import model.DeckModule.{Card, Suit, Value}
import model.ScoreModule.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

class ScoreTest extends AnyFunSuite:

  private def card(value: Value): Card = Card(Suit.Hearts, value)

  test("Score.toString shows a single value when min and max coincide"):
    Score(16, 16).toString shouldBe "16"

  test("Score.toString shows both readings when they differ and the high one does not bust"):
    Score(7, 17).toString shouldBe "7 / 17"

  test("Score.toString shows only the low value when the high one busts"):
    Score(13, 23).toString shouldBe "13"

  test("Score.toString shows only the Blackjack value when there is 2 valid value"):
    Score(11, 21).toString shouldBe "21"

  test("calculateScore returns zero for an empty hand"):
    calculateScore(List.empty) shouldBe Score(0, 0)

  test("calculateScore sums cards as they are when there is no aces"):
    calculateScore(List(card(Value.Ten), card(Value.Six))) shouldBe Score(16, 16)

  test("calculateScore correctly converts jacks, queens and kings to 10"):
    calculateScore(List(card(Value.King), card(Value.Queen))) shouldBe Score(20, 20)

  test("calculateScore reads a single ace as 1 or 11 when the high option does not bust"):
    calculateScore(List(card(Value.Ace), card(Value.Six))) shouldBe Score(7, 17)

  test("calculateScore correctly reports both values also if the maximum bust"):
    calculateScore(List(card(Value.Ace), card(Value.Nine), card(Value.Five))) shouldBe Score(15, 25)

  test("calculateScore never raises more than one ace"):
    calculateScore(List(card(Value.Ace), card(Value.Ace), card(Value.Nine))) shouldBe Score(11, 21)

  test("calculateScore keeps the high value uncapped even when it busts, with multiple aces"):
    calculateScore(List(card(Value.Ace), card(Value.Ace), card(Value.Ace), card(Value.King))) shouldBe Score(13, 23)

  test("calculateScore reports the same value for min and max when the hand with no ace already busts"):
    calculateScore(List(card(Value.Ten), card(Value.Queen), card(Value.Three))) shouldBe Score(23, 23)

  test("isBusted is true when the value is grater than 21"):
    isBusted(List(card(Value.Ten), card(Value.Queen), card(Value.Two))) shouldBe true

  test("isBusted is false when the hand is exactly 21"):
    isBusted(List(card(Value.Ace), card(Value.Ten))) shouldBe false

  test("isBusted is true when even the low reading of an ace hand exceeds 21"):
    isBusted(List(card(Value.Ace), card(Value.Ten), card(Value.Queen), card(Value.Two))) shouldBe true

  test("isBlackjack is true for an ace and a ten-value card"):
    isBlackjack(List(card(Value.Ace), card(Value.Queen))) shouldBe true

  test("isBlackjack is false when the total value is not 21"):
    isBlackjack(List(card(Value.Ten), card(Value.Nine))) shouldBe false

  test("isBlackjack is false for three cards even if the sum is 21"):
    isBlackjack(List(card(Value.Ace), card(Value.Five), card(Value.Five))) shouldBe false