---
title: Nicholas Forlivesi
parent: Design di dettaglio
grand_parent: Report
nav_order: 3
---

# Design di dettaglio — Nicholas Forlivesi

Moduli di competenza: **`FicheModule`**, **`ScoreModule`**, **`Scala2P`**, **`BotPlayer`**,
**raddoppio (double down)**, gestione del **Blackjack**.

## FicheModule

Le fiches sono modellate come `enum Fiche` con sei tagli fissi (0.5, 2, 5, 10, 20, 50). Il metodo
`Fiche.fromAmount` converte un importo monetario in una lista di fiches con un **algoritmo greedy**:
scorre i tagli dal più grande al più piccolo, prelevandone il massimo numero possibile ad ogni passo.
Questo è alla base sia del deposito iniziale del giocatore sia del calcolo del resto in `Wallet.withdraw`.

## ScoreModule e Scala2P

Il calcolo del punteggio è delegato a un piccolo motore **Prolog** (tuProlog), incapsulato da
`Scala2P.mkPrologEngine`: dato un insieme di clausole (fatti sul valore delle carte e regole per la
somma "bassa", per l'innalzamento in presenza di un Asso, e per il riconoscimento del Blackjack), il
motore espone una funzione `Term => LazyList[Term]` che restituisce le soluzioni di una query come uno
stream lazy, valutato solo finché servono soluzioni.

`Score` rappresenta il punteggio con una coppia `(minValue, maxValue)`: `minValue` conta ogni Asso come 1,
`maxValue` alza un solo Asso a 11 se possibile. `playableValue` sceglie automaticamente la lettura più
alta che non supera 21, e la `toString` mostra entrambe le letture quando sono diverse (es. `"7 / 17"`).

`Scala2P` fornisce inoltre le `given Conversion` necessarie per costruire i `Term` di tuProlog a partire
da stringhe e sequenze Scala, rendendo la costruzione delle query più idiomatica.

## BotPlayer

`BotPlayer` estende `Player` con un saldo e una puntata iniziali generati casualmente entro un intervallo
configurabile (`randomBalance`, `randomBet`, con step fissi per allinearsi ai tagli delle fiches).
`computeSafeBet` riduce automaticamente la puntata al saldo disponibile se questo è inferiore alla
puntata pianificata, evitando puntate non coperte da fiches. Il turno del bot (`Game.computeBotTurn`)
riusa la stessa soglia di stop del banco (17), pescando automaticamente finché non è raggiunta.

## Raddoppio (Double Down)

`canDoubleDown(player)` verifica che il giocatore abbia esattamente due carte e un saldo sufficiente a
raddoppiare la puntata corrente. `doubleDown(player)` raddoppia l'importo puntato e pesca esattamente
una carta per il giocatore, che termina immediatamente il proprio turno (regola standard del Black Jack:
una sola carta aggiuntiva dopo il raddoppio).

## Gestione del Blackjack

- **`initialBlackjackPlayers()`** — individua i giocatori che hanno ottenuto Blackjack (21 con le prime
  due carte) subito dopo la distribuzione iniziale.
- **`playersWithBlackjack(players)`** — variante generica usata anche dopo eventi come lo split, per
  verificare il Blackjack su un sottoinsieme di giocatori (le mani nate da uno split non possono infatti
  vincere con il payout da Blackjack, ma la funzione resta riutilizzabile).
- **`handleBlackjacks(winners)`** — accredita ai vincitori la puntata moltiplicata per il payout da
  Blackjack (2,5×) e ne aggiorna lo stato a `Blackjack`.
