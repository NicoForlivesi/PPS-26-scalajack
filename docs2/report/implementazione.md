---

title: Implementazione
nav_order: 5
parent: Report
has_children: true

---

# Implementazione

Questa sezione approfondisce gli aspetti implementativi più rilevanti, con particolare attenzione alle **tecniche
avanzate di Scala** e alla componente in **Prolog**. La trattazione è organizzata per componente:

- [Model](impl/model.md) — *opaque type*, tipi algebrici (`enum`), *extension method*, *mixin* e ricorsione.
- [Integrazione Scala–Prolog](impl/prolog.md) — calcolo del punteggio delegato a un motore logico.
- [View](impl/view.md) — input validato e *context abstractions*.
- [Controller](impl/controller.md) — composizione di effetti con Cats Effect `IO`.

## Tecniche di Scala adottate — quadro d'insieme

Il progetto fa uso estensivo di elementi avanzati del linguaggio:

- **Tipi algebrici** tramite `enum` (per esempio `Card`, `Suit`, `Value`, `PlayerState`, `PlayerAction`, `Command`);
- **Opaque type** per il `Deck`, che espone una lista di carte solo attraverso l'API desiderata;
- **Extension method** per arricchire tipi esistenti (`StandardCard`, `Deck`, `List[Fiche]`, `List[StandardCard]`);
- **Mixin** e linearizzazione dei *trait* (`Player extends Participant with Wallet`);
- **Context abstractions** (`using`/`given`) per la `Console[IO]` e per le conversioni verso i termini Prolog;
- **Monade `IO`** di Cats Effect per la gestione pura degli effetti;
- **Ricorsione in coda** (`@tailrec`) e *higher-order function* (`fold`, `map`, `collect`, `traverse_`);
- **`export`** per comporre l'API pubblica dei moduli in punti di accesso unici (`ModelExports`, `GameUIExports`).
