package model

object FicheModule:

  enum Fiche:
    case Five, Ten, Twenty, Fifty

  object Fiche:
    private val denominations: List[Fiche] = Fiche.values.toList.sortBy(-_.value) // == sortWith((a, b) => a.value > b.value),
    // insomma vuol dire ordinare in modo decrescente in modo molto coinciso (CANELLARE COMMENTO POI)
    val smallestDenomination: Int = denominations.map(_.value).min

    def fromAmount(amount: Int): List[Fiche] =
      require(amount >= smallestDenomination, s"amount must be at least $smallestDenomination")
      require(amount % smallestDenomination == 0, s"amount must be a multiple of $smallestDenomination")
      denominations.foldLeft((amount, List.empty[Fiche])):
        case ((remaining, acc), fiche) =>
          val count = remaining / fiche.value
          (remaining - count * fiche.value, acc ::: List.fill(count)(fiche))
      ._2

    extension (f: Fiche)
      def value: Int = f match
        case Five => 5
        case Ten => 10
        case Twenty => 20
        case Fifty => 50

    extension (fiches: List[Fiche])
      def totalValue: Int = fiches.map(_.value).sum