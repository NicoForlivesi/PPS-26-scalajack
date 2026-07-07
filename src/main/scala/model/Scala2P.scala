package model

import alice.tuprolog.*

object Scala2P:
  given Conversion[String, Term] = Term.createTerm(_)
  given Conversion[Seq[_], Term] = _.mkString("[", ",", "]")

  def extractTerm(t: Term, i: Integer): Term =
    t.asInstanceOf[Struct].getArg(i).getTerm

  def mkPrologEngine(clauses: String*): Term => LazyList[Term] =
    val engine = Prolog()
    engine.setTheory(Theory(clauses mkString " "))
    goal => new Iterable[Term]{
      override def iterator = new Iterator[Term] {
        var solution = engine.solve(goal)
        override def hasNext =
          solution.isSuccess || solution.hasOpenAlternatives
        override def next() =
          try { solution.getSolution } finally { solution = engine.solveNext }
      }
    }.to(LazyList)