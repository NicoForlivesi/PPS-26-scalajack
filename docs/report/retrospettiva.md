---

title: Retrospettiva
nav_order: 7
parent: Report

---

# Retrospettiva

## Processo di sviluppo

Il processo di sviluppo che il gruppo si era prefissato è stato rispettato: sono stati svolti 4 sprint, ognuno
con il relativo **Sprint Backlog** e una **review** conclusiva, mantenendo aggiornato il
[Product Backlog](../process/product_backlog.md) ad ogni iterazione. Gli sprint sono stati così suddivisi:

1. [Sprint 1](../process/sprint1.md) — fondamenta economiche (fiches, puntate, uscita dal tavolo);
2. [Sprint 2](../process/sprint2.md) — inizio mano, turno del giocatore, turno del banco;
3. [Sprint 3](../process/sprint3.md) — fine mano e fine partita, split e raddoppio;
4. [Sprint 4](../process/sprint4.md) — assicurazione e giocatori automatici.

La quasi totalità dei task pianificati per ogni sprint è stata completata all'interno dello sprint stesso. Il lavoro è
stato svolto prevalentemente in modo individuale sui rispettivi task, con alcune parti comuni (in particolare il
`Game`, punto di incontro di quasi tutte le funzionalità) progettate e realizzate in collaborazione; lo stesso vale per
la stesura della documentazione.

## Git Workflow

È stata adottata una strategia di versionamento a **due branch**:

- `master`, contenente esclusivamente le versioni stabili corrispondenti alle release;
- `develop`, linea principale di sviluppo condivisa dal gruppo.

Poiché il repository è condiviso e tutti i componenti hanno accesso in scrittura, l'accento è stato posto sulle pratiche
di collaborazione: suddivisione della *ownership* dei file per ridurre i conflitti, `pull` prima di iniziare e prima di
ogni `push`, commit piccoli e frequenti e utilizzo degli strumenti di merge di IntelliJ IDEA per le parti comuni.

Le **release** sono state prodotte in modo **incrementale**, una per sprint, e pubblicate manualmente dalla sezione
*Releases* di GitHub seguendo il **Semantic Versioning**:

- `v0.1.0` — ritiro e riconversione delle fiches, scelta della puntata, uscita dalla partita;
- `v0.2.0` — gestione dell'inizio mano, del turno del giocatore e del turno del banco;
- `v0.3.0` — gestione di fine mano e fine partita, split e raddoppio;
- `v0.4.0` — gestione dell'assicurazione e dei giocatori automatici;
- `v1.0.0` — versione finale: relazione completa e artefatto eseguibile.

## Valutazione conclusiva

Il progetto ha raggiunto tutti i requisiti obbligatori (funzionalità di base del Blackjack con più giocatori, utilizzo
tramite CLI, gestione del saldo di ogni giocatore) e la maggior parte dei requisiti opzionali (raddoppio, split,
assicurazione e giocatori *bot*). L'integrazione multi-paradigma si è rivelata utile: delegare il calcolo
del punteggio a un motore **Prolog** ha permesso di esprimere in modo dichiarativo e conciso una regola altrimenti
verbosa.

Il ricorso al TDD ha dato solidità alla logica di gioco e ha reso agevoli i numerosi *refactoring*, tra cui
l'unificazione del comportamento automatico di banco e bot (`computeAutomaticTurn`) e l'introduzione degli oggetti di
*export* per l'API pubblica dei moduli.

Tra i possibili **miglioramenti futuri**: l'adozione di una pipeline di **Continuous Integration** (per esempio con
GitHub Actions) per automatizzare l'esecuzione dei test e delle release, l'introduzione di uno strumento di misurazione
della **copertura** e la realizzazione di una **interfaccia grafica** (GUI), prevista come requisito opzionale e non
sviluppata in questa versione.
