---

title: Processo di sviluppo
nav_order: 1
parent: Report

---

# Processo di sviluppo

Il processo di sviluppo adottato si ispira a **Scrum**, ed è quindi basato su *sprint* iterativi e su un insieme di
*task* pianificati. Viene mantenuto un **Product Backlog** ordinato per priorità, aggiornato al termine di ogni sprint e
scomposto di volta in volta in uno **Sprint Backlog**.

## Ruoli

Il gruppo è composto da **tre sviluppatori paritari** (Nicholas Forlivesi, Anna Malagoli, Elena Sarti). Non è stato
designato un Product Owner formale: le decisioni relative alle priorità del backlog, all'architettura e alle scelte
implementative più rilevanti sono state prese **in modo condiviso** durante gli incontri di pianificazione e di review.
Ogni componente si è assunto la responsabilità di un insieme di task per ciascuno sprint, come documentato negli
*Sprint Backlog*.

## Sprint planning

Gli sprint hanno avuto durata **settimanale**. All'inizio di ogni sprint sono stati definiti l'obiettivo da raggiungere
e, tramite lo Sprint Backlog, i task da svolgere con il relativo assegnatario e una stima dell'effort. Al termine di
ogni sprint è stata effettuata una **Sprint Review** per verificare il lavoro svolto, aggiornare le stime del backlog e
pianificare l'iterazione successiva.

## Definition of Done

Una funzionalità è considerata conclusa quando la relativa logica è coperta da test automatici che hanno successo. Questo
vale per tutti i componenti — *model*, *view* e *controller* — resi verificabili in modo uniforme dalla scelta di
modellare l'I/O da terminale come effetto `IO`. Per le parti interattive si richiede inoltre che l'esecuzione
dell'applicazione risulti utilizzabile e non ambigua. In ogni caso deve essere presente la **Scaladoc** su tutte le API
pubbliche.

## Documentazione

La documentazione è realizzata in formato **Markdown**, contenuta nella directory `docs`, e pubblicata come sito statico
tramite **GitHub Pages** (tema [just-the-docs](https://just-the-docs.com/), diagrammi resi con **Mermaid**). Il codice è
inoltre corredato di **Scaladoc** su tutte le API pubbliche dei moduli.

## Versioning

Il controllo di versione è gestito con **Git** e con una strategia a **due branch**:

- `master`: contiene esclusivamente le versioni stabili, ciascuna corrispondente a una release;
- `develop`: rappresenta la linea principale di sviluppo condivisa dal gruppo.

Il repository è condiviso e tutti i componenti hanno accesso in scrittura. Le pratiche di collaborazione adottate sono:
suddivisione della *ownership* dei file tra i membri per ridurre i conflitti, `pull` prima di iniziare a lavorare e
prima di ogni `push`, commit piccoli e frequenti con messaggi in *imperative mood*, comunicazione rapida nel gruppo in
caso di modifiche a parti comuni e utilizzo degli strumenti di merge integrati in **IntelliJ IDEA**, l'IDE comune al
gruppo.

Si adotta il **Semantic Versioning** nel formato `MAJOR.MINOR.PATCH`. Ogni versione è marcata con un **tag** Git
(`v*.*.*`).

## Release incrementali

Coerentemente con un approccio **Agile**, sono state effettuate **release incrementali**, una per ogni sprint, ognuna
delle quali aggiunge un insieme coeso di funzionalità. Le release sono state pubblicate **manualmente** dalla sezione
*Releases* di GitHub, a partire dal branch `master`:

- **`v0.1.0`** — ritiro e riconversione delle fiches, scelta della puntata, uscita di un giocatore dalla partita;
- **`v0.2.0`** — gestione dell'inizio mano, del turno del singolo giocatore e del turno del banco;
- **`v0.3.0`** — gestione di fine mano e fine partita, split e raddoppio;
- **`v0.4.0`** — gestione dell'assicurazione e dei giocatori automatici (*bot*).

L'elenco completo è disponibile nella pagina
[Releases](https://github.com/NicoForlivesi/PPS-26-scalajack/releases) del repository.

## Strumenti

- **Linguaggio**: Scala 3 (versione 3.3.5), con l'integrazione di **tuProlog** per il calcolo del punteggio.
- **Build tool**: sbt.
- **Librerie principali**: Cats Effect (IO) per la gestione degli effetti, tuProlog per il motore logico.
- **Testing**: ScalaTest (`AnyFunSuite`), con Mockito per alcuni test del controller.
- **IDE**: IntelliJ IDEA (comune a tutto il gruppo).
- **Version control e documentazione**: Git, GitHub e GitHub Pages.
