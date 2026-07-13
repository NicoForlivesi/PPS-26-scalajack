package model // Forse sarebbe meglio metterlo in un package "util" invece che in model?

import alice.tuprolog.*

object Scala2P:
  given Conversion[String, Term] = Term.createTerm(_)
  given Conversion[Seq[_], Term] = _.mkString("[", ", ", "]")

  def extractTerm(t: Term, i: Integer): Term =
    t.asInstanceOf[Struct].getArg(i).getTerm

  /** Costruisce un motore Prolog a partire dalla teoria data e lo espone come una
   * funzione che, dato un goal, restituisce una lazy list delle sue soluzioni.
   *
   * Ho dovuto cambiare qualcosa rispetto a quella delle slide, nella versione delle slide
   * next() chiama sempre engine.solveNext in un blocco finally, ma solveNext lancia un eccezione
   * se non resta aperto nessun ramo usando il cut nella teoria qui creava problemi.
   * Ho fatto in modo che next() chiama solveNext solo se hasOpenAlternatives è vero, altrimenti segna
   * l'iterator come esaurito, così un goal termina in modo pulito invece di lanciare
   * un'eccezione alla sua seconda soluzione (che non esiste avendo cuttato).
   *
   * Insomma avendo usato il cut andava modificata :) (CANCELLARE????)
   */
  def mkPrologEngine(clauses: String*): Term => LazyList[Term] =
    val engine = Prolog()
    engine.setTheory(Theory(clauses mkString " "))
    goal => new Iterable[Term]{
      override def iterator = new Iterator[Term] {
        var solution = engine.solve(goal)
        var allSolutionsExplored = false
        override def hasNext = !allSolutionsExplored && solution.isSuccess
        override def next(): Term =
          val term = solution.getSolution
          if solution.hasOpenAlternatives then solution = engine.solveNext
          else allSolutionsExplored = true
          term
      }
    }.to(LazyList)