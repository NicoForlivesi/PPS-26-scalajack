---

title: View
nav_order: 3
parent: Implementazione
grand_parent: Report

---

# Implementazione della View

## Un'unica astrazione per l'input validato

Tutte le richieste di input all'utente seguono lo stesso schema: stampa del *prompt*, lettura, *parsing*, validazione e
ripetizione in caso di errore. Questo schema è fattorizzato in un'unica funzione generica di ordine superiore,
`promptUntilValid[T]`, parametrizzata sul tipo `T` del valore atteso e su un *parser*, un *predicato* di validazione e i
messaggi di successo/errore. La ripetizione in caso di input non valido è realizzata tramite **ricorsione**:

```scala
private def promptUntilValid[T](
    prompt: String,
    parser: String => Option[T],
    predicate: T => Boolean,
    successMessage: T => String,
    errorMessage: String
)(using console: Console[IO]): IO[T] =
  for
    _     <- console.println(prompt)
    input <- console.readLine.map(_.trim)
    value <- parser(input).filter(predicate) match
      case Some(v) => console.println(successMessage(v)).as(v)
      case _       => console.println(errorMessage) >> promptUntilValid(prompt, parser, predicate, successMessage, errorMessage)
  yield value
```

Su questa base sono costruite tutte le funzioni concrete (`getNumPlayers`, `getPlayersNames`, `getInitialDeposit`,
`getBet`, `getPlayerAction`, ...), che si limitano a fornire *parser* e predicati specifici. Ne risulta un forte rispetto
del principio **DRY**: la logica del ciclo di input esiste in un solo punto.

## Effetti e context parameter

La console è astratta dalla capacità `Console[IO]` di Cats Effect, passata come **context parameter** (`using`). Questo
rende le funzioni della *view* **pure** (non eseguono I/O al momento della definizione, ma descrivono un effetto `IO`) e
soprattutto **testabili**: nei test è sufficiente fornire una diversa `Console[IO]` per simulare l'utente, come descritto
nella pagina di [Testing](../testing.md).

## Rendering guidato dai tipi

L'output è centralizzato nella funzione `renderMessage`, che effettua *pattern matching* esaustivo sull'enumerazione
`Command`, traducendo ogni evento di gioco nel corrispondente messaggio testuale:

```scala
def renderMessage(message: Command)(using console: Console[IO]): IO[Unit] = message match
  case CardsDistribution              => console.println("...")
  case ShowBlackJack(winner)          => console.println(s"${winner.name}, you have done Black Jack!\n$winner")
  case ShowInsuranceWin(name, win)    => console.println(s"INSURANCE DEAL: $name has won $win fiches!\n")
  case GameOver                       => console.println("The game is over!\n")
  // ... un caso per ogni Command
```

L'esaustività del *match* garantisce, a livello di compilazione, che ogni nuovo `Command` aggiunto all'enumerazione
venga gestito anche in fase di rendering.

*Contributi principali: funzioni di input (deposito, puntata, uscite) e rendering — Elena; interazione durante il turno —
Anna.*
