---

title: Testing
nav_order: 6
parent: Report

---

# Testing

## TDD

Lo sviluppo è stato guidato principalmente dal **Test-Driven Development (TDD)**: per la maggior parte delle funzionalità
il test è stato scritto prima dell'implementazione, seguendo il ciclo *red–green–refactor*, senza però applicarlo in modo
rigido a ogni singola riga di codice. La strategia di test copre in modo completo tutti i componenti: *model*, *view* e
*controller*. La verifica di *view* e *controller* è resa possibile dal fatto che l'interazione da terminale è modellata
come effetto `IO`: fornendo un *test double* della `Console[IO]` è possibile simulare l'input dell'utente e controllare
l'output in modo deterministico (si veda oltre).

## ScalaTest

Per i test è stata utilizzata la libreria **ScalaTest**, nello stile `AnyFunSuite` con i `Matchers` (`shouldBe`) per
favorire la leggibilità. Coerentemente con i principi di *clean testing*, ogni test verifica un **singolo concetto** ed
è indipendente dagli altri. Gli import comuni ai test sono raccolti nell'oggetto `TestExports`, in modo simmetrico agli
*export* del codice di produzione.

Un esempio dai test del modulo `Score`, che verifica la corretta gestione della doppia valenza dell'Asso:

```scala
test("calculateScore reads a single ace as 1 or 11 when the high option does not bust"):
  calculateScore(List(card(Value.Ace), card(Value.Six))) shouldBe Score(7, 17)

test("calculateScore never raises more than one ace"):
  calculateScore(List(card(Value.Ace), card(Value.Ace), card(Value.Nine))) shouldBe Score(11, 21)

test("isBlackjack is false for three cards even if the sum is 21"):
  isBlackjack(List(card(Value.Ace), card(Value.Five), card(Value.Five))) shouldBe false
```

## Testabilità di view e controller

Poiché l'interazione con l'utente è modellata come effetto `IO` di **Cats Effect** e le funzioni della *view*
dipendono da una `Console[IO]` passata come *context parameter* (`using`), è possibile testare *view* e *controller* in
modo **deterministico** senza I/O reale. Nei test viene fornita una implementazione di comodo (*test double*) di
`Console[IO]` che simula l'input dell'utente tramite un `Iterator` di stringhe e cattura l'output in una lista, valutando
poi il programma con `unsafeRunSync()`:

```scala
def mockConsoleWith(readLineBehavior: () => String): Console[IO] = new Console[IO]:
  override def readLine: IO[String] = IO(readLineBehavior())
  override def println[A](a: A)(using S: Show[A]): IO[Unit] = IO:
    outputMessages = outputMessages :+ S.show(a)
  // ...

test("Players count should match the requested number."):
  val simulatedInputs = Iterator("P1, P2", "50", "100")
  given Console[IO] = mockConsoleWith(() => simulatedInputs.next())
  getPlayers(2).unsafeRunSync().length shouldBe 2
```

Un aspetto delicato è la presenza di **casualità** nello stato di gioco (mescolamento del mazzo, carte del banco). Per
evitare test *flaky*, dove necessario viene iniettato un mazzo deterministico tramite l'apposito costruttore
`Game(players, Deck.testDeck(...))`, così da controllare esattamente le carte distribuite.

## Copertura

La suite comprende i test di tutti i componenti: i moduli del *model* (`Score`, `Deck`, `Fiche`, `Player`, `Dealer`,
`Game`), la *view* (`CLIView`) e il *controller*. È eseguibile con il comando `sbt test`. Non è stato integrato uno
strumento automatico di misurazione della copertura: la completezza è stata perseguita scrivendo i test contestualmente
allo sviluppo e coprendo esplicitamente i casi limite (per esempio mani con più Assi, sballamento, Blackjack, split e
assicurazione).
