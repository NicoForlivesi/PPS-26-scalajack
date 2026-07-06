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
    promptForValidInt(
      initialMessage = "Please insert the number of players in the game:",
      parser = _.toIntOption,
      isValidCondition = number => number > 0, // For the player count, being greater than 0 is sufficient
      successMessage = count => s"Perfect! The game will start with $count players.",
      errorMessage = "Sorry, the number of players must be a valid number greater than 0",
      )

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
    promptForValidInt(
      initialMessage = "Please insert your initial balance in € below: ",
      parser = _.toIntOption,
      isValidCondition = amount => amount > 0,
      successMessage = amount => s"Your balance of €$amount has been correctly added! Now it will be converted in fiches.",
      errorMessage = "Sorry, your input is not valid!"
    )

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
    val totalBalance = player.balance.sum
    promptForValidInt(
      initialMessage = s"Your actual balance is $totalBalance fiches.\nPlease insert your bet for the upcoming hand!",
      parser = _.toIntOption,
      isValidCondition = betAmount => betAmount <= totalBalance && betAmount > 0,
      successMessage = betAmount => s"Your bet of $betAmount fiches has been correctly added!",
      errorMessage = s"Sorry, your input is not valid or exceeds your current balance ($totalBalance fiches)!"
    )

  /** Prompts the player to decide whether they want to leave the current game session.
   *
   * This method interacts with the user via the console, displaying a choice prompt and
   * waiting for a valid confirmation. The input is case-insensitive (accepts both lowercase
   * and uppercase) and will recursively prompt the player until either 'Y' or 'N' is entered.
   *
   * @param player  The [[Player]] currently being asked to make a decision.
   * @param console The contextual console capability required to perform I/O operations.
   * @return An [[cats.effect.IO]] containing the validated string ("Y" or "N") representing
   *         the player's final decision.
   */
  def getLeaveChoice(player: Player)(using console: Console[IO]): IO[String] =
    promptForValidInt(
      initialMessage = "Do you wish to leave the game? Type 'Y' if yes, 'N' if no.",
      parser = input => Some(input.toUpperCase().trim),
      isValidCondition = input => input.equals("Y") || input.equals("N"),
      successMessage = choice => s"Your choice $choice has been correctly registered!",
      errorMessage = "Sorry, your input is not valid."
    )

  /** Helper method to handle reading from the console, validation with a custom predicate, and recursive retry.
   *
   * @param initialMessage   The text instructions explaining to the user what to enter.
   * @param successMessage   A function that generates the text to display when the input is valid.
   * @param errorMessage     The text to display if the input fails verification.
   * @param isValidCondition The validation predicate function that the parsed integer must satisfy.
   */
  private def promptForValidInt[T](
                        initialMessage: String,
                        parser: String => Option[T],
                        isValidCondition: T => Boolean,
                        successMessage: T => String,
                        errorMessage: String
  )(using console: Console[IO]): IO[T] =
    for
      _          <- console.println(initialMessage)
      input      <- console.readLine
      finalValue <- parser(input) match
        case Some(parsedValue) if isValidCondition(parsedValue) =>
          for
            _ <- console.println(successMessage(parsedValue))
          yield parsedValue
        case _ =>
          for
            _          <- console.println(errorMessage)
            retryValue <- promptForValidInt(initialMessage, parser, isValidCondition, successMessage, errorMessage)
          yield retryValue
    yield finalValue