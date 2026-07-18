---
title: Anna Malagoli
parent: Implementazione
grand_parent: Report
nav_order: 2
---

# Implementazione — Anna Malagoli

## Deck come opaque type

```scala
opaque type Deck = List[Card]

object Deck:
  def standard(numParticipants: Int): Deck = generateDeck(1, numParticipants)

extension (d: Deck)
  def draw(): (Option[Card], Deck) = d match
    case h :: t => (Some(h), t)
    case _      => (None, d)

  def shuffle(numParticipants: Int): Deck =
    val shuffledDeck: Deck = Random.shuffle(d.filterNot(_ == Card.CutCard))
    addCutCardToDeck(shuffledDeck, numParticipants)
```

Rappresentare `Deck` come tipo opaco permette di esporre solo le operazioni di dominio (`draw`,
`shuffle`, `isEmpty`, `size`), impedendo dall'esterno del modulo qualsiasi manipolazione diretta della
lista di carte sottostante.

## Resto in fiches nel Wallet

```scala
def withdraw(amount: Double): Boolean =
  val sortedFiches = currentBalance.sortBy(-_.value)
  val (keptFiches, remainingAmount) = selectFichesToKeep(sortedFiches, amount)
  if remainingAmount == 0 then
    currentBalance = keptFiches
    true
  else
    keptFiches.sortBy(_.value).find(_.value >= remainingAmount).exists: changeFiche =>
      val change = changeFiche.value - remainingAmount
      currentBalance = keptFiches.diff(List(changeFiche)) ::: (if change > 0 then Fiche.fromAmount(change) else Nil)
      true
```

Il prelievo prova prima una combinazione esatta di fiches; se non è possibile, individua la fiche più
piccola sufficiente a coprire il residuo e restituisce il resto, scomposto a sua volta in fiches tramite
`Fiche.fromAmount`.

## Assicurazione

```scala
def resolveInsurances(): List[(String, Double)] =
  insuredPlayers.map: player =>
    val originalBet = restoreOriginalBet(player)
    val payout = if dealer.hasBlackjack then originalBet * InsurancePayoutMultiplier else 0.0
    if payout > 0 then player.deposit(payout)
    (player.name, payout)
```

*(estratto semplificato a scopo illustrativo dell'idea implementativa in `GameModule.scala`)*: per ogni
giocatore assicurato la puntata viene riportata al valore originale, e — solo se il banco ha Blackjack —
viene accreditata la vincita dell'assicurazione.
