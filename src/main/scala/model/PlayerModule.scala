package model

import model.FicheModule.Fiche
import ParticipantModule.Participant
import model.DeckModule.Card

object PlayerModule:

  enum PlayerState:
    case Active
    case LeftGame
    case Busted //The player exceeded 21
    case Standing //The player has decided to stop asking for cards

  /** Represents the player wallet during the game. */
  trait Wallet:

    /** The list of fiches currently owned by the player. */
    def balance: List[Fiche]

    /** Deposit a specific amount in the current balance by converting it into fiches.
     *
     * @param amount The value to be deposited.
     */
    def deposit(amount: Double): Unit //Torna l'istanza del giocatore con balance aggiornato

    /** Withdraws the necessary fiches to cover the requested bet amount
     *
     * The method first attempts to withdraw exact value fiches starting from the largest.
     * If an exact combination is not found it attempts to use the smallest available
     * fiche that is greater than the remaining debt, returning the change to the balance.
     *
     * @param amount The total bet amount to be subtracted.
     * @return [[true]] if the player has enough fiches and the withdrawn succeeds, [[false]] otherwise,
     */
    def withdraw(amount: Double): Boolean //Torna l'istanza del giocatore con balance aggiornato

  /** Represents a player at the game table.
   * Manages the player's current balance and state in the game.
   */
  trait Player extends Participant with Wallet:

    /** The state of the player */
    def state: PlayerState

    /** Changes the player's state to `Standing`. */
    def stand(): Unit

    /** Changes the player's state to `Busted`(when exceeding 21). */
    def bust(): Unit

    /** Changes the player's state to `LeftGame`. */
    def leaveTable(): Unit

    /** Resets the player's state to `Active` to start a new round. */
    def startNewRound(): Unit

    /** Prints a player in a format: [NAME] CARDS - STATE */
    override def toString: String = super.toString + s"\nSTATE: $state\n"

  object Player:
    def apply(name: String, balance: Double): Player =
      PlayerImpl(name, balance, PlayerState.Active)

    private class PlayerImpl(override val name: String,
                             val balanceToBeConverted: Double,
                             var initialState: PlayerState) extends Player:

      //private var currentCards: List[Card] = List.empty
      private var currentState = initialState
      private var currentBalance: List[Fiche] = Fiche.fromAmount(balanceToBeConverted)
      
      //override def addCard(card: Card): Unit = currentCards = currentCards :+ card
      
      override def state: PlayerState = currentState
      
      override def stand(): Unit = currentState = PlayerState.Standing
      
      override def bust(): Unit = currentState = PlayerState.Busted
      
      override def leaveTable(): Unit = currentState = PlayerState.LeftGame
      
      override def startNewRound(): Unit =
        currentState = PlayerState.Active
        clearHand()
      
      override def balance: List[Fiche] = currentBalance
      
      override def deposit(amount: Double): Unit =
        require(amount > 0, "deposit amount must be grater than 0")
        currentBalance = currentBalance ::: Fiche.fromAmount(amount)
        
      override def withdraw(amount: Double): Boolean =
        require(amount > 0, "withdraw amount must be grater than 0")
        var hasEnoughFiches = true
        val sortedFiches = currentBalance.sortBy(-_.value) // in modo decrescente (CANCELLARE)
        var (keptFiches, remainingAmount) = sortedFiches.foldLeft((List.empty[Fiche], amount)):
          case ((remainedFiches, leftAmount), fiche) =>
            if leftAmount > 0 && fiche.value <= leftAmount then
              (remainedFiches, leftAmount - fiche.value)
            else
              (remainedFiches :+ fiche, leftAmount)
        if remainingAmount > 0 then
          val sortedFichesAscending = keptFiches.sortBy(-_.value)
          sortedFichesAscending.find(_.value >= remainingAmount) match
            case Some(fiche) =>
              val change = fiche.value - remainingAmount
              keptFiches = keptFiches.diff(List(fiche))
              if change > 0 then
                keptFiches = keptFiches ::: Fiche.fromAmount(change)
            case None => hasEnoughFiches = false
        if hasEnoughFiches then
          currentBalance = keptFiches
        hasEnoughFiches