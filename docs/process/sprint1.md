---

title: Sprint 1
nav_order: 3
parent: Processo Scrum

---

# Sprint 1

**Iterazione:** prima settimana di sviluppo (le date sono indicative e da verificare).

## Obiettivo

Realizzare le fondamenta economiche del gioco: la gestione del portafoglio dei giocatori tramite le fiches, la
conversione tra valuta e fiches, la scelta della puntata per ogni mano e l'uscita di un giocatore dal tavolo. Al termine
dello sprint deve essere possibile avviare una partita, registrare i giocatori con il proprio saldo iniziale e gestirne
puntate e abbandono.

## Sprint Backlog

| Feature                                                              | Sprint Task                                                            | Volontario       | Stima | Completato |
|---------------------------------------------------------------------|-----------------------------------------------------------------------|------------------|:-----:|:----------:|
| Un giocatore deve poter ritirare fiches e riconvertirle in valuta   | Creare il tipo di dato giocatore                                       | Anna, Nicholas   |   2   |     ✓      |
|                                                                     | Creare il tipo di dato fiches                                          | Nicholas         |   1   |     ✓      |
|                                                                     | Creare il meccanismo di conversione tra valuta e fiches               | Nicholas         |   2   |     ✓      |
|                                                                     | View per ricevere il balance iniziale                                 | Elena            |   2   |     ✓      |
|                                                                     | Meccanismo per collegare la view al model (controller)                | Elena            |   2   |     ✓      |
| Un giocatore deve poter decidere quanto puntare per ogni mano       | Creazione del tipo partita (`Game`)                                   | Anna             |   2   |     ✓      |
|                                                                     | Creazione del tipo puntata (`Bet`)                                    | Anna             |   2   |     ✓      |
|                                                                     | Nella view, richiedere la puntata per la mano corrente                | Elena            |   3   |     ✓      |
| Un giocatore deve poter lasciare una partita                        | Richiedere nella view se il giocatore vuole uscire                    | Elena            |   2   |     ✓      |
|                                                                     | Meccanismo per rimuovere un giocatore dalla partita                   | Anna             |   2   |     ✓      |
|                                                                     | Verificare se restano giocatori al tavolo e chiudere la partita       | Anna             |   2   |     ✓      |
|                                                                     | Rimozione automatica del giocatore che esaurisce il balance           | Elena            |   2   |     ✓      |

## Sprint Review

Tutti i task pianificati sono stati completati. Sono stati introdotti i tipi fondamentali del dominio (`Game`, `Bet`,
`Player`, `Fiche`) e l'ossatura del pattern MVC, con la prima interazione tra view e controller. Lo sprint si conclude
con la release **`v0.1.0`**, che comprende ritiro e riconversione delle fiches, scelta della puntata e uscita dal tavolo.
