package model

import model.DeckModule.Card


object ParticipantModule:

  trait Participant:
    /** The name of the participant. */
    def name: String

    /** The cards in the hand of a player. */
    def cards: List[Card]

    /** Adds a card to the list of cards of a participant */
    def addCard(card: Card): Unit

    /** The score of a participant during a hand. */
    def score: Int = Participant.calculateScore(cards)

    override def toString: String =
      val cardsLines: List[Array[String]] = cards.map(_.toString.linesIterator.toArray)
      val topRow = cardsLines.map(lines => lines(0)).mkString("  ")
      val middleRow = cardsLines.map(lines => lines(1)).mkString("  ")
      val bottomRow = cardsLines.map(lines => lines(2)).mkString("  ")
      s"[$name]:\n$topRow\n$middleRow\n$bottomRow"

  object Participant:
    def calculateScore(cards: List[Card]): Int = ??? //TODO cambiare i test
