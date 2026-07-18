---
title: Testing
parent: Report
nav_order: 7
---

# Testing

## Strumenti

- **ScalaTest** (stile `AnyFunSuite`) come framework di test principale.
- **Mockito** per il mocking delle dipendenze nei test del controller e del livello di orchestrazione.
- **junit-interface** come test runner integrato in sbt.
- Un modulo di supporto `utils.TestExports` centralizza gli import comuni ai test (analogo, per la parte
  di test, alle facciate `ModelExports`/`GameUIExports` usate nel codice di produzione).

## Organizzazione dei test

I test sono organizzati specularmente ai package di produzione, uno per ciascun modulo principale:

| Test | Oggetto verificato |
|:-----|:--------------------|
| `ScoreTest` | Calcolo del punteggio, gestione dell'Asso, riconoscimento del Blackjack |
| `DeckTest` | Generazione, pesca, mescolamento del mazzo e posizionamento della cut card |
| `FicheTest` | Conversione di un importo in fiches (algoritmo greedy) |
| `PlayerTest` | Stati del giocatore, deposito/prelievo dal wallet, split, bot |
| `DealerTest` | Rivelazione delle carte, soglia di stop, profitto del banco |
| `GameTest` | Distribuzione carte, bust, blackjack, assicurazione, split, raddoppio, payout, fine mano |
| `CLIViewTest` | Parsing e validazione degli input utente, formattazione dei messaggi |
| `ControllerTest` | Orchestrazione dell'intero ciclo di gioco tramite `IO` e mock del `Console` |

`GameTest` e `ControllerTest` sono le suite più estese, poiché coprono rispettivamente il cuore della
logica di dominio condivisa (`Game`) e l'intero ciclo applicativo end-to-end simulato in memoria.

## Approccio

- Ogni requisito funzionale e di sistema descritto nella [Requirement specification](requirement-specification.md)
  è coperto da almeno un test automatico, eseguito ad ogni modifica del codice.
- La logica di dominio (`model`) è testata in isolamento, costruendo direttamente le entità
  (`NormalPlayer`, `SplitPlayer`, `BotPlayer`, mazzi di test tramite `Deck.testDeck`) senza passare
  dall'interfaccia utente.
- Il `Controller`, essendo basato su effetti `IO`, è testato fornendo un'implementazione di test di
  `Console[IO]` che simula sequenze di input predefinite e cattura l'output prodotto, permettendo di
  verificare l'intero flusso applicativo senza interazione reale da tastiera.
