package model


object ParticipantModule:

  trait Participant:
    /** The name of the participant. */
    def name: String

    /** The cards in the hand of a player. */
    def cards: List[Int] //TODO cambiare da Int a Card quando Card è implementato

    /** Adds a card to the list of cards of a participant */
    def addCard(card: Int): Unit//TODO cambiare da Int a Card

    /** The score of a participant during a hand. */
    def score: Int = Participant.calculateScore(cards)
    
    override def toString: String = s"[$name] ${cards.mkString(", ")} "
    
  object Participant:
    def calculateScore(cards: List[Int]): Int = cards.sum
      