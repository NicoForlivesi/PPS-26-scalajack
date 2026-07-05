package controller

import cats.effect.{IO, IOApp}
import model.PlayerModule.Player
import view.View.*

object Controller extends IOApp.Simple:

  def getPlayer: IO[Player] =
    for
      playerID             <- getPlayerID
      playerInitialBalance <- getInitialBalance
      //TODO convertire da soldi a fiches
    yield Player(playerID, playerInitialBalance)

  def startGame: IO[Unit] =
    for
      player <- getPlayer
      //TODO creare istanza della partita passando la lista dei giocatori definiti dall'utente che avvia la partita
    yield ()

  def run: IO[Unit] =
    //TODO creare un object che contiene tutti gli oggetti da esportare e farne l'import
    startGame

