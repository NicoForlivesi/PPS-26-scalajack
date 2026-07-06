package controller

import cats.effect.std.Console
import cats.effect.{IO, IOApp}
import cats.implicits.*
import model.PlayerModule.Player
import view.View.*
import model.GameModule.*

object Controller extends IOApp.Simple:

  def getPlayer(using console: Console[IO]): IO[Player] =
    for
      playerID <- getPlayerID
      playerInitialBalance <- getInitialBalance
    yield Player(playerID, playerInitialBalance)

  def initializeGame(using console: Console[IO]): IO[Game] =
    for
      numPlayers <- getNumPlayers
      players    <- (1 to numPlayers).toList.traverse(_ => getPlayer)
      game        = Game(players)
    yield game

  def initializeHand(game: Game)(using console: Console[IO]): IO[Unit] =
    for
      bets <- game.players.traverse(player => getBet(player).map(bet => Bet(player, bet)))
      _    <- IO(game.currentBets = bets)
    yield ()

  def endHand(game: Game)(using console: Console[IO]): IO[Unit] =
    for
      choices <- game.players.traverse(player => getLeaveChoice(player).map(choice => (player, choice)))
      _       <- IO:
        choices
          .filter((_, choice) => choice == "Y")
          .foreach((player, _) => game.removePlayer(player))
    yield ()

  def run: IO[Unit] =
    //TODO creare un object che contiene tutti gli oggetti da esportare e farne l'import
    for
      game <- initializeGame
      _    <- initializeHand(game)
      //TODO mano
      _    <- endHand(game)
    yield ()

