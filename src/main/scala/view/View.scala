package view

import cats.effect.IO
import cats.effect.std.Console
import model.FicheModule.Fiche
import model.PlayerModule.Player
import view.View.Command.{CardsDistribution, DealerBusted, DealerTurn, PlayerTurn, RemovePlayer, ShowBlackJack, ShowBusted, ShowCard, ShowCutCard}

object View:

  //possible inputs from the users
  object Choices:
    val Yes = "Y"
    val No = "N"

  //Penso che forse per le azioni del giocatore sia meglio una enum per rendere il controller in grado di distinguerle con pattern matching in modo intuitivo
  enum PlayerAction:
    case DrawCard
    case Stand

  //Comandi che vengono passati alla funzione 'renderMessage' dal controller per renderizzare date stringhe
  enum Command:
    case CardsDistribution
    case ShowCard(card: String)
    case ShowBlackJack(player: Player)
    case PlayerTurn(name: String)
    case DealerTurn()
    case DealerBusted
    case ShowBusted(player: Player)
    case ShowCutCard
    case RemovePlayer(name: String)

  /** Interactively prompts the user to enter the number of players in the match.
   *
   * @param console the contextual [[cats.effect.std.Console]] instance used to perform
   *                pure and testable I/O operations.
   * @return an [[cats.effect.IO]] encapsulating the computation that yields the
   *         initial number of players in the game as a [[Int]].
   */
  def getNumPlayers(using console: Console[IO]): IO[Int] =
    promptUntilValid(
      prompt = "Please insert the number of players in the game:",
      parser = _.toIntOption,
      predicate = number => number > 0, // For the player count, being greater than 0 is sufficient
      successMessage = count => s"Perfect! The game will start with $count players.\n",
      errorMessage = "Sorry, the number of players must be a valid number greater than 0.",
    )

  /** Prompts the user to enter all players' names in a single comma-separated string,
   * parsing and validating the input into a list of unique names.
   *
   * This method leverages [[promptUntilValid]] to guarantee that the operation
   * is retried recursively until the user provides an input that satisfies both
   * structural and game-rule criteria (matching the expected count and containing
   * no duplicates).
   *
   * @param numPlayers The exact number of players expected to join the game.
   * @param console    The contextual console capability required to perform I/O operations.
   * @return An [[cats.effect.IO]] wrapping a [[List]] of successfully validated,
   *         trimmed, and unique player names.
   */
  def getPlayersNames(numPlayers: Int)(using console: Console[IO]): IO[List[String]] =
    promptUntilValid(
      prompt = "Please all the players names below, separated by \", \":",
      parser = rawInput =>
        val names = rawInput.split(",").map(_.trim).filter(_.nonEmpty).toList
        Some(names),
      predicate = playersNames => playersNames.length == numPlayers && playersNames.distinct.length == playersNames.length,
      successMessage = _ => "All names have been correctly added!\n",
      errorMessage = s"Sorry, your input is not valid. Either the number of players does not $numPlayers, or there are duplicate/empty names."      
    )

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
  def getInitialDeposit(name: String, isDepositValid: Double => Boolean)(using console: Console[IO]): IO[Double] =
    promptUntilValid(
      prompt = s"$name, please insert your initial balance in € below: ",
      parser = _.toDoubleOption,
      predicate = amount => isDepositValid(amount),
      successMessage = amount => s"Your balance of €$amount has been correctly added! Now it will be converted in fiches.\n",
      errorMessage = "Sorry, your input is not valid!"
    )

  /** Interactively prompts a player to enter their bet for the upcoming hand via the console.
   *
   * This method displays the player's current balance, validates the input
   * and recursively prompts the user again if the input is invalid until a correct amount is provided.
   *
   * @param player  The [[Player]] who is placing the bet.
   * @param isBetValid The method used to validate the input.
   * @param console The implicit [[Console]] instance used to handle terminal I/O.
   * @return A [[cats.effect.IO]] that, when evaluated, contains the valid
   *         bet amount.
   */
  def getBet(player: Player, isBetValid: Int => Boolean)(using console: Console[IO]): IO[Int] =
    val totalBalance = player.balance.totalValue
    promptUntilValid(
      prompt = s"${player.name}, your actual balance is $totalBalance fiches.\nPlease insert your bet for the upcoming hand!",
      parser = _.toIntOption,
      predicate = betAmount => isBetValid(betAmount),
      successMessage = betAmount => s"Your bet of $betAmount fiches has been correctly added!\n",
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
    promptUntilValid(
      prompt = s"${player.name}, do you wish to leave the game? Type 'Y' if yes, 'N' if no.",
      parser = input => Some(input.toUpperCase().trim),
      predicate = input => input.equals(Choices.Yes) || input.equals(Choices.No),
      successMessage = choice => s"Your choice $choice has been correctly registered!\n",
      errorMessage = "Sorry, your input is not valid."
    )

  /** Prompts the player to choose an action during their turn.
   *
   * This method interacts with the player through the console, displaying the available
   * actions and waiting for a valid choice. The input is case-insensitive (accepts both
   * lowercase and uppercase letters) and will recursively prompt the player until either
   * 'D' (draw a card) or 'S' (stand) is entered.
   *
   * @param player  The [[Player]] currently performing their turn.
   * @param console The contextual [[cats.effect.std.Console]] capability required to perform I/O operations.
   * @return An [[cats.effect.IO]] containing the validated [[PlayerAction]] chosen by the player.
   */
  def getPlayerAction(player: Player)(using console: Console[IO]): IO[PlayerAction] =
    promptUntilValid(
      prompt =  s"${player.name}, choose your action: type D to draw a card or S to stand.",
      parser = input =>
        input.trim.toUpperCase match
          case "D" => Some(PlayerAction.DrawCard)
          case "S" => Some(PlayerAction.Stand)
          case _   => None,
      predicate = input => Set(PlayerAction.DrawCard, PlayerAction.Stand).contains(input),
      successMessage =
        case PlayerAction.DrawCard  => "A new card will be dealt to you:"
        case PlayerAction.Stand => "You have chosen to stand. Your turn is over.\n",
      errorMessage = "Sorry, your input is not valid."
    )

  /** Renders a message received from the controller and displays it to the user.
   *
   * This method handles different types of [[Command]] and converts them into
   * appropriate console output. Each command represents a specific event that
   * occurred during the game, such as removing a player, showing a card, or
   * announcing a Blackjack.
   *
   * @param message The [[Command]] containing the information to be rendered.
   * @param console The contextual [[cats.effect.std.Console]] capability required
   *                to perform I/O operations.
   * @return An [[cats.effect.IO]] representing the console output operation.
   */
  def renderMessage(message: Command)(using console: Console[IO]): IO[Unit] = message match
    case CardsDistribution     => console.println("The current hand is going to start! Here comes the distribution of the first two cards per player.\n")
    case ShowCard(card)        => console.println(card)
    case ShowBlackJack(winner) => console.println(s"${winner.name}, you have done Black Jack!\n$winner")
    case PlayerTurn(name)      => console.println(s"Turn of $name:\n")
    case DealerTurn()          => console.println("Turn of the Dealer.\nThe dealer reveals the hidden card.")
    case DealerBusted          => console.println("DEALER BUSTED - EVERY PLAYER WINS!\n")
    case ShowBusted(player)    => console.println(s"${player.name} is busted!\n")
    case ShowCutCard           => console.println("CUT CARD HAS BEEN EXTRACTED!\n")
    case RemovePlayer(name)    => console.println(s"Player $name has been removed from the game.")


  /** Helper method to handle reading from the console, parsing, validation with a
   * custom predicate, and recursive retry.
   *
   * This method abstracts the generic loop of displaying a prompt, reading raw text from
   * the console, attempting to parse it into a specific type, validating the result,
   * and handling invalid attempts via recursion.
   *
   * @tparam T The target type of the validated input (e.g., Int, String, Boolean).
   * @param prompt   The text instructions explaining to the user what to enter.
   * @param parser           A function to transform the raw console String into an Option of type T.
   * @param predicate The validation predicate function that the parsed value must satisfy.
   * @param successMessage   A function that generates the text to display when the input is valid.
   * @param errorMessage     The text to display if the input fails verification or parsing.
   * @param console          The contextual console capability required to perform I/O operations.
   * @return An [[cats.effect.IO]] wrapping the successfully parsed and validated value of type T.
   */
  private def promptUntilValid[T](
                                   prompt: String,
                                   parser: String => Option[T],
                                   predicate: T => Boolean,
                                   successMessage: T => String,
                                   errorMessage: String
                                  )(using console: Console[IO]): IO[T] =
    for
      _     <- console.println(prompt)
      input <- console.readLine
      value <- parser(input).filter(predicate) match
        case Some(v) =>
          console.println(successMessage(v)).as(v) //as ritorna v senza dover fare un altro for yield interno
        case _ =>
          console.println(errorMessage) >> promptUntilValid(prompt, parser, predicate, successMessage, errorMessage)
    yield value
