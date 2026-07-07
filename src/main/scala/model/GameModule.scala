package model

import PlayerModule.*
import model.PlayerModule.PlayerState.LeftGame
import FicheModule.*
import model.DeckModule.Deck
import model.ParticipantModule.Participant

object GameModule:

  /** Represents a bet placed by a player in the game.
   *
   * @param player The Player who is placing the bet.
   * @param bet The amount of coins wagered by the player.
   */
  case class Bet(player: Player, bet: Int)

  trait Game:

    /** Returns the list of players currently participating in the game.
     *
     * @return the list of players in the game.
     */
    def players: List[Player]

    //TODO def dealer: Dealer

    /** Returns the current deck of the game */
    def deck: Deck

    /** Returns the list of bets placed during the current round.
     *
     * @return The current list of bets.
     */
    def currentBets: List[Bet]

    /** Updates or replaces the list of bets for the current round.
     *
     * @param bets The new list of player's bets to be applied to the game.
     */
    def currentBets_=(bets: List[Bet]): Unit

    /** Checks if a given bet is valid.
     *
     * @param player The player making the bet.
     * @param amount The amount that he wants to bet
     * @return [[true]] if the bet is valid, [[false]] if it is not.
     */
    def isBetValid(player: Player)(amount: Double): Boolean

    /** Distributes two cards to each player, one card at a time
     *
     * @return a list containing the value to print in standard output for every card that has been distributed
     */
    def distributeCards(): List[String]

    /** Checks if the game is over, meaning that every starting player has already left the table.
     *
     * @return [[true]] if all players have left, [[false]] if there are still active players.
     */
    def isOver(): Boolean

    /** Removes a specified player from the game.
     *
     * @param player is the one that need to be removed from the game.
     */
    def removePlayer(player: Player): Unit

  object Game:

    def apply(players: List[Player]): Game = GameImpl(players, List.empty)

    def isInitialDepositValid(amount: Double): Boolean = amount > 0 && amount % Fiche.smallestDenomination == 0

    private class GameImpl(private var currentPlayers: List[Player],
                           override var currentBets: List[Bet]) extends Game:

      private val minBet: Double = Fiche.Five.value
      private var currentDeck: Deck = Deck.standard()
      override def deck: Deck = currentDeck

      //TODO aggiungere al game l'istanza del dealer -  private val dealer: Dealer = ???

      def players: List[Player] = currentPlayers

      //TODO getter per il dealer

      override def isBetValid(player: Player)(amount: Double): Boolean =
        amount > 0 && amount % minBet == 0 && amount <= player.balance.totalValue

      override def distributeCards(): List[String] =
        def distributeCards_(participants: List[Participant]): List[String] =
          participants.flatMap(participant =>
            val (optCard, newDeck) = deck.draw()
            optCard match
              case Some(card) =>
                participant.addCard(card)
                currentDeck = newDeck
                List(participant.toString)
              case _ =>
                //TODO cosa fare se il mazzo è vuoto - GESTIONE FINE PARTITA
                List.empty
          )
        currentDeck = deck.shuffle()
        val firstRound = distributeCards_(players) //TODO aggiungere il dealer alla lista dei partecipanti
        val secondRound = distributeCards_(players)
        firstRound ::: secondRound

      override def isOver(): Boolean = currentPlayers match
        case Nil  => true
        case _    => currentPlayers.forall(player => player.state == LeftGame)

      override def removePlayer(targetPlayer: Player): Unit =
        currentPlayers = currentPlayers.filterNot(player => player == targetPlayer)
        targetPlayer.leaveTable()





