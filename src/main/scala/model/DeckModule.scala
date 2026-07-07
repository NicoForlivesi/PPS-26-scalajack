package model

import scala.util.Random

object DeckModule:

  /** The four suits of a standard deck of playing cards. */
  enum Suit:
    case Hearts, Spades, Clubs, Diamonds

  /** The possible card values in a standard deck. */
  enum Value:
    case Ace
    case Two, Three, Four, Five, Six, Seven, Eight, Nine, Ten
    case Jack, Queen, King

  /** Represents a playing card.
   *
   * @param suit     the suit of the card
   * @param value    the value (rank) of the card
   * @param isFaceUp whether the card is face up
   */
  case class Card(suit: Suit, value: Value, isFaceUp: Boolean = true):

    /** Returns a string representation of the card.
     *
     * If the card is face up, the representation shows its value and suit.
     * Otherwise, the card is hidden.
     *
     * @return the formatted string representation of the card
     */
    override def toString: String =
      if !isFaceUp then
        """┌─────┐
          |│ ??? │
          |└─────┘""".stripMargin
      else
        val suitSymbol = suit match
          case Suit.Hearts   => "♥"
          case Suit.Diamonds => "♦"
          case Suit.Clubs    => "♣"
          case Suit.Spades   => "♠"
        val valueSymbol = value match
          case Value.Ace   => "A"
          case Value.Jack  => "J"
          case Value.Queen => "Q"
          case Value.King  => "K"
          case Value.Ten   => "10"
          case other => other.ordinal + 1 match
            case n => n.toString
        s"""┌─────┐
           |│$valueSymbol ${if valueSymbol.length == 1 then " " else ""}$suitSymbol │
           |└─────┘""".stripMargin

  extension (c: Card)

    /** Returns a copy of the card with its visibility flipped.
     *
     * @return a card with the opposite face-up state
     */
    def flip(): Card =
      //c.copy(state = if c.state == CardState.FaceUp then CardState.FaceDown else CardState.FaceUp)
      c.copy(isFaceUp = !c.isFaceUp)

  /** Represents a deck of playing cards. */
  opaque type Deck = List[Card]

  object Deck:

    /** Generates one or more standard 52-card decks.
     *
     * @param numDeck the number of decks to generate
     * @return a deck containing all generated cards
     */
    def generateDeck(numDeck: Int): Deck =
      require(numDeck > 0)
      val singleDeck = for
        suit <- Suit.values.toList
        value <- Value.values.toList
      yield Card(suit, value)
      List.fill(numDeck)(singleDeck).flatten


    /** Creates a single standard 52-card deck.
     *
     * @return a standard deck
     */
    def standard(): Deck =
      generateDeck(1)

  extension (d: Deck)

    /** Draws the top card from the deck.
     *
     * @return a tuple containing the drawn card, if any, and the remaining deck
     */
    def draw(): (Option[Card], Deck) = d match
      case h :: t => (Some(h), t)
      case Nil => (None, d)

    /** Returns a shuffled copy of the deck.
     *
     * @return a randomly shuffled deck
     */
    def shuffle(): Deck =
      Random.shuffle(d)

    /** Checks whether the deck is empty.
     *
     * @return `true` if the deck contains no cards, `false` otherwise
     */
    def isEmpty(): Boolean =
      d.isEmpty

    /** Returns the number of cards currently in the deck.
     *
     * @return the deck size
     */
    def size(): Int =
      d.size

    /** Returns a list of cards.
     *
     * @return the cards contained in the deck
     */
    def toList(): List[Card] = d
