---

title: Model
nav_order: 1
parent: Implementazione
grand_parent: Report

---

# Implementazione del Model

## Tipi algebrici con `enum`

Le carte sono modellate come un **tipo algebrico** tramite `enum`. In particolare `Card` è una somma di due casi, di cui
uno parametrico (`StandardCard`) e uno senza parametri (`CutCard`), il che consente di trattare in modo uniforme le
carte normali e il segnalibro di fine mazzo:

```scala
enum Suit:
  case Hearts, Spades, Clubs, Diamonds

enum Value:
  case Ace
  case Two, Three, Four, Five, Six, Seven, Eight, Nine, Ten
  case Jack, Queen, King

enum Card:
  case StandardCard(suit: Suit, value: Value, isFaceUp: Boolean = true)
  case CutCard
```

Lo stesso stile è applicato agli stati e ai comandi del gioco (`PlayerState`, `PlayerAction`),
sfruttando l'esaustività del *pattern matching* per gestirne tutti i casi.

## Opaque type per il mazzo

Il `Deck` è realizzato come **opaque type** sopra una `List[Card]`: all'esterno del modulo il mazzo non è una lista
qualsiasi, ma un tipo con una propria API (`draw`, `shuffle`, `generateDeck`, `size`), impedendo usi impropri e
preservando gli invarianti (come la presenza della *cut card*).

```scala
opaque type Deck = List[Card]
```

## Extension method

Le operazioni sui tipi sono espresse tramite **extension method**, che arricchiscono i tipi esistenti senza inquinarne
la definizione. Per esempio la pesca e il mescolamento del mazzo, o il valore totale di un insieme di fiches:

```scala
extension (d: Deck)
  def draw(): (Option[Card], Deck) = d match
    case h :: t => (Some(h), t)
    case _      => (None, d)

extension (fiches: List[Fiche])
  def totalValue: Double = fiches.map(_.value).sum
```

## Mixin e linearizzazione dei trait

La composizione di comportamenti avviene tramite **mixin**. Il giocatore combina la gestione delle carte
(`Participant`) e quella del saldo (`Wallet`), mentre le sue varianti aggiungono ulteriori aspetti:

```scala
trait Player(initialBalance: Double) extends Participant with Wallet:
  // stato, stand(), bust(), prepareForNewHand(), ...

class NormalPlayer(override val name: String, val initialBalance: Double)
  extends Player(initialBalance) with InsuranceSupport
```

## Ricorsione in coda e funzioni di ordine superiore

Le elaborazioni sulle collezioni sfruttano *higher-order function* e, dove il calcolo è iterativo, la **ricorsione in
coda** annotata con `@tailrec`. Un esempio è il turno automatico di banco e bot, unificato in un'unica funzione che
pesca finché non è raggiunta la soglia:

```scala
@tailrec
def extractUntilSeventeen(acc: List[String]): List[String] =
  if hasFinished() then acc
  else
    drawCard(participant) match
      case Some(card) => extractUntilSeventeen(acc :+ msg)
      case _          => List.empty
```

Anche la conversione di un importo in fiches è espressa in forma funzionale: `fromAmount`
accumula con una `foldLeft` sui tagli, dal più grande al più piccolo, il resto ancora da
coprire e le fiches via via selezionate:

```scala
def fromAmount(amount: Double): List[Fiche] =
  denominations.foldLeft((amount, List.empty[Fiche])):
    case ((remaining, acc), fiche) =>
      val count: Int = (remaining / fiche.value).toInt
      (remaining - count * fiche.value, acc ::: List.fill(count)(fiche))
  ._2
```

Con lo stesso stile è realizzato il prelievo dal portafoglio (`withdraw`), che seleziona le
fiches da mantenere a partire dalle più grandi e gestisce l'eventuale resto.

## Pattern matching per le vincite

Il calcolo del pagamento a fine mano combina più condizioni (giocatore sballato, banco sballato, Blackjack del
giocatore, Blackjack del banco) in un unico *pattern matching* su tupla, rendendo esplicite e leggibili tutte le
casistiche delle regole del Blackjack:

```scala
val payout: Double = (playerBusted, dealerBusted, playerBJ, dealerBJ) match
  case (true, _, _, _) | (_, _, false, true) => 0
  case (_, _, true, true)                    => bet
  case (_, _, true, false)                   => bet * BlackjackPayoutMultiplier
  case (_, true, _, _)                       => bet * 2
  case _ if playerScore > dealerScore        => bet * 2
  case _ if playerScore == dealerScore       => bet
  case _                                     => 0
```

## Export e API pubblica dei moduli

Per offrire punti di accesso unici all'API dei moduli si è usata la clausola **`export`**: gli oggetti `ModelExports` e
`GameUIExports` raccolgono e ri-esportano i tipi e le operazioni pubbliche, semplificando gli import nel resto del
codice e nei test.

*Contributi principali: `Score` e regole di punteggio — Nicholas; `Card`/`Deck` — Anna; `CutCard`, mano e stampa dei
giocatori — Elena; oggetti di `export` — Elena; conversione valuta–fiches — Nicholas.*
