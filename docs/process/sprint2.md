---

title: Sprint 2
nav_order: 4
parent: Processo Scrum

---

# Sprint 2

**Iterazione:** seconda settimana di sviluppo (le date sono indicative e da verificare).

## Obiettivo

Realizzare la parte centrale di una mano di Blackjack: la distribuzione iniziale delle carte, il calcolo del punteggio (con la
doppia valenza dell'Asso), il turno di gioco di ogni singolo giocatore e il turno automatico del banco. Al termine dello
sprint una mano può essere giocata dall'inizio fino alla determinazione del punteggio finale del banco.

## Sprint Backlog

| Feature                              | Sprint Task                                                                                          | Volontario      | Stima | Completato |
|--------------------------------------|-----------------------------------------------------------------------------------------------------|-----------------|:-----:|:----------:|
| Gestione inizio mano                 | Calcolo del punteggio del giocatore (Asso 1 o 11)                                                    | Nicholas        |   7   |     ✓      |
|                                      | Creare il tipo `Card` (con stampa) e il tipo `Deck`                                                  | Anna            |   7   |     ✓      |
|                                      | Distribuzione di due carte a ogni giocatore (metodo di inizializzazione mano nel `Game`)            | Elena, Anna     |   7   |     ✓      |
|                                      | Modulo `Participant` con le caratteristiche comuni a `Dealer` e `Player` (nome, carte, addCard, score) | Anna         |   7   |     ✓      |
|                                      | Modulo `Dealer` con implementazione                                                                  | Anna            |   3   |     ✓      |
|                                      | Modellazione mano del giocatore, aggiunta carta e stampa                                             | Elena           |   4   |     ✓      |
|                                      | Stampa a video delle carte dei giocatori dopo la prima distribuzione                                | Elena           |   4   |     ✓      |
|                                      | Metodo nel `Game` per rilevare i Blackjack iniziali, aggiornare il balance e lo stato               | Nicholas        |   4   |     ✓      |
| Gestione turno di un singolo giocatore | Nuovo `Command` nella view per stampare i giocatori che hanno fatto Blackjack                     | Nicholas, Elena |   2   |     ✓      |
|                                      | Meccanismo per aggiungere una carta su richiesta del giocatore o fermarsi                            | Nicholas        |   4   |     ✓      |
|                                      | Interazione con la view per le richieste degli utenti                                                | Anna            |   6   |     ✓      |
|                                      | Meccanismo per calcolare se un giocatore ha sballato (*busted*)                                      | Nicholas        |   2   |     ✓      |
|                                      | Richiesta automatica di carte fino al raggiungimento della soglia                                   | Anna            |   3   |     ✓      |
| Gestione turno del banco             | Metodo `revealCards` nel `Dealer` per scoprire la carta coperta                                      | Anna            |   2   |     ✓      |
|                                      | Verificare la conclusione della mano rispetto alle carte del banco                                  | Elena           |   1   |     ✓      |

## Sprint Review

Tutti i task sono stati completati. È stato introdotto il modulo `Participant` come astrazione comune a `Player` e
`Dealer`, il modulo `Score` con il calcolo del punteggio delegato a un motore **Prolog**, e la logica di distribuzione e
turni. Lo sprint si conclude con la release **`v0.2.0`** (gestione inizio mano, turno del giocatore, turno del banco).
