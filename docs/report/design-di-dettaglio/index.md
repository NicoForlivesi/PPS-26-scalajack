---
title: Design di dettaglio
parent: Report
nav_order: 5
has_children: true
permalink: /report/design-di-dettaglio/
---

# Design di dettaglio

Il design di dettaglio è documentato per parti di competenza, in base a chi ha progettato e implementato
ciascun modulo. La progettazione del cuore di `GameModule` — l'interfaccia `Game` che coordina turni,
giocatori e pagamenti — è stata invece definita e rivista collettivamente da tutto il gruppo, essendo il
punto di intersezione tra le responsabilità di ciascuno.

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
