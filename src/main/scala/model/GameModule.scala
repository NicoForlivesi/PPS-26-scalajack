package model

import PlayerModule.*
import model.PlayerModule.PlayerState.{LeftGame}

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

    private class GameImpl(private var currentPlayers: List[Player],
                           override var currentBets: List[Bet]) extends Game:

      def players: List[Player] = currentPlayers

      override def isOver(): Boolean = currentPlayers match
        case Nil  => true
        case _    => currentPlayers.forall(player => player.state == LeftGame)

      override def removePlayer(targetPlayer: Player): Unit =
        currentPlayers = currentPlayers.filterNot(player => player == targetPlayer)





