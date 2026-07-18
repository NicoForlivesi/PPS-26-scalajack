---

title: Product Backlog
nav_order: 1
parent: Processo Scrum

---

# Product Backlog

Il *Product Backlog* raccoglie le funzionalità del gioco ordinate per priorità. Ad ogni voce è associata una **stima
iniziale** (un valore che ne esprime il peso, ovvero l'effort complessivo previsto). Al termine di ogni sprint le stime
vengono rivalutate: il valore riportato nella colonna di uno sprint indica l'effort ancora *residuo* per quella voce
alla fine dell'iterazione, per cui il valore `0` corrisponde a una funzionalità completata.

| Priorità | Item                                                                             | Stima iniziale | Sprint 1 | Sprint 2 | Sprint 3 | Sprint 4 |
|:--------:|----------------------------------------------------------------------------------|:--------------:|:--------:|:--------:|:--------:|:--------:|
|    1     | Un giocatore deve poter ritirare un numero di fiches a scelta e riconvertirle in valuta |       12       |    0     |          |          |          |
|    2     | Un giocatore deve poter decidere quanto puntare per ogni mano                     |       7        |    0     |          |          |          |
|    3     | Un giocatore deve poter lasciare una partita                                     |       5        |    0     |          |          |          |
|    4     | Gestione inizio mano                                                              |       7        |    7     |    0     |          |          |
|    5     | Gestione turno di un singolo giocatore                                            |       14       |    14    |    0     |          |          |
|    6     | Gestione turno del banco                                                          |       3        |    3     |    0     |          |          |
|    7     | Gestione fine mano e fine partita                                                 |       7        |    7     |    7     |    0     |          |
|    8     | Gestione di split, raddoppio e assicurazione                                      |       10       |    10    |    10    |    5     |    0     |
|    9     | Gestione di un giocatore bot                                                      |       5        |    5     |    5     |    5     |    0     |

Le prime tre voci coprono i requisiti economici di base (fiches, puntate, uscita dal tavolo); le voci 4–7 realizzano il
ciclo completo di una mano di Blackjack (distribuzione, turni dei giocatori, turno del banco, pagamenti e fine partita);
le voci 8–9 aggiungono le funzionalità avanzate opzionali (split, raddoppio, assicurazione e giocatori automatici).

Ogni sprint si è concluso con una release incrementale pubblicata su GitHub: Sprint 1 → `v0.1.0`, Sprint 2 → `v0.2.0`,
Sprint 3 → `v0.3.0`, Sprint 4 → `v0.4.0`.
