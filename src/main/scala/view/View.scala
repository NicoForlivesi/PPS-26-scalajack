package view

import cats.effect.IO
import cats.effect.std.Console
import model.PlayerModule.Player

object View:

  /** Validates a raw string input by attempting to parse it into a value of type `A`
   * and then checking it against a given predicate.
   *
   * @param input     the raw user input as a string
   * @param parser    a function that attempts to convert the string into a value of type `A`
   *                  (returns `None` if parsing fails)
   * @param predicate a condition that the parsed value must satisfy
   * @tparam A the target type of the parsed and validated value
   * @return Some(value) if parsing succeeds and the predicate condition matches, None otherwise
   */
  private def isInputValid[A](input: String, parser: String => Option[A], predicate: A => Boolean): Option[A] =
    parser(input).filter(predicate)

  /** Repeatedly prompts the user until a valid input is provided.

   * @param prompt    the message displayed to request input from the user
   * @param error     the message displayed when the input is invalid
   * @param parser    function used to parse the input string into a value of type `A`
   * @param predicate condition that the parsed value must satisfy
   * @tparam A the type of the validated result
   * @return an IO effect producing a valid value of type `A`
   */
  private def askUntilValidPrompt[A](prompt: String,
                                     error: String,
                                     parser: String => Option[A],
                                     predicate: A => Boolean)(using console: Console[IO]): IO[A] =
    for
      _ <- console.println(prompt)
      input <- console.readLine
      value <- isInputValid(input, parser, predicate) match
        case Some(v) => IO.pure(v)
        case None =>
          for
            _          <- console.println(error)
            retryValue <- askUntilValidPrompt(prompt, error, parser, predicate)
          yield retryValue
    yield value


  /** Interactively prompts the user to enter the number of players in the match.
   *
   * @param console the contextual [[cats.effect.std.Console]] instance used to perform
   *                pure and testable I/O operations.
   * @return an [[cats.effect.IO]] encapsulating the computation that yields the
   *         initial number of players in the game as a [[Int]].
   */
  def getNumPlayers(using console: Console[IO]): IO[Int] =
    for
      numPlayers <- askUntilValidPrompt(
        prompt = "Please insert the number of players in the game:",
        error = "Sorry, the number of players must be a valid number greater than 0",
        parser = _.toIntOption,
        predicate = _ > 0
      )
      _ <- console.println(s"Perfect! The game will start with $numPlayers players.")
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
      _        <- console.println("Please insert your ID below: ")
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
    balance <- askUntilValidPrompt(
        prompt = "Please insert your initial balance in € below: ",
        error = "Sorry, your input is not valid!",
        parser = _.toIntOption,
        predicate = _ > 0
      )
      _ <- console.println(s"Your balance of €$balance has been correctly added! Now it will be converted in fiches.")
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
  def getBet(player: Player)(using console: Console[IO]): IO[Int] = {
    for
      _        <- console.println(s"[${player.name}]Your actual balance is ${player.balance.sum} fiches.")
      _        <- console.println(s"[${player.name}]Please insert your bet for the upcoming hand!")
      betAmount <- askUntilValidPrompt(
        prompt = s"[${player.name}]Please insert your bet for the upcoming hand!",
        error = "Sorry, your input is not valid!",
        parser = _.toIntOption,
        predicate = (bet: Int) => bet > 0 && bet <= player.balance.sum
      )
      _ <- console.println(s"Your bet of $betAmount fiches has been correctly added!")
    yield betAmount

  }
