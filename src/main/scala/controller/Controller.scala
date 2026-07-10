package controller

import cats.effect.std.Console
import cats.effect.{IO, IOApp}
import cats.implicits.*
import model.PlayerModule.Player
import view.View.*
import model.GameModule.*
import model.PlayerModule.PlayerState.Blackjack
import model.ScoreModule.WinningScore
import view.View.Command.*

object Controller extends IOApp.Simple:

  def getPlayers(numPlayers: Int)(using console: Console[IO]): IO[List[Player]] =
    for
      playersNames <- getPlayersNames(numPlayers)
      players      <- playersNames.traverse(name =>
          getInitialDeposit(name, Game.isInitialDepositValid).map(balance => Player(name, balance))
      )
    yield players

  def getBets(game: Game)(using console: Console[IO]): IO[Unit] =
    for
      bets <- game.players.traverse(player => getBet(player, game.isBetValid(player)).map(bet => Bet(player, bet)))
      _    <- IO(game.currentBets = bets)
    yield ()

  def handleBlackjacksWinners(game: Game)(using console: Console[IO]): IO[Unit] =
    val winners: List[Player] = game.playersWithBlackjack()
    for
      _ <- IO(game.handleBlackjacks(winners))
      _ <- winners.traverse_(winner => renderMessage(ShowBlackJack(winner)))
    yield ()

  def initializeGame(using console: Console[IO]): IO[Game] =
    for
      numPlayers <- getNumPlayers
      players    <- getPlayers(numPlayers)
      game = Game(players)
    yield game

  def initializeHand(game: Game)(using console: Console[IO]): IO[Unit] =
    for
      _ <- getBets(game)
      _ <- renderMessage(CardsDistribution)
      _ <- game.distributeCards().traverse_(card => processCardDrawing(game, card))
      _ <- handleBlackjacksWinners(game)
    yield ()

  def handleHands(game: Game)(using console: Console[IO]): IO[Unit] =
    handleHand(game).flatMap(_ => if game.isCutCardInDeck && game.players.nonEmpty then handleHands(game) else IO.unit)

  def handleHand(game: Game)(using console: Console[IO]): IO[Unit] =
    for
      _ <- initializeHand(game)
      _ <- handlePlayersTurn(game)
      _ <- handleDealerTurn(game)
      // TODO _ <- handleHandWinners(game)
      _ <- endHand(game)
    yield ()

  def handlePlayerAction(game: Game, player: Player, action: PlayerAction)(using console: Console[IO]): IO[Boolean] =
    def handleDraw(player: Player): IO[Boolean] =
      game.drawCard(player) match
        case Some(card) =>
          processCardDrawing(game, s"$card\n$player") >>
            IO(game.evaluatePlayerBust(player)).flatMap:
              case true =>
                renderMessage(ShowBusted(player)) >> IO(false)
              case _    => player.score.playableValue match
                case score if score == WinningScore =>
                  IO(player.stand()) >> IO(false)
                case _                              => IO(true)
        case _          => IO(false)
    action match
      case PlayerAction.DrawCard =>
        handleDraw(player)
      case PlayerAction.Split    => game.splitPlayer(player) match
        case Some(cardPlayer, cardSplittedPlayer) =>
          renderMessage(ShowCard(s"$cardPlayer\n$player")) >> IO(true) //renderMessage(ShowCard(s"$cardSplittedPlayer\n$player"))
        //TODO modo per passare direttamente alla gestione del prossimo giocatore (lo SplitPlayer added)
        case _                                    => IO(false) //TODO fine partita
      case PlayerAction.Stand    => IO(player.stand()) >> IO(false)

  def handlePlayersTurn(game: Game)(using console: Console[IO]): IO[Unit] =
    def _handleSinglePlayerTurn(player: Player)(using console: Console[IO]): IO[Unit] =
      getPlayerAction(player, game.canSplit).flatMap: action =>
        handlePlayerAction(game, player, action).flatMap:
          case true  => _handleSinglePlayerTurn(player)
          case _     => IO.unit
    game.players
      .filterNot(_.state == Blackjack)
      .traverse_(player =>
      renderMessage(PlayerTurn(player.name)) >>
        renderMessage(ShowCard(player.toString)) >>
          _handleSinglePlayerTurn(player)
      )

  def handleDealerTurn(game: Game)(using console: Console[IO]): IO[Unit] =
    for
      _ <- renderMessage(DealerTurn())
      _ <- renderMessage(ShowCard(game.dealer.toString))
      _ <- game.computeDealerTurn().traverse_(card => processCardDrawing(game, card))
    yield()

  def handleHandWinners(game: Game)(using console: Console[IO]): IO[Unit] = ???
//    IO(game.evaluateDealerBusted()).flatMap:
//      case true =>
//        renderMessage(DealerBusted) >>
//        game.payAllPlayers()
//      case _    => ???
  //TODO andare a controllare game.evaluateDealerBusted, in caso true pagare tutti i giocatori, in caso false controllare le singole vincite

  def endHand(game: Game)(using console: Console[IO]): IO[Unit] =
    def ejectPlayer(player: Player): IO[Unit] =
      renderMessage(RemovePlayer(player.name)) >>
        IO(game.removePlayer(player)) //use of >> to concatenate the two effects without using a nested for-yield
    for
      _       <- game.players
        .filter(_.balance.totalValue <= 0)
        .traverse_(ejectPlayer)
      choices <- game.players.traverse(player => getLeaveChoice(player).map(choice => (player, choice)))
      _       <- choices
        .filter((_, choice) => choice == Choices.Yes)
        .traverse_((player, _) => ejectPlayer(player))
      _       <- IO(game.startNewHand())
    yield ()

  def run: IO[Unit] =
    //TODO creare un object che contiene tutti gli oggetti da esportare e farne l'import
    for
      game <- initializeGame
      _    <- handleHands(game)
      //TODO chiamare metodo endGame alla fine della partita
    yield ()

  private def processCardDrawing(game: Game, cardMessage: String)(using console: Console[IO]): IO[Unit] =
    val cutCardCheckEffect = if !game.isCutCardInDeck then renderMessage(ShowCutCard) else IO.unit
    cutCardCheckEffect >> renderMessage(ShowCard(cardMessage))
