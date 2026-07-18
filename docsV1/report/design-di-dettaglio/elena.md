---
title: Elena Sarti
parent: Design di dettaglio
grand_parent: Report
nav_order: 1
---

# Design di dettaglio — Elena Sarti

Moduli di competenza: **View**, **Controller**, `ModelExports`, `GameUIExports`, `TestExports`,
`StandardCard` e `CutCard` (e i metodi che le utilizzano).

## CLIView

`CLIView` è un `object` puramente funzionale rispetto agli effetti: ogni operazione di interazione con
l'utente è espressa come `IO[A]` di Cats Effect, parametrica rispetto a un `Console[IO]` (passato come
parametro `using`), il che consente di sostituirlo con un'implementazione di test.

Sono definiti due enum di supporto:

- **`PlayerAction`** — le azioni disponibili a un giocatore nel proprio turno (pescare, fermarsi, raddoppiare, splittare).
- **`Command`** — i messaggi testuali che la view può mostrare in output, centralizzando la formattazione.

Ogni funzione di input (`getNumPlayers`, `getPlayersNames`, `getInitialDeposit`, `getBet`,
`getLeavingPlayers`, `getInsurancePlayers`, `getPlayerAction`) segue lo stesso design: richiede in
ingresso un predicato di validazione fornito dal model (es. `isPlayerNumValid: Int => Boolean`), stampa
un messaggio, legge l'input e **ripete la richiesta in caso di input non valido**, tramite ricorsione
sull'`IO`. Questo mantiene la view disaccoppiata dalle regole di validazione, che restano nel model.

## Controller

`Controller` è un `IOApp.Simple` che definisce il ciclo di vita dell'applicazione tramite tre fasi
componibili in una for-comprehension:

```scala
def run: IO[Unit] =
  for
    game <- initializeGame
    _    <- handleHands(game)
    _    <- endGame(game)
  yield ()
```

- **`initializeGame`** raccoglie numero di giocatori, nomi e depositi iniziali, crea il `Game` e vi
  aggiunge i bot richiesti.
- **`handleHands`** esegue il ciclo ricorsivo delle mani, usando l'enum `TurnOutcome` (`Continue`/`Stop`)
  per decidere se proseguire con una nuova mano o terminare, in base allo stato del gioco
  (`isOver`, `areAllHumanPlayersOut`).
- **`endGame`** mostra il messaggio di chiusura e il riepilogo finale dei saldi.

## StandardCard e CutCard

Il tipo `Card` è modellato come `enum` con due casi: `StandardCard(suit, value, isFaceUp)` per le carte
di gioco vere e proprie, e `CutCard` come marcatore speciale privo di valore di gioco. Questa scelta
(anziché un flag booleano su `StandardCard`) rende impossibile per costruzione assegnare un punteggio a
una cut card, ed è alla base di:

- **`Deck.addCutCardToDeck`** — inserisce la cut card a una distanza dalla fine del mazzo pari a
  `numParticipanti × k` (con `k = 5` di default), per garantire carte sufficienti a concludere l'ultima mano.
- **`Game.isCutCardInDeck`** / **`Game.shouldShowCutCardMessage`** — verificano se la cut card è ancora
  nel mazzo e se il messaggio di avviso è già stato mostrato all'utente, per notificare una sola volta
  l'imminente fine della partita.
