package model

import model.DeckModule.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

class DeckTest extends AnyFunSuite with BeforeAndAfterEach:

  val card = Card(Suit.Hearts, Value.Queen)
  val deck = Deck.standard()
  val numSuits = Suit.values.length
  val numValues = Value.values.length
  val shuffledDeck = deck.shuffle()

  test("A card can be flipped when requested to be shown"):
    card.isFaceUp shouldBe false
    card.toString shouldBe
      """┌─────┐
        |│ ??? │
        |└─────┘""".stripMargin

    val flippedCard = card.flip()
    flippedCard.isFaceUp shouldBe true
    flippedCard.toString shouldBe
      """┌─────┐
        |│Q  ♥ │
        |└─────┘""".stripMargin

  test("The standard deck should have the correct number of cards (52)"):
    deck.isEmpty() shouldBe false
    deck.size() shouldBe numSuits * numValues

  test("The standard deck should contain the correct number of cards for each suit"):
    for cardType <- List(Suit.Hearts, Suit.Clubs, Suit.Spades, Suit.Diamonds) do
      deck.toList().count(c => c.suit == cardType) shouldBe numValues

  test("No cards inside the deck should be face up"):
    deck.toList().filter(_.isFaceUp) shouldBe empty

  test("shuffleDeck method should only change order of cards without other side-effects (duplicate or remove cards)"):
    shuffledDeck.size() shouldBe deck.size()
    shuffledDeck.toList().toSet shouldBe deck.toList().toSet

  test("Drawing from a non-empty deck should return the first card and the remaining deck"):
    val expectedCard = shuffledDeck.toList().head
    val (drawnCard, remainingDeck) = shuffledDeck.draw()
    val deckInitialSize = shuffledDeck.size()

    drawnCard.isDefined shouldBe true
    drawnCard.get shouldBe expectedCard
    remainingDeck.size() shouldBe deckInitialSize - 1
    remainingDeck.toList() shouldBe shuffledDeck.toList().tail
    remainingDeck.toList().head should not be drawnCard.get
    remainingDeck.toList().filter(c => c == drawnCard.get) shouldBe empty

  test("Drawing from an empty deck should return None and an empty deck"):
    val emptiedDeck = emptyDeck(deck)
    val (drawnCard, remainingDeck) = emptiedDeck.draw()

    drawnCard shouldBe None
    remainingDeck.isEmpty() shouldBe true
    remainingDeck.size() shouldBe 0

  test("generateDeck should create a deck composed of multiple standard decks"):
    val numDecks = 3
    val multipleDeck = Deck.generateDeck(numDecks)
    val expectedSize = numSuits * numValues * 3

    multipleDeck.toList().size shouldBe expectedSize
    val standardCards = for
        suit <- Suit.values.toList
        value <- Value.values.toList
    yield Card(suit, value)

    for expectedCard <- standardCards do
      val cardCount = multipleDeck.toList().count(c => c.suit == expectedCard.suit && c.value == expectedCard.value)
      cardCount shouldBe numDecks

  @annotation.tailrec
  final def emptyDeck(deck: Deck): Deck =
    deck.draw() match
      case (None , remainingDeck) =>
        remainingDeck
      case (_, remainingDeck) =>
        emptyDeck(remainingDeck)






