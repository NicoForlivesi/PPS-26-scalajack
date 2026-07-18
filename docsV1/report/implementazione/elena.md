---
title: Elena Sarti
parent: Implementazione
grand_parent: Report
nav_order: 1
---

# Implementazione — Elena Sarti

## Input validato con retry ricorsivo (CLIView)

Le funzioni di input della view non delegano la validazione alla view stessa, ma ricevono un predicato
dal model e ripetono la richiesta ricorsivamente in caso di input non valido, mantenendo l'operazione
puramente descritta come valore `IO`:

```scala
def getNumPlayers(isPlayerNumValid: Int => Boolean)(using console: Console[IO]): IO[Int] =
  for
    _        <- console.println("Inserisci il numero di giocatori:")
    input    <- console.readLine
    numOpt   =  input.toIntOption.filter(isPlayerNumValid)
    result   <- numOpt match
      case Some(n) => IO.pure(n)
      case None    => console.println("Numero non valido, riprova.") *> getNumPlayers(isPlayerNumValid)
  yield result
```

*(estratto semplificato a scopo illustrativo; la firma e la logica di validazione rispecchiano
l'implementazione reale in `CLIView.scala`)*

## Ciclo di gioco nel Controller

Il controller compone le fasi della partita come una singola for-comprehension su `IO`, delegando ogni
decisione di dominio (se una mano deve iniziare, se la partita è finita) al model tramite `Game`:

```scala
def run: IO[Unit] =
  for
    game <- initializeGame
    _    <- handleHands(game)
    _    <- endGame(game)
  yield ()
```

`handleHands` richiama se stesso finché `game.isOver` non è vero, alternando le fasi di puntata,
distribuzione carte, turni dei giocatori, turno del banco e payout.

## Cut card

La cut card è inserita nel mazzo come un caso distinto dell'enum `Card`, così da non poter mai essere
confusa con una carta valida ai fini del punteggio:

```scala
enum Card:
  case StandardCard(suit: Suit, value: Value, isFaceUp: Boolean = true)
  case CutCard
```

`Deck.addCutCardToDeck` la posiziona a `numParticipanti × k` posizioni dalla fine, garantendo carte
sufficienti per concludere l'ultima mano prima che il mazzo si esaurisca.
