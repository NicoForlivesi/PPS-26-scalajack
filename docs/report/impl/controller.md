---

title: Controller
nav_order: 4
parent: Implementazione
grand_parent: Report

---

# Implementazione del Controller

Il *controller* è realizzato in stile **funzionale** sulla monade `IO` di **Cats Effect**: ogni fase del gioco è un
valore `IO[...]` e il flusso complessivo si ottiene **componendo** questi effetti. L'applicazione estende
`IOApp.Simple`, che ne fa il punto d'ingresso eseguibile.

## Composizione delle fasi

Il ciclo di vita di una mano è descritto sequenziando le fasi con l'operatore `>>` (esegui in sequenza scartando il
risultato), mentre l'intero programma è espresso con una *for-comprehension*:

```scala
def run: IO[Unit] =
  for
    game <- initializeGame
    _    <- handleHands(game)
    _    <- endGame(game)
  yield ()

private def handleHand(game: Game)(using Console[IO]): IO[Unit] =
  initializeHand(game) >>
    handlePlayersTurn(game) >>
    handleDealerTurn(game) >>
    handleHandWinners(game) >>
    finalizeHand(game)
```

La ripetizione delle mani fino alla fine della partita usa il combinatore `iterateUntil`, che esprime dichiarativamente
il ciclo senza mutare variabili di controllo esterne:

```scala
def handleHands(game: Game)(using Console[IO]): IO[Unit] =
  handleHand(game).iterateUntil(_ => game.isOver)
```

## Iterazione, effetti condizionali e collezioni

Il controller fa uso dei combinatori idiomatici di Cats Effect:

- `traverse_` per eseguire un effetto su ogni elemento di una lista (per esempio mostrare ogni carta distribuita o ogni
  saldo aggiornato);
- `IO.whenA` / `IO.unlessA` per gli effetti condizionali (mostrare il messaggio della *cut card*, gestire l'assicurazione
  solo se il banco ha un Asso, ecc.);
- una funzione ricorsiva locale per scorrere i giocatori nel loro ordine di turno, ottenendo il successivo con
  `getNextPlayer`.

```scala
def handlePlayersTurn(game: Game)(using Console[IO]): IO[Unit] =
  def loop(playerOpt: Option[Player]): IO[Unit] = playerOpt match
    case Some(player) =>
      for
        _          <- IO.unlessA(player.state == Blackjack)(handleSinglePlayerTurn(game, player))
        nextPlayer <- IO(game.getNextPlayer(player))
        _          <- loop(nextPlayer)
      yield ()
    case _ => IO.unit
  IO(game.players.headOption).flatMap(loop)
```

## Turno del giocatore e `TurnOutcome`

Il turno interattivo di un giocatore è un ciclo governato dall'enum `TurnOutcome`: il controller chiede l'azione
alla *view*, la instrada al *model* e, in base all'esito (`Continue`/`Stop`), decide se proseguire. L'instradamento delle
azioni è un *pattern matching* su `PlayerAction`, in cui pesca e raddoppio condividono lo stesso meccanismo di
estrazione ed elaborazione della carta, differenziandosi solo per il flag di arresto automatico:

```scala
def handlePlayerAction(game: Game, player: Player, action: PlayerAction)(using Console[IO]): IO[TurnOutcome] = action match
  case PlayerAction.DrawCard   => drawAndProcess(game, player, game.drawCard, autoStop = false)
  case PlayerAction.DoubleDown => drawAndProcess(game, player, game.doubleDown, autoStop = true)
  case PlayerAction.Stand      => finalizePlayerTurn(game, player)
  case PlayerAction.Split      => processSplit(game, player)
```

Le mutazioni dello stato del *model* (per esempio `game.addBots()` o l'aggiornamento delle puntate) sono racchiuse
esplicitamente in `IO(...)`, coerentemente con la scelta di rendere osservabili tutti gli effetti all'interno della
monade, senza eseguirli in modo implicito.

*Contributi principali: Elena; pagamenti e blackjack - Nicholas; gestione dinamica del turno dei giocatori - Anna*
