---
title: Sprint 3
parent: Process
nav_order: 4
---

# Sprint 3

## Gestione fine mano e fine partita

| Task | Volontari | Stima | Note |
|:-----|:----------|:------|:-----|
| Metodo nel Game per gestire il pagamento dei giocatori a fine mano e l'aggiornamento del balance del banco, inclusa la gestione degli `SplittedPlayer` (rimozione, redistribuzione del balance) | Nicholas, Elena | 10 | Restava da rifinire la gestione degli `SplittedPlayer` e sistemare/decommentare `handleHandWinners` nel Controller |
| Rivedere il meccanismo per chiedere ai giocatori se vogliono uscire (gestione a lista) | Elena | 4 | |
| Metodo `startNewHand` nel Game, richiamato dal Controller in `endHand` | Nicholas | 3 | |
| Controllo che non esista già un altro giocatore con lo stesso nome | Elena | 3 | |
| Introduzione di `CutCard` come case di `Card`, inserita in `generateDeck`; modifica di `drawCard` per ripescare se esce la cut card; nuovo tipo di ritorno con booleano (`isCutCardInDeck`) | Elena | 7 | |
| Verifica se il mazzo contiene ancora il segnalibro, per capire se la partita deve terminare | Elena | 2 | Da testare |
| A fine partita, pagare i giocatori con il balance rimasto convertito in valuta e stamparlo a video | Elena | 3 | |

## Gestione split e raddoppio

| Task | Volontari | Stima |
|:-----|:----------|:------|
| Raddoppio: aggiungere la scelta "Double Down" in view (solo con due carte in mano, con verifica nel Game); raddoppiare la puntata, pescare una carta e fermarsi | Nicholas | 7 |
| Aggiungere la scelta di split in view (solo con due carte uguali, verifica nel Game sul Player); crea uno `SplittedPlayer` aggiunto alla lista dei giocatori, sostituendo la seconda carta con una nuova pescata | Anna | 10 |
| Nuova classe `SplittedPlayer` che estende `Player`: la lista carte è inizializzata con quella del giocatore originale, con la seconda carta assegnata subito alla creazione | Anna | 8 |
| `SplittedPlayer` sovrascrive il controllo di split-abilità verificando la presenza di un Asso | Anna | 2 |
| La creazione di uno `SplittedPlayer` aggiunge una nuova voce identica in `currentBets` e sottrae l'importo puntato dal balance del giocatore originale; gestione dell'esecuzione del turno del nuovo giocatore da parte del Controller | Anna | 5 |
| Quando un giocatore che ha splittato finisce il proprio turno, il balance rimanente passa allo `SplittedPlayer` (nome derivato con suffisso `_split_n`) | Anna | 4 |
