---
title: Requirement specification
parent: Report
nav_order: 2
---

# Requirement specification

## Business requirement

- **BR1** — Realizzare un'implementazione software del gioco da casinò Black Jack, giocabile da più
  utenti umani contemporaneamente, ciascuno in sfida individuale contro il banco.

## Functional requirements

### Requisiti utente

| ID | Descrizione | Priorità |
|:---|:------------|:---------|
| FR1 | L'utente deve poter specificare, all'avvio, il numero di giocatori umani al tavolo. | Obbligatorio |
| FR2 | Ogni giocatore deve poter specificare il proprio nome e il proprio deposito iniziale in fiches. | Obbligatorio |
| FR3 | Ogni giocatore deve poter piazzare una puntata prima dell'inizio di ciascuna mano. | Obbligatorio |
| FR4 | Durante il proprio turno, ogni giocatore deve poter richiedere carte aggiuntive o fermarsi. | Obbligatorio |
| FR5 | Un giocatore deve poter abbandonare il tavolo (es. saldo esaurito). | Obbligatorio |
| FR6 | Un giocatore deve poter richiedere il **raddoppio** (double down) quando possiede esattamente due carte. | Opzionale |
| FR7 | Un giocatore deve poter richiedere lo **split** della propria mano quando le due carte hanno lo stesso valore. | Opzionale |
| FR8 | Un giocatore deve poter richiedere l'**assicurazione** quando il banco mostra un Asso scoperto. | Opzionale |
| FR9 | L'utente deve poter configurare la presenza di bot che giocano insieme ai giocatori umani. | Opzionale |

### Requisiti di sistema

| ID | Descrizione |
|:---|:------------|
| SR1 | Il sistema distribuisce due carte scoperte a ciascun giocatore e due carte (una coperta, una scoperta) al banco a inizio mano. |
| SR2 | Il sistema calcola automaticamente il punteggio di una mano, gestendo il valore ambiguo dell'Asso (1 oppure 11). |
| SR3 | Il sistema dichiara automaticamente "sballato" (bust) un giocatore la cui somma supera 21, terminandone il turno. |
| SR4 | Al termine dei turni di tutti i giocatori, il sistema scopre la carta coperta del banco e ne automatizza il turno, pescando finché il punteggio non è ≥ 17. |
| SR5 | Il sistema determina automaticamente i vincitori di ogni mano confrontando il punteggio di ciascun giocatore non sballato con quello del banco. |
| SR6 | Il sistema gestisce il pagamento delle vincite: 1:1 in caso di vittoria normale, 2,5:1 in caso di Blackjack (21 con due carte), restituzione della puntata in caso di pareggio, perdita della puntata in caso di sconfitta. |
| SR7 | Il sistema aggiorna il saldo (fiches) di ogni giocatore a inizio e fine mano. |
| SR8 | Il sistema segnala la fine della partita quando le carte nel mazzo si esauriscono, tramite un meccanismo di "carta di taglio" (cut card) inserita a una distanza dalla fine proporzionale al numero di partecipanti. |
| SR9 | *(opzionale)* Il sistema gestisce automaticamente il comportamento dei bot, che seguono una logica di gioco automatica analoga a quella del banco. |

## Non-functional requirements

| ID | Descrizione |
|:---|:------------|
| NFR1 | L'applicazione deve essere fruibile tramite interfaccia a riga di comando (CLI). |
| NFR2 | *(opzionale)* L'applicazione deve poter essere fruita anche tramite interfaccia grafica (GUI). |
| NFR3 | Il sistema deve segnalare in modo chiaro all'utente ogni input non valido (nomi duplicati, puntate non ammesse, numero di giocatori fuori range), permettendo di correggerlo senza terminare l'applicazione. |
| NFR4 | La logica di dominio deve essere verificabile tramite test automatici indipendenti dall'interfaccia utente. |

## Implementation requirements

| ID | Descrizione |
|:---|:------------|
| IR1 | Il progetto è sviluppato in **Scala 3** (versione 3.3.5), gestito con **sbt**. |
| IR2 | La gestione degli effetti collaterali (I/O da console) è delegata alla libreria **Cats Effect**. |
| IR3 | Il calcolo del punteggio delle mani è delegato a un motore **Prolog** integrato (**tuProlog**), tramite un adattatore Scala dedicato. |
| IR4 | I test automatici sono realizzati con **ScalaTest**, con supporto a **Mockito** per il mocking e **junit-interface** come test runner. |
