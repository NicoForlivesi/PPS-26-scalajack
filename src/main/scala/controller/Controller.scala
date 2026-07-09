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

  def getPlayer(using console: Console[IO]): IO[Player] =
    for
      playerID <- getPlayerID
      playerInitialBalance <- getInitialDeposit(playerID, Game.isInitialDepositValid)
    yield Player(playerID, playerInitialBalance)

  def getBets(game: Game)(using console: Console[IO]): IO[Unit] =
    for
      bets <- game.players.traverse(player => getBet(player, game.isBetValid(player)).map(bet => Bet(player, bet)))
      _    <- IO(game.currentBets = bets)
    yield ()

  def handleBlackJacks(game: Game)(using console: Console[IO]): IO[Unit] =
    val winners: List[Player] = game.playersWithBlackjack()
    for
      _ <- IO(game.handleBlackjacks(winners))
      _ <- winners.traverse_(winner => renderMessage(ShowBlackJack(winner)))
    yield ()

  def initializeGame(using console: Console[IO]): IO[Game] =
    for
      numPlayers <- getNumPlayers
      players    <- (1 to numPlayers).toList.traverse(_ => getPlayer)
      game        = Game(players)
    yield game

  def initializeHand(game: Game)(using console: Console[IO]): IO[Unit] =
    for
      _ <- getBets(game)
      _ <- renderMessage(CardsDistribution)
      _ <- game.distributeCards().traverse_(card => renderMessage(ShowCard(card)))
      _ <- handleBlackJacks(game)
    yield ()

  def handleHands(game: Game): IO[Unit] =
    handleHand(game).flatMap(_ => if game.deck.size() > 0 && game.players.nonEmpty then handleHands(game) else IO.unit)
    //TODO 'game.hasEnoughCardsForHand' invece che deck.size() > 0, oppure uso del segnalibro '!game.isCutCardReached' ??

  def handleHand(game: Game)(using console: Console[IO]): IO[Unit] =
    for
      _ <- initializeHand(game)
      _ <- handlePlayersTurn(game)
      _ <- handleDealerTurn(game)
      _ <- handleHandWinners(game)
      _ <- endHand(game)
    yield ()

  def handlePlayersTurn(game: Game)(using console: Console[IO]): IO[Unit] =
    def _handleSinglePlayerTurn(player: Player)(using console: Console[IO]): IO[Unit] =
      for
        action <- getPlayerAction(player)
        _      <- action match
          case PlayerAction.DrawCard =>
            game.drawCard(player) match
              case Some(card) =>
                  renderMessage(ShowCard(s"$card\n$player")) >>
                  IO(game.evaluateBust(player)).flatMap(busted =>
                    if busted then renderMessage(ShowBusted(player))
                    else if player.score.playableValue == WinningScore then IO(player.stand())
                    else _handleSinglePlayerTurn(player)
                  )
              case None       => IO.unit
                //TODO gestione fine partita
          case PlayerAction.Stand =>
            IO(player.stand())
      yield()
    game.players
      .filterNot(_.state == Blackjack)
      .traverse_(player =>
      renderMessage(PlayerTurn(player.name)) >>
      renderMessage(ShowCard(player.toString)) >>
      _handleSinglePlayerTurn(player))

  def handleDealerTurn(game: Game)(using console: Console[IO]): IO[Unit] =
    for
      _ <- renderMessage(DealerTurn())
      _ <- renderMessage(ShowCard(game.dealer.toString))
      _ <- game.computeDealerTurn().traverse_(card => renderMessage(ShowCard(card)))
    yield()

  def handleHandWinners(game: Game)(using console: Console[IO]): IO[Unit] = ???
//    IO(game.dealer.isBusted()).flatMap:
//      case true =>
//        renderMessage(DealerBusted) >>
//        game.payAllPlayers()
//      case _    => ???
  //TODO andare a controllare game.dealer.isBusted, in caso true pagare tutti i giocatori, in caso false controllare le singole vincite

  def endHand(game: Game)(using console: Console[IO]): IO[Unit] =
    def ejectPlayer(player: Player): IO[Unit] =
      renderMessage(RemovePlayer(player.name)) >> IO(game.removePlayer(player)) //use of >> to concatenate the two effects without using a nested for-yield
    for
      _       <- game.players
        .filter(_.balance.totalValue <= 0)
        .traverse_(ejectPlayer)
      choices <- game.players.traverse(player => getLeaveChoice(player).map(choice => (player, choice)))
      _       <- choices
        .filter((_, choice) => choice == Choices.Yes)
        .traverse_((player, _) => ejectPlayer(player))
      _       <- game.players.traverse_(player => IO(player.startNewRound()))
    yield ()

  def run: IO[Unit] =
    //TODO creare un object che contiene tutti gli oggetti da esportare e farne l'import
    for
      game <- initializeGame
      _    <- handleHands(game)
      //TODO chiamare metodo endGame alla fine della partita
    yield ()


