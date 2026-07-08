package controller

import cats.effect.std.Console
import cats.effect.{IO, IOApp}
import cats.implicits.*
import model.PlayerModule.Player
import view.View.*
import model.GameModule.*
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

  def handleBlackJacks(game: Game)(using console: Console[IO]): IO[Unit] = ???
  //TODO controllare i blackjack dopo la distribuzione iniziale: game.playersWithBlackjack(),
  // pagarli con game.handleBlackjacks(), e mostrarli in view (serve un nuovo Command,
  // es. ShowBlackjackWinners, non ancora definito in View.Command)
  
  def initializeGame(using console: Console[IO]): IO[Game] =
    for
      numPlayers <- getNumPlayers
      players    <- (1 to numPlayers).toList.traverse(_ => getPlayer)
      game        = Game(players)
    yield game

  def initializeHand(game: Game)(using console: Console[IO]): IO[Unit] =
    for
      _ <- getBets(game)
      _ <- game.distributeCards().traverse_(card => renderMessage(ShowCard(card)))
      _ <- handleBlackJacks(game)
    yield ()

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
      _    <- initializeHand(game)
      //TODO mano (i player che hanno fatto BJ sono già esclusi qui)
      _    <- endHand(game)
    yield ()

