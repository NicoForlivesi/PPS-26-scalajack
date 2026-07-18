---
title: Nicholas Forlivesi
parent: Implementazione
grand_parent: Report
nav_order: 3
---

# Implementazione — Nicholas Forlivesi

## Punteggio calcolato in Prolog

Le regole di punteggio sono espresse come clausole Prolog, valutate tramite un motore tuProlog
incapsulato da `Scala2P`:

```scala
private val engine: Term => LazyList[Term] = mkPrologEngine(
  """
  value(two, 2). value(three, 3). ... value(king, 10).

  low_value(ace, 1) :- !.
  low_value(C, V) :- value(C, V).

  low_sum([], 0).
  low_sum([C|Cs], S) :- low_value(C, V), low_sum(Cs, S1), S is V + S1.

  has_ace(Cards) :- member(ace, Cards).
  raised(Cards, Low, High) :- has_ace(Cards), !, High is Low + 10.
  raised(_, Low, Low).

  score(Cards, pair(Low, High)) :- low_sum(Cards, Low), raised(Cards, Low, High).
  blackjack([C1, C2]) :- score([C1, C2], pair(_, 21)).
  """
)
```

Il motore restituisce le soluzioni come `LazyList[Term]`, valutate pigramente: `ScoreModule` estrae la
prima soluzione (`.head`) per ottenere la coppia `(minValue, maxValue)` del punteggio.

## Fiches generate con un algoritmo greedy

```scala
def fromAmount(amount: Double): List[Fiche] =
  denominations.foldLeft((amount, List.empty[Fiche])):
    case ((remaining, acc), fiche) =>
      val count: Int = (remaining / fiche.value).toInt
      (remaining - count * fiche.value, acc ::: List.fill(count)(fiche))
  ._2
```

I tagli sono ordinati dal più grande al più piccolo (`denominations`); l'algoritmo scorre ciascun taglio
prelevandone il massimo numero possibile, garantendo la scomposizione con il minor numero di fiches.

## BotPlayer

```scala
def computeSafeBet: Int =
  if bet > balance.totalValue then bet = balance.totalValue.toInt
  bet
```

Il bot riduce automaticamente la propria puntata pianificata al saldo effettivamente disponibile, per
evitare di proporre puntate superiori a quanto posseduto.
