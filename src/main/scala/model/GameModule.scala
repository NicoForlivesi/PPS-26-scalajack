package model

import PlayerModule.*

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

  object Game:
    def apply(players: List[Player]): Game = GameImpl(players, List.empty)

    private class GameImpl(private var currentPlayers: List[Player],
                           override var currentBets: List[Bet]) extends Game:

      def players: List[Player] = currentPlayers


