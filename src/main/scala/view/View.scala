package view

import cats.effect.IO
import cats.effect.std.Console
import model.PlayerModule.Player

object View:

  /** Interactively prompts the user to enter the number of players in the match.
   *
   * @param console the contextual [[cats.effect.std.Console]] instance used to perform
   *                pure and testable I/O operations.
   * @return an [[cats.effect.IO]] encapsulating the computation that yields the
   *         initial number of players in the game as a [[Int]].
   */
  def getNumPlayers(using console: Console[IO]): IO[Int] =
    for
      _ <- console.println("Please insert the number of players in the game:")
      input <- console.readLine
      numPlayers <- input.toIntOption match
        case Some(count) if count > 0 =>
          for
            _ <- console.println(s"Perfect! The game will start with $count players.")
          yield count
        case _ =>
          for
            _          <- console.println("Sorry, the number of players must be a valid number greater than 0")
            retryCount <- getNumPlayers
          yield retryCount
    yield numPlayers

  /** Interactively prompts the user to enter their ID.
   *
   * This method prints a welcome message and an ID prompt to the terminal,
   * then suspends the execution until the user provides an input via standard input.
   *
   * @param console the contextual [[cats.effect.std.Console]] instance used to perform
   *                pure and testable I/O operations.
   * @return a [[cats.effect.IO]] encapsulating the computation that yields the player's ID
   *         as a [[String]] upon evaluation.
   */
  def getPlayerID(using console: Console[IO]): IO[String] =
    for
      _        <- console.println("Welcome to the game!")
      _        <- console.println("Please insert your ID below:")
      playerID <- console.readLine
      _        <- console.println(s"Your ID $playerID has been correctly added!")
    yield playerID

  /** Interactively prompts the user to enter their initial playing balance.
   *
   * This method prints a prompt to the terminal and reads the user's input.
   * If the input is not a valid positive number, it prints an error message
   * and recursively prompts the user again until a valid balance is provided.
   *
   * @param console the contextual [[cats.effect.std.Console]] instance used to perform
   *                pure and testable I/O operations.
   *
   * @return an [[cats.effect.IO]] encapsulating the computation that yields the
   *         initial balance as a [[Int]].
   */
  def getInitialBalance(using console: Console[IO]): IO[Int] =
    for
      _       <- console.println("Please insert your initial balance in € below: ")
      input   <- console.readLine
      balance <- input.toIntOption match
        case Some(amount) if amount > 0 =>
          for
            _ <- console.println(s"Your balance of €$amount has been correctly added! Now it will be converted in fiches.")
          yield amount
        case _ =>
          for
            _            <- console.println("Sorry, your input is not valid!")
            retryBalance <- getInitialBalance
          yield retryBalance
    yield balance

  /** Interactively prompts a player to enter their bet for the upcoming hand via the console.
   *
   * This method displays the player's current balance, validates the input
   * and recursively prompts the user again if the input is invalid until a correct amount is provided.
   *
   * @param player  The [[Player]] who is placing the bet.
   * @param console The implicit [[Console]] instance used to handle terminal I/O.
   * @return A [[cats.effect.IO]] that, when evaluated, contains the valid
   *         bet amount.
   */
  def getBet(player: Player)(using console: Console[IO]): IO[Int] =
    for
      _        <- console.println(s"Your actual balance is ${player.balance.sum} fiches.")
      _        <- console.println("Please insert your bet for the upcoming hand!")
      bet      <- console.readLine
      validBet <- bet.toIntOption match
        case Some(amount) if amount > 0 && amount <= player.balance.sum =>
          for
            _ <- console.println(s"Your bet of $amount fiches has been correctly added!")
          yield amount
        case _ =>
          for
            _        <- console.println("Sorry, your input is not valid!")
            retryBet <- getBet(player)
          yield retryBet
    yield validBet
