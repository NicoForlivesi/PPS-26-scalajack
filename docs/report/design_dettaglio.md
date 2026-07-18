---

title: Design di dettaglio
nav_order: 4
parent: Report
has_children: true

---

# Design di dettaglio

In questa sezione si approfondiscono le scelte di progettazione dei singoli componenti dell'architettura MVC. La
trattazione è organizzata **per componente**; per ciascuno, dove rilevante, si indicano i principali autori dei task
(come da [Processo Scrum](../process.md)), ferma restando la natura collaborativa del *trait* `Game`, punto di
convergenza di quasi tutte le funzionalità.

- [Model](design/model.md) — organizzazione a moduli, gerarchia dei partecipanti, mazzo, punteggio e fiches.
- [View](design/view.md) — comandi, azioni e funzioni di input/output.
- [Controller](design/controller.md) — orchestrazione del ciclo di gioco.

Una scelta trasversale è l'organizzazione del *model* in **moduli**: ogni concetto del dominio è racchiuso in un
`object` (per esempio `GameModule`, `DeckModule`, `PlayerModule`, `ScoreModule`, `FicheModule`, `ParticipantModule`,
`DealerModule`) che espone i propri tipi e operazioni. Questo favorisce **alta coesione** interna e **basso
accoppiamento** tra moduli, e riflette il principio di *information hiding*: l'implementazione concreta è resa privata e
visibile solo attraverso i tipi pubblici del modulo.
