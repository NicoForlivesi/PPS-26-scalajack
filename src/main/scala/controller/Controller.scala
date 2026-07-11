package controller

import cats.effect.std.Console
import cats.effect.{IO, IOApp}
import cats.implicits.*
import model.DeckModule.Card
import model.PlayerModule.Player
import view.View.*
import model.GameModule.*
import model.PlayerModule.PlayerState.Blackjack
import model.ScoreModule.WinningScore
import view.View.Command.*
import view.View.PlayerAction.DoubleDown

import scala.None

object Controller extends IOApp.Simple:

  def getPlayers(numPlayers: Int)(using console: Console[IO]): IO[List[Player]] =
    for
      playersNames <- getPlayersNames(numPlayers)
      players      <- playersNames.traverse(name =>
          getInitialDeposit(name, Game.isInitialDepositValid).map(balance => Player(name, balance))
      )
    yield players

  def getBets(game: Game)(using console: Console[IO]): IO[Unit] =
    for
      bets <- game.players.traverse(player => getBet(player, game.isBetValid(player)).map(bet => Bet(player, bet)))
      _    <- IO(bets.foreach(bet => bet.player.withdraw(bet.amount)))
      _    <- IO(game.currentBets = bets)
    yield ()

  def handleBlackjacksWinners(game: Game)(using console: Console[IO]): IO[Unit] =
    val winners: List[Player] = game.playersWithBlackjack()
    IO(game.handleBlackjacks(winners)) >>
      winners.traverse_(winner => renderMessage(ShowBlackJack(winner)))

  def initializeGame(using console: Console[IO]): IO[Game] =
    for
      numPlayers <- getNumPlayers
      players    <- getPlayers(numPlayers)
      game = Game(players)
    yield game

  def initializeHand(game: Game)(using console: Console[IO]): IO[Unit] =
    getBets(game) >>
      renderMessage(CardsDistribution) >>
        game.distributeCards().traverse_(card => processCardDrawing(game, card)) >>
        handleBlackjacksWinners(game)

  def handlePlayerAction(game: Game, player: Player, action: PlayerAction)(using console: Console[IO]): IO[Boolean] =
    def finalizePlayerTurn(player: Player): IO[Boolean] =
      IO(player.stand()) >>
        IO(game.transferBalance(player)) >>
        IO.pure(false)

    def processPostDrawState(player: Player, autoStand: Boolean): IO[Boolean] =
      IO(game.evaluatePlayerBust(player)).flatMap:
        case true                                                                         => //gestione di un player busted, può essere sia per draw che per double down
          renderMessage(ShowBusted(player)) >>
            IO(game.transferBalance(player)) >>
            IO.pure(false)
        case _ if !autoStand || (autoStand && player.score.playableValue == WinningScore) => finalizePlayerTurn(player) // sia per double down, che per draw quando il player arriva a 21, deve andare in stand
        case _                                                                            => IO.pure(true) //per draw quando il player ha meno di 21, bisogna richiedere la azione successiva

    def drawAndProcess(player: Player, drawEffect: Player => Option[Card], autoStand: Boolean): IO[Boolean] = drawEffect(player) match
        case Some(card) =>
          processCardDrawing(game, s"$card\n$player") >>
            processPostDrawState(player, autoStand)
        case _          => IO.pure(false)

    action match
      case PlayerAction.DrawCard => drawAndProcess(player, game.drawCard, autoStand = true)
      case DoubleDown            => drawAndProcess(player, game.doubleDown, autoStand = false)
      case PlayerAction.Stand    => finalizePlayerTurn(player)
      case PlayerAction.Split    => game.splitPlayer(player) match
        case Some(cardPlayer, _) => renderMessage(ShowCard(s"$cardPlayer\n$player")) >> IO.pure(true)
        case _                   => IO.pure(false)

  def handlePlayersTurn(game: Game)(using console: Console[IO]): IO[Unit] =
    def _handleSinglePlayerTurn(player: Player)(using console: Console[IO]): IO[Unit] =
      getPlayerAction(player, game.canDoubleDown, game.canSplit).flatMap: action =>
        handlePlayerAction(game, player, action).flatMap:
          case true => _handleSinglePlayerTurn(player)
          case _    => IO.unit

    def startSinglePlayerTurn(player: Player): IO[Unit] =
      renderMessage(PlayerTurn(player.name)) >>
        renderMessage(ShowCard(player.toString)) >>
        _handleSinglePlayerTurn(player)

    def loop(current: Player): IO[Unit] =
      val currentTurn =
        if current.state == Blackjack then IO.unit
        else startSinglePlayerTurn(current)
      currentTurn >>
        (if current == game.players.last then IO.unit
        else game.getFollowingPlayer(current) match
          case Some(next) => loop(next)
          case _          =>  IO.unit)

    game.players.headOption match
      case Some(first) => loop(first)
      case None        => IO.unit

  def handleDealerTurn(game: Game)(using console: Console[IO]): IO[Unit] =
    renderMessage(DealerTurn()) >>
      renderMessage(ShowCard(game.dealer.toString)) >>
      game.computeDealerTurn().traverse_(card => processCardDrawing(game, card))

  def handleHandWinners(game: Game)(using console: Console[IO]): IO[Unit] =
    val dealerBustedEffect = if game.evaluateDealerBust then renderMessage(DealerBusted) else IO.unit
    dealerBustedEffect >>
      IO(game.handlePayout()) >>
      IO(game.handleHandEnd()) >>
      renderMessage(HandOver) >>
      game.balances(game.players).traverse_(nameAndBalance => renderMessage(ShowBalance(nameAndBalance._1, nameAndBalance._2)))

  def endHand(game: Game)(using console: Console[IO]): IO[Unit] =
    def ejectPlayer(isToEject: Player => Boolean): IO[Unit] =
      game.players.filter(isToEject).traverse_(player =>
        renderMessage(RemovePlayer(player.name)) >> //use of >> to concatenate the two effects without using a nested for-yield
          IO(game.removePlayer(player))
      )

    def handleLeavingPlayers: IO[Unit] = game.players.match
      case players if players.nonEmpty =>
        for
            leavingNames   <- getLeavingPlayers(game.isNameValid)
            leavingPlayers = game.players.filter(p => leavingNames.contains(p.name))
            _              <- game.balances(leavingPlayers).traverse_(nameAndBalance => renderMessage(ShowFinalBalance(nameAndBalance._1, nameAndBalance._2)))
            _              <- ejectPlayer(player => leavingNames.contains(player.name))
        yield ()
      case _                          => IO.unit

    IO(game.removeSplittedPlayers()) >>
      ejectPlayer(_.balance.totalValue <= 0) >>
      handleLeavingPlayers >>
      IO(game.handleHandEnd())

  def handleHand(game: Game)(using console: Console[IO]): IO[Unit] =
    initializeHand(game) >>
      handlePlayersTurn(game) >>
      handleDealerTurn(game) >>
      handleHandWinners(game) >>
      endHand(game)

  def handleHands(game: Game)(using console: Console[IO]): IO[Unit] =
    handleHand(game).flatMap(_ => if game.isCutCardInDeck && game.players.nonEmpty then handleHands(game) else IO.unit)

  def endGame(game: Game): IO[Unit] =
    renderMessage(GameOver) >> (
      if game.players.nonEmpty then
        game.balances(game.players).traverse_(nameAndBalance => renderMessage(ShowFinalBalance(nameAndBalance._1, nameAndBalance._2)))
      else
        IO.unit
      )

  def run: IO[Unit] =
    //TODO creare un object che contiene tutti gli oggetti da esportare e farne l'import
    for
      game <- initializeGame
      _    <- handleHands(game)
      _    <- endGame(game)
    yield ()

  private def processCardDrawing(game: Game, cardMessage: String)(using console: Console[IO]): IO[Unit] =
    val cutCardCheckEffect = if !game.isCutCardInDeck then renderMessage(ShowCutCard) else IO.unit
    cutCardCheckEffect >> renderMessage(ShowCard(cardMessage))
