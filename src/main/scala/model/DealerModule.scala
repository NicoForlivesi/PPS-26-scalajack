package model

import model.ParticipantModule.Participant

object DealerModule:

  trait Dealer extends Participant:

    /** Returns the total profit earned by the dealer during the game.
     *
     * @return the total amount of money won by the dealer.
     */
    def totalProfit: Double

    /** Reveals all the dealer's cards that are currently face down.
     */
    def revealCards(): Unit

    /** Adds an amount to the dealer's total profit.
     *
     * @param amount the amount of money to add to the dealer's profit.
     */
    def addProfit(amount: Double): Unit

    override def toString: String = super.toString + "\n"

  object Dealer:
    def apply(): Dealer = DealerImpl()

    private class DealerImpl() extends Dealer:
      private var profit: Double = 0.0

      override def name: String = "Dealer"

      override def totalProfit: Double = profit

      override def addProfit(amount: Double): Unit =
        profit += amount

      override def revealCards(): Unit =
        setCards(cards.map(card => if !card.isFaceUp then card.flip() else card))



