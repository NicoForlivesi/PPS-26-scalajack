package model

import model.DeckModule.Card


object ParticipantModule:

  trait Participant:
    //Viene già fatta qui l'implementazione dei metodi comuni a tutti i trait che lo estendono(Dealer e Player)
    private var currentCards: List[Card] = List.empty

    /** The name of the participant. */
    def name: String

    /** The cards in the hand of a player. */
    def cards: List[Card] = currentCards

    /** Clears all the cards currently held by the participant. */
    def clearHand(): Unit =
      currentCards = List.empty

    /** Replaces the current cards held by the participant with a new list of cards.
     *
     * @param newCards the new list of cards to assign to the participant.
     */
    protected def setCards(newCards: List[Card]): Unit =
      currentCards = newCards

    /** Adds a card to the list of cards of a participant */
    def addCard(card: Card): Unit =
      currentCards = cards :+ card

    /** The score of a participant during a hand. */
    def score: Int = Participant.calculateScore(cards)

    /** Returns a string representation of the player. */
    override def toString: String =
      val cardsLines: List[Array[String]] = cards.map(_.toString.linesIterator.toArray)
      val topRow = cardsLines.map(lines => lines(0)).mkString("  ")
      val middleRow = cardsLines.map(lines => lines(1)).mkString("  ")
      val bottomRow = cardsLines.map(lines => lines(2)).mkString("  ")
      s"[$name]:\n$topRow\n$middleRow\n$bottomRow"

  object Participant:
    def calculateScore(cards: List[Card]): Int = ??? //TODO cambiare i test
