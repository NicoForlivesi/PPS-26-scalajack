package model

import PlayerModule.*
import model.PlayerModule.PlayerState.LeftGame
import FicheModule.*
import model.DealerModule.*
import model.DeckModule.Card.{CutCard, StandardCard}
import model.DeckModule.{Card, Deck}
import model.ScoreModule.*
import model.ParticipantModule.Participant

import scala.annotation.tailrec

object GameModule:

  /** Represents a bet placed by a player in the game.
   *
   * @param player The Player who is placing the bet.
   * @param amount The amount of coins wagered by the player.
   */
  case class Bet(player: Player, amount: Int)

  trait Game:

    /** Returns the list of players currently participating in the game.
     *
     * @return the list of players in the game.
     */
    def players: List[Player]

    /** Returns the dealer of the game.
     *
     * @return The dealer currently participating in the game.
     */
    def dealer: Dealer

    /** Returns the current deck of the game */
    def deck: Deck

    /** Updates the current deck, method useful for tests
     *
     * @param deck The new deck
     */
    def deck_=(deck: Deck): Unit

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

    /** Returns the players who got a BJ right after the initial deal (two cards with sum equal to 21).
     *
     * @return the list of players who have blackjack in the current round (can be empty).
     */
    def playersWithBlackjack(): List[Player]

    /** Handles every player in the given list who got a blackjack: credits their balance with their
     *  current bet multiplied by the blackjack payout, and updates their state to `Blackjack`.
     *
     * @param winners The players to handle for their blackjack.
     */
    def handleBlackjacks(winners: List[Player]): Unit

    /** Checks if the game is over, meaning that every starting player has already left the table.
     *
     * @return [[true]] if all players have left, [[false]] if there are still active players.
     */
    def isOver: Boolean

    /** Removes a specified player from the game.
     *
     * @param player is the one that need to be removed from the game.
     */
    def removePlayer(player: Player): Unit

    /** Checks whether a specified player is busted and updates its state accordingly.
     *
     * @param player The player to be checked.
     * @return true if the player is busted, false otherwise.
     */
    def evaluatePlayerBust(player: Player): Boolean

    /** Checks whether the dealer is busted
     *
     * @param dealer The dealer
     * @return true if the dealer is busted, false otherwise
     */
    def evaluateDealerBust(dealer: Dealer): Boolean

    /** Draws cards from the deck until a standard card is drawn. Each card
     * drawn is removed from the deck.
     * 
     * @return An Optional containing the standard card drawn from the desk if not empty
     */
    def drawStandardCard(): Option[StandardCard] 
    
    /** Draws a standard card from the desk and adds it to the player's list of cards.
     *
     * @param participant The participant asking for a new card
     * @return An Optional containing the standard card drawn from the desk if not empty
     */
    def drawCard(participant: Participant): Option[Card]

    /** Checks whether the cut card is still in deck
     * 
     * @return [[true]] it is, [[false]] otherwise
     */
    def isCutCardInDeck: Boolean
    
    /** Executes the dealer's turn according to blackjack rules.
     *
     * The dealer first reveals the hidden card and then repeatedly draws cards
     * until reaching a score of at least 17. The dealer does not make choices:
     * cards are automatically drawn following the game rules.
     *
     * @return a list containing the value to print in standard output for every card that has been distributed
     */
    def computeDealerTurn(): List[String]

    /** Prepares the game for a new hand: resets every remaining player's state
    * to `Active` and clears their hand, clears the dealer's hand, and — if the
    * cut card was reached during the previous hand — reshuffles a fresh deck
    * sized for the current number of participants.
    */
    def startNewHand(): Unit

  object Game:

    def apply(players: List[Player]): Game =
      val numParticipants = players.size + 1
      GameImpl(players, List.empty, Deck.standard(numParticipants).shuffle(numParticipants))

    def apply(players: List[Player], deck: Deck): Game = GameImpl(players, List.empty, deck)

    def isInitialDepositValid(amount: Double): Boolean = amount > 0 && amount % Fiche.smallestDenomination == 0

    private class GameImpl(private var currentPlayers: List[Player],
                           override var currentBets: List[Bet],
                           private var currentDeck: Deck) extends Game:
      
      private val BlackjackPayoutMultiplier = 2.5
      private val minBet: Double = Fiche.Five.value
      private val initialNumParticipants: Int = players.size + 1
      private val gameDealer: Dealer = Dealer()
      private var cutCardInDeck: Boolean = true

      def players: List[Player] = currentPlayers

      override def dealer: Dealer = gameDealer

      override def deck: Deck = currentDeck

      override def deck_=(deck: Deck): Unit = currentDeck = deck

      override def isBetValid(player: Player)(amount: Double): Boolean =
        amount > 0 && amount % minBet == 0 && amount <= player.balance.totalValue

      override def distributeCards(): List[String] =
        def distributeCards_(participants: List[Participant], faceUp: Boolean = true): List[String] =
          participants.flatMap(participant =>
            drawStandardCard() match
              case Some(card: StandardCard) =>
                // sennò è molto probabile che una mano rimarebbe a metà (CONTROLLER)
                if participant.isInstanceOf[Dealer] && !faceUp then
                  participant.addCard(card.flip())
                else
                 participant.addCard(card) 
                List(participant.toString)
              case _                        => List.empty
          )
        val participants: List[Participant] = players :+ gameDealer
        val firstRound = distributeCards_(participants)
        val secondRound = distributeCards_(participants, faceUp = false) //Aggiunto il fatto che la seconda carta del banco è coperta
        firstRound ::: secondRound

      override def playersWithBlackjack(): List[Player] =
        currentPlayers.filter(player => player.cards.isBlackjack)

      override def handleBlackjacks(winners: List[Player]): Unit =
        winners.foreach(playerWithBJ =>
          val bet = currentBets.find(_.player == playerWithBJ).get
          playerWithBJ.deposit(bet.amount * BlackjackPayoutMultiplier)
          playerWithBJ.winBlackjack()
        )

      override def isCutCardInDeck: Boolean = cutCardInDeck
      
      override def isOver: Boolean = currentPlayers match
        case Nil  => true
        case _    => currentPlayers.forall(player => player.state == LeftGame)

      override def removePlayer(targetPlayer: Player): Unit =
        currentPlayers = currentPlayers.filterNot(player => player == targetPlayer)
        targetPlayer.leaveTable()

      override def evaluatePlayerBust(player: Player): Boolean =
        val busted = player.cards.isBusted
        if busted then player.bust()
        busted

      override def evaluateDealerBust(dealer: Dealer): Boolean = dealer.cards.isBusted

      override def drawStandardCard(): Option[StandardCard] =
        val (optCard, newDeck) = deck.draw()
        currentDeck = newDeck
        optCard match
          case Some(card: StandardCard) => Some(card)
          case Some(CutCard)            => 
            cutCardInDeck = false
            drawStandardCard()
          case _                        => None

      override def drawCard(participant: Participant): Option[StandardCard] =
        drawStandardCard().map: card =>
          participant.addCard(card)
          card
          
      override def computeDealerTurn(): List[String] =
        @tailrec
        def extractUntilSeventeen(messages: List[String]): List[String] =
          if dealer.hasFinishedTurn then messages
          else
            drawCard(dealer) match
              case Some(card) =>
                val updatedMessages = messages :+ s"A new card will be dealt to the dealer:\n" + s"${card.toString}" + s"\n${dealer.toString}"
                extractUntilSeventeen(updatedMessages)
              case _ => List.empty
        var messages = List.empty[String]
        dealer.revealCards()
        messages = messages :+ dealer.toString
        extractUntilSeventeen(messages)

      override def startNewHand(): Unit =
        currentPlayers.foreach(_.prepareForNewHand())
        gameDealer.clearHand()










