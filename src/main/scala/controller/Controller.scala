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
    //TODO convertire da soldi a fiches
    yield Player(playerID, playerInitialBalance)

  def initializeGame: IO[Game] =
    for
      numPlayers <- getNumPlayers
      players    <- (1 to numPlayers).toList.traverse(_ => getPlayer)
      game        = Game(players)
    yield game

  def run: IO[Unit] =
    //TODO creare un object che contiene tutti gli oggetti da esportare e farne l'import
    for
      game <- initializeGame
    yield ()

