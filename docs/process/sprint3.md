---

title: Sprint 3
nav_order: 5
parent: Processo Scrum

---

# Sprint 3

**Iterazione:** terza settimana di sviluppo (le date sono indicative e da verificare).

## Obiettivo

Completare il ciclo di vita di una partita, gestendo il pagamento dei giocatori a fine mano, la fine della partita
(tramite la *cut card*) e la preparazione di una nuova mano. In parallelo, introdurre le prime funzionalità avanzate:
il **raddoppio** (*double down*) e lo **split**.

## Sprint Backlog

| Feature                            | Sprint Task                                                                                                | Volontario       | Stima | Completato |
|------------------------------------|------------------------------------------------------------------------------------------------------------|------------------|:-----:|:----------:|
| Gestione fine mano e fine partita  | Metodo di pagamento dei giocatori a fine mano e aggiornamento del balance del banco (con gestione degli split) | Nicholas, Elena |  10   |     ✓      |
|                                    | Rivedere il meccanismo per chiedere ai giocatori se vogliono uscire                                        | Elena            |   4   |     ✓      |
|                                    | Metodo `startNewHand` nel `Game` (invocato dal controller a fine mano)                                     | Nicholas         |   3   |     ✓      |
|                                    | Controllo che non esista già un giocatore con lo stesso nome                                               | Elena            |   3   |     ✓      |
|                                    | Definire la `CutCard` come caso di `Card`, inserirla in `generateDeck()` e adattare `drawCard`            | Elena            |   7   |     ✓      |
|                                    | Verificare la presenza della *cut card* nel mazzo per capire se la partita deve terminare                 | Elena            |   2   |     ✓      |
|                                    | A fine partita, liquidare i giocatori con il balance residuo convertito in euro                           | Elena            |   3   |     ✓      |
| Gestione split e raddoppio         | Scelta "Double Down" nella view (solo con due carte in mano); raddoppio della puntata, pesca e stand      | Nicholas         |   7   |     ✓      |
|                                    | Scelta "Split" nella view (con due carte di uguale valore); creazione di uno `SplitPlayer`                | Anna             |  10   |     ✓      |
|                                    | Classe `SplitPlayer` che estende `Player`, inizializzata con una delle carte del giocatore                | Anna             |   8   |     ✓      |
|                                    | Regola `canSplit` sul `Player` con controllo del caso particolare degli Assi                              | Anna             |   2   |     ✓      |
|                                    | Gestione della puntata dello `SplitPlayer` e inserimento nel giro dei turni                                | Anna             |   5   |     ✓      |
|                                    | Trasferimento del balance residuo dal giocatore allo `SplitPlayer` a fine turno                           | Anna             |   4   |     ✓      |

## Sprint Review

Tutti i task sono stati completati. È stata introdotta la `CutCard` come meccanismo di terminazione della partita e le
funzionalità di split e raddoppio, con la classe `SplitPlayer`. Lo sprint si conclude con la release **`v0.3.0`**
(gestione fine mano e fine partita, split e raddoppio).
