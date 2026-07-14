package model

object PlayerModule:
  import utils.ModelExports.{Fiche, StandardCard, Participant}

  enum PlayerState:
    case Active
    case Busted //The player exceeded 21
    case Standing //The player has decided to stop asking for cards
    case Blackjack

  /** Represents the player wallet during the game. */
  trait Wallet:

    /** The list of fiches currently owned by the player. */
    def balance: List[Fiche]

    /** Deposit a specific amount in the current balance by converting it into fiches.
     *
     * @param amount The value to be deposited.
     */
    def deposit(amount: Double): Unit //Torna l'istanza del giocatore con balance aggiornato

    /** Withdraws the necessary fiches to cover the requested bet amount
     *
     * The method first attempts to withdraw exact value fiches starting from the largest.
     * If an exact combination is not found it attempts to use the smallest available
     * fiche that is greater than the remaining debt, returning the change to the balance.
     *
     * @param amount The total bet amount to be subtracted.
     * @return [[true]] if the player has enough fiches and the withdrawn succeeds, [[false]] otherwise,
     */
    def withdraw(amount: Double): Boolean //Torna l'istanza del giocatore con balance aggiornato

  trait InsuranceSupport:
    /**
     * Returns whether the player has chosen the insurance option.
     *
     * @return `true` if the player has insurance, `false` otherwise.
     */
    def hasInsurance: Boolean

    /**
     * Updates the player's insurance status.
     *
     * @param value `true` if the player chooses insurance, `false` otherwise.
     */
    def hasInsurance_=(value: Boolean): Unit

  /** Represents a player at the game table.
   * Manages the player's current balance and state in the game.
   */
  trait Player extends Participant with Wallet:

    /** The state of the player */
    def state: PlayerState

    /** Changes the player's state to `Standing`. */
    def stand(): Unit

    /** Changes the player's state to `BlackJack`. */
    def winBlackjack(): Unit

    /** Changes the player's state to `Busted`(when exceeding 21). */
    def bust(): Unit

    /** Resets the player's state to `Active` to start a new round. */
    def prepareForNewHand(): Unit

    /** Prints a player in a format: [NAME] CARDS - SCORE - STATE */
    override def toString: String = super.toString + s"\nSTATE: $state\n"

  //Classe astratta che implementa una sola volta tutti i metodi che sono comuni sia al Player che allo SplitPlayer
  //si sceglie di farla astratta così che non possa essere implementata direttamente
  abstract class PlayerBase(override val name: String,
                            val balanceToBeConverted: Double) extends Player:

    private var currentState = PlayerState.Active
    private var currentBalance = Fiche.fromAmount(balanceToBeConverted)

    override def state: PlayerState =
      currentState

    protected def active(): Unit =
      currentState = PlayerState.Active

    override def stand(): Unit =
      currentState = PlayerState.Standing

    override def winBlackjack(): Unit =
      currentState = PlayerState.Blackjack

    override def bust(): Unit =
      currentState = PlayerState.Busted

    override def prepareForNewHand(): Unit =
      currentState = PlayerState.Active
      clearHand() /*TODO capire se è possibile rendere solo il Player in grado di iniziare un nuovo round*/

    override def balance: List[Fiche] =
      currentBalance

    override def deposit(amount: Double): Unit =
      require(amount > 0)
      currentBalance = currentBalance ::: Fiche.fromAmount(amount)

    override def withdraw(amount: Double): Boolean =
      require(amount > 0, "withdraw amount must be greater than 0")
      val sortedFiches = currentBalance.sortBy(-_.value)
      val (keptFiches, remainingAmount) = sortedFiches.foldLeft((List.empty[Fiche], amount)):
        case ((remainedFiches, leftAmount), fiche) =>
          if leftAmount > 0 && fiche.value <= leftAmount then (remainedFiches, leftAmount - fiche.value)
          else (remainedFiches :+ fiche, leftAmount)
      if remainingAmount <= 0 then
        currentBalance = keptFiches
        true
      else
        keptFiches.sortBy(_.value).find(_.value >= remainingAmount) match
          case Some(changeFiche) =>
            val change = changeFiche.value - remainingAmount
            val updatedKept = keptFiches.diff(List(changeFiche))
            currentBalance = if change > 0 then updatedKept ::: Fiche.fromAmount(change) else updatedKept
            true
          case None              => false

  class NormalPlayer(override val name: String,
                     override val balanceToBeConverted: Double) extends PlayerBase(name, balanceToBeConverted) with InsuranceSupport:
    private var insurance = false
    override def hasInsurance: Boolean =
      insurance

    override def hasInsurance_=(value: Boolean): Unit =
      insurance = value


  class SplitPlayer(override val name: String,
                    val splitCard: StandardCard,
                    override val balanceToBeConverted: Double = 0) extends PlayerBase(name, balanceToBeConverted):
    addCard(splitCard) //Aggiunta (nel costruttore) alla sua mano della carta con cui si è fatto lo split

  class BotPlayer(override val name: String,
                  override val balanceToBeConverted: Double = BotPlayer.randomBalance,
                  val fixedBet: Int = BotPlayer.randomBet) extends PlayerBase(name, balanceToBeConverted)

  object BotPlayer:
    private val MinBalance = 100
    private val MaxBalance = 500
    private val BalanceStep = 100
    private val MinBet = 10
    private val MaxBet = 50
    private val BetStep = 10

    private def getRandomNumber(min: Int, max: Int, step: Int): Int =
      min + scala.util.Random.nextInt((max - min) / step + 1) * step

    private def randomBalance: Double = getRandomNumber(MinBalance, MaxBalance, BalanceStep)

    private def randomBet: Int = getRandomNumber(MinBet, MaxBet, BetStep)

