---
title: Design di dettaglio
parent: Report
nav_order: 5
has_children: true
permalink: /report/design-di-dettaglio/
---

# Design di dettaglio

Il design di dettaglio è documentato per parti di competenza, in base a chi ha progettato e implementato
ciascun modulo. Alcune parti, invece, sono state definite e riviste collettivamente da tutto il gruppo,
essendo punti di intersezione tra le responsabilità di ciascuno: il cuore di `GameModule` e i trait base
`Participant` e `Player`.

## Participant e Player: le basi condivise

`Participant` (trait base comune a banco e giocatori: gestione delle carte in mano e calcolo dello score)
e `Player` (trait base dei giocatori: stato di gioco e portafoglio) sono stati progettati e implementati
a più mani, poiché costituiscono l'interfaccia su cui si appoggiano sia le specializzazioni del banco
(`Dealer`) sia quelle dei giocatori (`NormalPlayer`, `SplitPlayer`, `BotPlayer`), di competenza di membri
diversi del gruppo.

## Game: il coordinatore condiviso

`GameModule.Game` è il trait più ampio del progetto ed è stato progettato a più mani, poiché ogni membro
del gruppo doveva integrarvi la propria area di competenza (puntate e pagamenti, split, raddoppio,
assicurazione, blackjack, bot). Le sue responsabilità principali sono:

- gestione dei giocatori e del relativo turno (`players`, `getNextPlayer`, `removePlayer`)
- distribuzione delle carte e verifica del bust (`distributeCards`, `drawCard`, `evaluatePlayerBust`, `evaluateDealerBust`)
- orchestrazione del turno del banco e dei bot (`computeDealerTurn`, `computeBotTurn`)
- calcolo del pagamento finale di ogni mano (`handlePayout`), che tiene conto di vittorie 1:1, blackjack,
  pareggi (push) e sconfitte, incluso il caso particolare in cui più mani da 21 con un numero diverso di
  carte pareggiano comunque tra loro
- gestione del fine mano e del reshuffle del mazzo quando la cut card è stata pescata (`handleHandEnd`)

Le pagine seguenti descrivono in dettaglio i moduli di competenza di ciascun membro.
