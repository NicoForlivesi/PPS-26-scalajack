package view

import cats.effect.IO
import cats.effect.std.Console
import model.FicheModule.Fiche
import model.PlayerModule.Player
import view.View.Command.{CardsDistribution, DealerBusted, DealerTurn, GameOver, HandOver, PlayerTurn, RemovePlayer, ShowBalance, ShowBlackJack, ShowBusted, ShowCard, ShowCutCard, ShowFinalBalance, ShowInsuranceWin}

object View:

  enum PlayerAction:
    case DrawCard
    case Stand
    case DoubleDown
    case Split

  enum Command:
    case CardsDistribution
    case ShowCard(card: String)
    case ShowBlackJack(player: Player)
    case PlayerTurn(name: String)
    case DealerTurn()
    case DealerBusted
    case ShowBusted(player: Player)
    case ShowInsuranceWin(player: Player, win: Double)
    case ShowCutCard
    case RemovePlayer(name: String)
    case HandOver
    case ShowBalance(name: String, balance: Double)
    case GameOver
    case ShowFinalBalance(name: String, finalBalance: Double)

  /** Interactively prompts the user to enter the number of players in the match.
   *
   * @param console the contextual [[cats.effect.std.Console]] instance used to perform
   *                pure and testable I/O operations.
   * @return        an [[cats.effect.IO]] encapsulating the computation that yields the
   *                initial number of players in the game as a [[Int]].
   */
  def getNumPlayers(using console: Console[IO]): IO[Int] =
    promptUntilValid(
      prompt = "Welcome to the game! \nPlease enter the desired number of players:",
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
   * @return           An [[cats.effect.IO]] wrapping a [[List]] of successfully validated,
   *                   trimmed, and unique player names.
   */
  def getPlayersNames(numPlayers: Int)(using console: Console[IO]): IO[List[String]] =
    promptUntilValid(
      prompt = "Please, enter all the players' names below, separated by \", \". Note that it is not possible to define a name containing the character \"_\".",
      parser = rawInput =>
        val names = rawInput.split(",").map(_.trim).filter(_.nonEmpty).toList
        Some(names),
      predicate = playersNames => playersNames.length == numPlayers &&
        playersNames.distinct.length == playersNames.length &&
        !playersNames.exists(_.contains("_")),
      successMessage = _ => "All names have been correctly added!\n",
      errorMessage = s"Sorry, your input is not valid. Either the number of players does not $numPlayers, there are duplicate/empty names, or some names contain \"_\"."
    )

  /** Interactively prompts the user to enter their initial playing balance.
   *
   * This method prints a prompt to the terminal and reads the user's input.
   * If the input is not a valid positive number, it prints an error message
   * and recursively prompts the user again until a valid balance is provided.
   *
   * @param console the contextual [[cats.effect.std.Console]] instance used to perform
   *                pure and testable I/O operations.
   * @return        an [[cats.effect.IO]] encapsulating the computation that yields the
   *                initial balance as a [[Double]].
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
   * @param player     The [[Player]] who is placing the bet.
   * @param isBetValid The method used to validate the input.
   * @param console    The implicit [[Console]] instance used to handle terminal I/O.
   * @return           A [[cats.effect.IO]] that, when evaluated, contains the valid
   *                   bet amount.
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

  /** Helper to read, parse, and validate a comma-separated list of player names.
   *
   * @param promptMessage The instruction string printed to the user.
   * @param isNameValid   Predicate to verify if each name belongs to an eligible player.
   * @param console       The contextual [[cats.effect.std.Console]] instance used to perform
   *                      pure and testable I/O operations.
   * @return              A [[List]] of validated, trimmed, and non-empty player names, or an empty list if input is blank.
   */
  private def promptForPlayerList(promptMessage: String, isNameValid: String => Boolean)
                                   (using console: Console[IO]): IO[List[String]] =
    promptUntilValid(
      prompt = promptMessage,
      parser = input =>
        val trimmed = input.trim
        if trimmed.isEmpty then Some(List.empty)
        else Some(trimmed.split(",").map(_.trim).filter(_.nonEmpty).toList),
      predicate = _.forall(isNameValid),
      successMessage = _ => "Your choices have been correctly registered!\n",
      errorMessage = "Sorry, your input is not valid."
    )

  /** Prompts for the names of all players who wish to leave the table at the end of the hand.
   *
   * @param isNameValid Predicate to verify if the names match active players.
   * @param console     The contextual [[cats.effect.std.Console]] instance used to perform
   *                    pure and testable I/O operations.
   * @return A [[List]] containing the names of the leaving players.
   */
  def getLeavingPlayers(isNameValid: String => Boolean)(using console: Console[IO]): IO[List[String]] =
    promptForPlayerList(
      promptMessage = "Please, enter the names of the players that want to leave the game now, if any, separated by \", \".",
      isNameValid = isNameValid
    )

  /** Prompts players to buy insurance when the dealer shows an Ace.
   *
   * @param isNameValid Predicate to verify if the insurance is bought by valid, active players.
   * @param console     The contextual [[cats.effect.std.Console]] instance used to perform
   *                    pure and testable I/O operations.
   * @return A [[List]] containing the names of the players who chose to buy insurance.
   */
  def getInsurancePlayers(isNameValid: String => Boolean)(using console: Console[IO]): IO[List[String]] =
    promptForPlayerList(
      promptMessage = "The Dealer shows an Ace! Please, enter the names of the players who want to buy Insurance, if any, separated by \", \".",
      isNameValid = isNameValid
    )

  /** Prompts the player to choose an action during their turn.
   *
   * This method interacts with the player through the console, displaying the available
   * actions and waiting for a valid choice. The input is case-insensitive (accepts both
   * lowercase and uppercase letters) and will recursively prompt the player until either
   * 'D' (draw a card) or 'S' (stand) is entered.
   *
   * @param player        The [[Player]] currently performing their turn.
   * @param canDoubleDown The method used to check whether doubling down is currently allowed.
   * @param canSplit      The method used to check whether splitting is currently allowed.
   * @param console       The contextual [[cats.effect.std.Console]] capability required to perform I/O operations.
   * @return              An [[cats.effect.IO]] containing the validated [[PlayerAction]] chosen by the player.
   */
  def getPlayerAction(player: Player, canDoubleDown: Player => Boolean, canSplit: Player => Boolean)
                     (using console: Console[IO]): IO[PlayerAction] =
    val doubleDownOption = if canDoubleDown(player) then ", \"O\" to double down" else ""
    val splitOption = if canSplit(player) then ", \"P\" to split" else ""
    promptUntilValid(
      prompt =  s"${player.name}, choose your action: type \"D\" to draw a card, \"S\" to stand$doubleDownOption$splitOption.",
      parser = input =>
        input.trim.toUpperCase match
          case "D"                          => Some(PlayerAction.DrawCard)
          case "S"                          => Some(PlayerAction.Stand)
          case "O" if canDoubleDown(player) => Some(PlayerAction.DoubleDown)
          case "P" if canSplit(player)      => Some(PlayerAction.Split)
          case _                            => None,
      predicate = input => Set(
        PlayerAction.DrawCard,
        PlayerAction.Stand,
        PlayerAction.DoubleDown,
        PlayerAction.Split).contains(input),
      successMessage =
        case PlayerAction.DrawCard  => "A new card will be dealt to you:"
        case PlayerAction.Stand => "You have chosen to stand. Your turn is over.\n"
        case PlayerAction.DoubleDown => "You have chosen to double down!" +
          "Your bet is doubled, one last card will be dealt."
        case PlayerAction.Split => "You have chosen to split your hand. " +
          "Your cards will be divided into two separate hands, and you will play also the following turn.",
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
   * @return        An [[cats.effect.IO]] representing the console output operation.
   */
  def renderMessage(message: Command)(using console: Console[IO]): IO[Unit] = message match
    case CardsDistribution               => console.println("The current hand is going to start! Here comes the distribution of the first two cards per player.\n")
    case ShowCard(card)                  => console.println(card)
    case ShowBlackJack(winner)           => console.println(s"${winner.name}, you have done Black Jack!\n$winner")
    case PlayerTurn(name)                => console.println(s"Turn of $name:\n")
    case DealerTurn()                    => console.println("Turn of the Dealer.\nThe dealer reveals the hidden card.")
    case DealerBusted                    => console.println("DEALER BUSTED - EVERY PLAYER WINS!\n")
    case ShowBusted(player)              => console.println(s"${player.name} is busted!\n")
    case ShowInsuranceWin(player, win)   => console.println(s"INSURANCE DEAL: ${player.name} has won $win fiches!")
    case ShowCutCard                     => console.println("CUT CARD HAS BEEN EXTRACTED!\n")
    case RemovePlayer(name)              => console.println(s"Player $name has been removed from the game.\n")
    case HandOver                        => console.println("The current hand is over! Here are the current balances:\n")
    case ShowBalance(name, balance)      => console.println(s"$name has currently a balance of $balance fiches.\n")
    case GameOver                        => console.println("The game is over!\n")
    case ShowFinalBalance(name, balance) => console.println(s"$name ends the game with $balance €.\n")

  /** Helper method to handle reading from the console, parsing, validation with a
   * custom predicate, and recursive retry.
   *
   * This method abstracts the generic loop of displaying a prompt, reading raw text from
   * the console, attempting to parse it into a specific type, validating the result,
   * and handling invalid attempts via recursion.
   *
   * @tparam T               The target type of the validated input (e.g., Int, String, Boolean).
   * @param prompt           The text instructions explaining to the user what to enter.
   * @param parser           A function to transform the raw console String into an Option of type T.
   * @param predicate        The validation predicate function that the parsed value must satisfy.
   * @param successMessage   A function that generates the text to display when the input is valid.
   * @param errorMessage     The text to display if the input fails verification or parsing.
   * @param console          The contextual console capability required to perform I/O operations.
   * @return                 An [[cats.effect.IO]] wrapping the successfully parsed and validated value of type T.
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
