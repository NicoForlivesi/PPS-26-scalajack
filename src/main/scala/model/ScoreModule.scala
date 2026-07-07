package model

import model.DeckModule.Card
import alice.tuprolog.{Int as _, *} // Escludo l'int di tuprolog sennò sovrascrive l'Int di Scala
import Scala2P.{*, given}

object ScoreModule:

  private val WinningScore = 21

  /** Represents the score of a participant's hand, expressed as two possible
   * readings due to the ambiguous value of the Ace (1 or 11).
   *
   * @param minValue The total obtained by counting evey aces as 1.
   * @param maxValue The total obtained by counting at most one ace in the hand as 11,
   *                 it is equal to `minValue` when the hand contains no Ace.
   */
  case class Score(minValue: Int, maxValue: Int):

    override def toString: String = (minValue, maxValue) match
      case (min, max) if min != max && max <= WinningScore => s"$min / $max"
      case _ => minValue.toString

  private val engine: Term => LazyList[Term] = mkPrologEngine(
    """
    value(two, 2).
    value(three, 3).
    value(four, 4).
    value(five, 5).
    value(six, 6).
    value(seven, 7).
    value(eight, 8).
    value(nine, 9).
    value(ten, 10).
    value(jack, 10).
    value(queen, 10).
    value(king, 10).

    low_value(ace, 1) :- !.
    low_value(C, V) :- value(C, V).

    low_sum([], 0).
    low_sum([C|Cs], S) :- low_value(C, V), low_sum(Cs, S1), S is V + S1.

    has_ace(Cards) :- member(ace, Cards).

    % Se ce un asso, posso alzarne esattamente uno da 1 a 11
    % se non ce nessun asso Low = High.
    raised(Cards, Low, High) :- has_ace(Cards), !, High is Low + 10.
    raised(_, Low, Low).

    score(Cards, pair(Low, High)) :-
      low_sum(Cards, Low),
      raised(Cards, Low, High).

    blackjack([C1, C2]) :- score([C1, C2], pair(_, 21)).
    """
  )

  private def toAtoms(cards: List[Card]): Seq[String] =
    cards.map(_.value.toString.toLowerCase)

  def calculateScore(cards: List[Card]): Score =
    if cards.isEmpty then Score(0, 0)
    else
      val pair = engine(Struct("score", toAtoms(cards), Var())).map(extractTerm(_, 1)).head
      Score(extractTerm(pair, 0).toString.toInt, extractTerm(pair, 1).toString.toInt)

  def isBusted(cards: List[Card]): Boolean =
    calculateScore(cards).minValue > WinningScore

  def isBlackjack(cards: List[Card]): Boolean =
    cards.size == 2 && engine(Struct("blackjack", toAtoms(cards))).nonEmpty