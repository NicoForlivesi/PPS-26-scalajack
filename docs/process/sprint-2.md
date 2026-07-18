---
title: Sprint 2
parent: Process
nav_order: 3
---

# Sprint 2

## Gestione inizio mano

| Task | Volontari | Stima |
|:-----|:----------|:------|
| Meccanismo per il calcolo del punteggio del giocatore (Asso = 1 o 11) | Nicholas | 7 |
| Creare il tipo Carta (con metodo di stampa) e il tipo Mazzo | Anna | 7 |
| Distribuzione di due carte al giocatore (Controller), con metodo nel Game per l'inizializzazione del turno | Elena, Anna | 7 |
| Modulo con caratteristiche comuni a Dealer e Player (`Participant`): nome, lista carte, `addCard`, stampa, `getScore` | Elena | 7 |
| Modulo per il Dealer, con implementazione | Anna | 3 |
| Modellazione della mano del giocatore, meccanismo per aggiungere carte e stampa del giocatore | Anna | 4 |
| Stampa a video delle carte dei singoli giocatori dopo la prima distribuzione (metodo sul Game) | Elena | 5 |
| Metodo nel Game per individuare chi ha fatto Blackjack, aggiornare il balance e lo stato del giocatore | Nicholas | 4 |
| Meccanismo nella view (nuovo `Command`) per stampare la lista dei giocatori che hanno fatto Blackjack | Nicholas, Elena | 2 |

## Gestione turno di un singolo giocatore

| Task | Volontari | Stima |
|:-----|:----------|:------|
| Meccanismo per far pescare una carta al giocatore su richiesta, o fermarsi (`stand`) | Nicholas | 4 |
| Interazione con la view per raccogliere le richieste degli utenti | Anna | 6 |
| Meccanismo per calcolare se un giocatore è "Busted" | Nicholas | 2 |

## Gestione turno del banco

| Task | Volontari | Stima |
|:-----|:----------|:------|
| Meccanismo per pescare carte finché possibile (soglia 17) | Anna | 3 |
| Meccanismo per scoprire la carta coperta: metodo `revealCards` nel Dealer | Anna | 2 |
| Verificare se la partita è già finita in base alle carte del Dealer (Game) | Elena | 1 |
