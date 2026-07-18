---
title: Sprint 4
parent: Process
nav_order: 5
---

# Sprint 4

## Assicurazione

| Task | Volontari | Stima |
|:-----|:----------|:------|
| Metodo per chiedere la lista dei giocatori che vogliono l'assicurazione, quando la prima carta del banco è un Asso | Elena | 4 |
| Metodo nel Game per prelevare dal balance dei giocatori assicurati una quota pari a 0,5 volte la loro puntata e impostare il flag di assicurazione a `true` | Anna | 3 |
| Metodo nel Game che, per ogni giocatore assicurato, toglie la quota dalla puntata corrente e la restituisce raddoppiata se il banco ha fatto Blackjack, restituendo una lista di coppie nome/importo vinto (vuota se il banco non ha Blackjack o nessuno si è assicurato) | Nicholas | 5 |
| Richiamare in `handleDealerTurn` il metodo del Game per il calcolo delle assicurazioni, e chiamare il metodo della view per mostrare le relative vincite | Elena | 5 |

## Bot

| Task | Volontari | Stima |
|:-----|:----------|:------|
| Nuovo trait `Bot` che estende `Participant` con `Wallet`, istanziato con un balance casuale accettabile; `BotPlayer` che estende `Player`, istanziato automaticamente con balance casuale (100–500) e puntata casuale (10–50); metodo nel Game per istanziare i bot fino a completare il tavolo (`7 - numGiocatori`) | Nicholas | 6 |
| Aggiungere la puntata alla rappresentazione testuale (`toString`) di un giocatore, con override specifico per il bot | Nicholas | 1 |
| Metodo nel Game che, dato un bot, esegue le mosse automatiche e verifica la validità della puntata, scommettendo l'intero balance rimasto se non è valida (refactoring delle parti comuni con il Dealer) | Elena | 6 |
| Aggiungere una stampa a video, dopo l'istanziazione dei giocatori, che indichi quanti bot sono stati creati | Elena | 0 |
| Realizzare l'object `MyImports` | Elena | 5 |
