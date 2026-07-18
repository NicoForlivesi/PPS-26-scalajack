---

title: Sprint 4
nav_order: 6
parent: Processo Scrum

---

# Sprint 4

**Iterazione:** quarta settimana di sviluppo (le date sono indicative e da verificare).

## Obiettivo

Completare le funzionalità avanzate opzionali del gioco: l'**assicurazione** (offerta quando la carta scoperta del banco
è un Asso) e i **giocatori automatici** (*bot*), che riempiono i posti liberi al tavolo fino al massimo di sette
partecipanti e giocano seguendo una strategia deterministica.

## Sprint Backlog

| Feature       | Sprint Task                                                                                                     | Volontario | Stima | Completato |
|---------------|----------------------------------------------------------------------------------------------------------------|------------|:-----:|:----------:|
| Assicurazione | Chiedere quali giocatori vogliono l'assicurazione quando la prima carta del banco è un Asso                    | Elena      |   4   |     ✓      |
|               | Prelevare dal balance dei giocatori assicurati una quota pari a 0,5 della puntata e impostare `hasInsurance`    | Anna       |   3   |     ✓      |
|               | Metodo `resolveInsurances` che risolve l'assicurazione e paga in caso di Blackjack del banco                   | Nicholas   |   6   |     ✓      |
|               | Invocare la risoluzione dell'assicurazione in `handleDealerTurn` e mostrare le vincite nella view              | Elena      |   5   |     ✓      |
| Bot           | `BotPlayer` con balance e puntata casuali; metodo nel `Game` per istanziare i bot fino a 7 partecipanti        | Nicholas   |   6   |     ✓      |
|               | Aggiungere la puntata al `toString` del giocatore e override del `toString` del bot                            | Nicholas   |   1   |     ✓      |
|               | Metodo nel `Game` che esegue il turno automatico di un bot, validando la puntata (refactoring comune col banco) | Elena      |   6   |     ✓      |
|               | Stampa a video del numero di bot creati dopo l'istanziazione                                                    | Elena      |   2   |     ✓      |
|               | Creazione dell'oggetto di *export* `ModelExports`/`GameUIExports` (`MyImports`)                                 | Elena      |   5   |     ✓      |

## Sprint Review

Tutti i task sono stati completati. Sono state aggiunte l'assicurazione (con `resolveInsurances`) e i giocatori
automatici (`BotPlayer`), oltre al refactoring che unifica il comportamento automatico di bot e banco
(`computeAutomaticTurn`) e agli oggetti di *export* che raccolgono l'API pubblica dei moduli. Lo sprint si conclude con
la release **`v0.4.0`** (assicurazione e gestione dei bot).
