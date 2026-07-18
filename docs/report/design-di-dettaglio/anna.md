---
title: Anna Malagoli
parent: Design di dettaglio
grand_parent: Report
nav_order: 2
---

# Design di dettaglio — Anna Malagoli

Moduli di competenza: **`DeckModule`**, **`Player`** e **`SplitPlayer`** (e i metodi che li utilizzano),
**`DealerModule`**, **`ParticipantModule`**, gestione dell'**assicurazione**.

## Participant e Dealer

`Participant` è il trait base condiviso da banco e giocatori: incapsula lo stato mutabile delle carte in
mano (`currentCards`, privato) esponendo solo operazioni sicure (`addCard`, `clearHand`, `cards`) e
delega il calcolo dello `score` alle carte correnti. `Dealer` estende `Participant` aggiungendo il
comportamento specifico del banco: rivelare le carte coperte (`revealCards`), accumulare il profitto
(`totalProfit`/`addProfit`) e determinare se ha raggiunto la soglia di stop (`hasFinishedTurn`, a 17),
riusando la logica di visualizzazione del punteggio (`displayScore`) per nascondere il valore reale
finché il banco non ha finito il turno.

## DeckModule

Il `Deck` è un **opaque type** sopra `List[Card]`: all'esterno del modulo è visibile solo come tipo
astratto, dotato delle operazioni definite tramite `extension` (`draw`, `shuffle`, `isEmpty`, `size`,
`toList`). Questo impedisce a chi usa il `Deck` di manipolarne direttamente la struttura interna.
La generazione (`generateDeck`) crea `n` mazzi standard da 52 carte e vi inserisce automaticamente la
cut card; il mescolamento (`shuffle`) rimuove e re-inserisce la cut card nella posizione corretta dopo
lo shuffle casuale, per mantenere invariata la garanzia sul numero di carte residue.

## Player e SplitPlayer

`Player` estende `Participant` con `Wallet` (il portafoglio di fiches) e uno stato (`PlayerState`:
Active/Standing/Busted/Blackjack). Il metodo `state` ricalcola dinamicamente `Busted` osservando il
punteggio minimo della mano, evitando stati incoerenti se le carte cambiano. `NormalPlayer` aggiunge il
supporto all'assicurazione tramite il trait `InsuranceSupport`; `SplitPlayer` rappresenta la seconda
mano generata da uno split, condividendo la stessa interfaccia `Player` ma inizializzata con una sola
carta (`splitCard`).

`Wallet.withdraw` implementa un piccolo algoritmo di resto: prova prima una combinazione esatta di
fiches (dalla taglia più alta), e se non è possibile individua la fiche più piccola sufficiente a coprire
il residuo, restituendo il resto in fiches più piccole.

## Assicurazione

- **`handleInsurances(names)`** — per ogni giocatore il cui nome è nella lista, aumenta la puntata
  corrente di metà del valore originale (il costo dell'assicurazione).
- **`resolveInsurances()`** — una volta rivelata la carta del banco, per ogni giocatore assicurato
  rimuove il sovrapprezzo dell'assicurazione riportando la puntata al valore originale, e — solo se il
  banco ha Blackjack — corrisponde la vincita dell'assicurazione, restituendo l'elenco di nome/importo
  vinto per ciascun giocatore assicurato.
