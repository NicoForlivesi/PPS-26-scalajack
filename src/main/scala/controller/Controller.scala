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

object Controller extends IOApp.Simple:

  def getPlayers(numPlayers: Int)(using console: Console[IO]): IO[List[Player]] =
    for
      playersNames <- getPlayersNames(numPlayers)
      players      <- playersNames.traverse(name =>
          getInitialDeposit(name, Game.isInitialDepositValid).map(balance => Player(name, balance))
      )
    yield players

  def getBets(game: Game)(using console: Console[IO]): IO[Unit] =
    game.players.traverse(player =>
      getBet(player, game.isBetValid(player)).map(bet => Bet(player, bet))
    ).flatMap(bets =>
      IO:
        bets.foreach(bet => bet.player.withdraw(bet.amount))
        game.currentBets = bets
    )

  def handleBlackjacksWinners(game: Game, splitBlackjackPlayers: List[Player] = List.empty)(using console: Console[IO]): IO[Unit] =
    val winners: List[Player] = if splitBlackjackPlayers.isEmpty then game.initialBlackjackPlayers() else splitBlackjackPlayers
    IO(game.handleBlackjacks(winners)) >>
      winners.traverse_(winner => renderMessage(ShowBlackJack(winner)))

  def initializeGame(using console: Console[IO]): IO[Game] =
    for
      numPlayers <- getNumPlayers
      players    <- getPlayers(numPlayers)
      game        = Game(players)
    yield game

  def initializeHand(game: Game)(using console: Console[IO]): IO[Unit] =
    getBets(game) >>
      renderMessage(CardsDistribution) >>
      game.distributeCards().traverse_(card => processCardDrawing(game, card)) >>
      handleBlackjacksWinners(game) >>
      IO.whenA(game.dealerHasAce):
       getInsurancePlayers(game.isNameValid).flatMap(insuranceNames => IO(game.handleInsurances(insuranceNames)))

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

    def processSplit(player: Player): IO[Boolean] = game.splitPlayer(player) match
      case Some(cardPlayer, _) =>
        val splitPlayers     = List(player, game.getNextPlayer(player).get)
        val blackjackPlayers = game.playersWithBlackjack(splitPlayers)
        renderMessage(ShowCard(s"$cardPlayer\n$player")) >>
          IO.whenA(blackjackPlayers.nonEmpty)(handleBlackjacksWinners(game, splitBlackjackPlayers = blackjackPlayers)) >>
          IO.pure(!blackjackPlayers.contains(player))
      case _                   => IO.pure(false)

    action match
      case PlayerAction.DrawCard => drawAndProcess(player, game.drawCard, autoStand = true)
      case DoubleDown            => drawAndProcess(player, game.doubleDown, autoStand = false)
      case PlayerAction.Stand    => finalizePlayerTurn(player)
      case PlayerAction.Split    => processSplit(player)

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

    game.players.traverse_(player => IO.unlessA(player.state == Blackjack)(startSinglePlayerTurn(player)))

  def handleDealerTurn(game: Game)(using console: Console[IO]): IO[Unit] =
    renderMessage(DealerTurn()) >>
      renderMessage(ShowCard(game.dealer.toString)) >>
     //TODO game.resolveInsurances().traverse_(nameAndWin(renderMessage(ShowInsuranceWin(nameAndWin._1, nameAndWin._2)))) >>
      game.computeDealerTurn().traverse_(card => processCardDrawing(game, card))

  def handleHandWinners(game: Game)(using console: Console[IO]): IO[Unit] =
    IO.whenA(game.evaluateDealerBust)(renderMessage(DealerBusted)) >>
      IO:
        game.handlePayout()
        game.handleHandEnd()
      >>
      renderMessage(HandOver) >>
      game.balances(game.players).traverse_(nameAndBalance => renderMessage(ShowBalance(nameAndBalance._1, nameAndBalance._2)))

  def endHand(game: Game)(using console: Console[IO]): IO[Unit] =
    def ejectPlayer(isToEject: Player => Boolean): IO[Unit] =
      game.players.filter(isToEject).traverse_(player =>
        renderMessage(RemovePlayer(player.name)) >> //use of >> to concatenate the two effects without using a nested for-yield
          IO(game.removePlayer(player))
      )

    def handleLeavingPlayers: IO[Unit] =
      IO.whenA(game.players.nonEmpty):
        for
            leavingNames   <- getLeavingPlayers(game.isNameValid)
            leavingPlayers = game.players.filter(p => leavingNames.contains(p.name))
            _              <- game.balances(leavingPlayers).traverse_(nameAndBalance => renderMessage(ShowFinalBalance(nameAndBalance._1, nameAndBalance._2)))
            _              <- ejectPlayer(player => leavingNames.contains(player.name))
        yield ()

    IO(game.removeSplitPlayers()) >>
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
    handleHand(game).iterateUntil(_ => game.isOver)

  def endGame(game: Game): IO[Unit] =
    renderMessage(GameOver) >>
      IO.whenA(game.players.nonEmpty):
        game.balances(game.players).traverse_(nameAndBalance => renderMessage(ShowFinalBalance(nameAndBalance._1, nameAndBalance._2)))

  def run: IO[Unit] =
    //TODO creare un object che contiene tutti gli oggetti da esportare e farne l'import
    for
      game <- initializeGame
      _    <- handleHands(game)
      _    <- endGame(game)
    yield ()

  private def processCardDrawing(game: Game, cardMessage: String)(using console: Console[IO]): IO[Unit] =
    IO.unlessA(game.isCutCardInDeck)(renderMessage(ShowCutCard)) >>
      renderMessage(ShowCard(cardMessage))
