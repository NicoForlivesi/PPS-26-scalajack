package controller

import utils.GameUIExports.*

object Controller extends IOApp.Simple:
  import utils.ModelExports.*

  def run: IO[Unit] =
    for
      game <- initializeGame
      _    <- handleHands(game)
      _    <- endGame(game)
    yield ()

  def initializeGame(using console: Console[IO]): IO[Game] =
    for
      numPlayers <- getNumPlayers(Game.isPlayerNumValid)
      players    <- getPlayers(numPlayers)
      game       = Game(players)
    yield game

  def handleHands(game: Game)(using console: Console[IO]): IO[Unit] =
    handleHand(game).iterateUntil(_ => game.isOver)

  def endGame(game: Game)(using console: Console[IO]): IO[Unit] =
    renderMessage(GameOver) >>
      IO.whenA(game.players.nonEmpty):
        game.balances(game.players).traverse_(nameAndBalance => renderMessage(ShowFinalBalance(nameAndBalance._1, nameAndBalance._2)))

  def getPlayers(numPlayers: Int)(using console: Console[IO]): IO[List[Player]] =
    for
      playersNames <- getPlayersNames(Game.arePlayersNamesValid(numPlayers))
      players      <- playersNames.traverse(name =>
          getInitialDeposit(name, Game.isInitialDepositValid).map(balance => NormalPlayer(name, balance))
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

  def initializeHand(game: Game)(using console: Console[IO]): IO[Unit] =
    getBets(game) >>
      renderMessage(CardsDistribution) >>
      game.distributeCards().traverse_(card => processCardDrawing(game, card)) >>
      handleBlackjacksWinners(game) >>
      IO.whenA(game.dealerHasAce):
        getInsurancePlayers(game.isNameValid).flatMap(insuranceNames => IO(game.handleInsurances(insuranceNames)))

  def handleBlackjacksWinners(game: Game, splitBlackjackPlayers: List[Player] = List.empty)(using console: Console[IO]): IO[Unit] =
    val winners: List[Player] = if splitBlackjackPlayers.isEmpty then game.initialBlackjackPlayers() else splitBlackjackPlayers
    IO(game.handleBlackjacks(winners)) >>
      winners.traverse_(winner => renderMessage(ShowBlackJack(winner)))

  def handlePlayersTurn(game: Game)(using console: Console[IO]): IO[Unit] =
    game.players.traverse_(player => IO.unlessA(player.state == Blackjack)(handleSinglePlayerTurn(game, player)))

  def handlePlayerAction(game: Game, player: Player, action: PlayerAction)(using console: Console[IO]): IO[Boolean] = action match
    case PlayerAction.DrawCard   => drawAndProcess(game, player, game.drawCard, autoStand = true)
    case PlayerAction.DoubleDown => drawAndProcess(game, player, game.doubleDown, autoStand = false)
    case PlayerAction.Stand      => finalizePlayerTurn(game, player)
    case PlayerAction.Split      => processSplit(game, player)

  def handleDealerTurn(game: Game)(using console: Console[IO]): IO[Unit] =
    renderMessage(DealerTurn()) >>
      renderMessage(ShowCard(game.dealer.toString)) >>
      game.resolveInsurances().traverse_((name, win) => renderMessage(ShowInsuranceWin(name, win))) >>
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
    IO(game.removeSplitPlayers()) >>
      ejectBrokePlayers(game) >>
      handleLeavingPlayers(game) >>
      IO(game.handleHandEnd())

  private def handleHand(game: Game)(using console: Console[IO]): IO[Unit] =
    initializeHand(game) >>
      handlePlayersTurn(game) >>
      handleDealerTurn(game) >>
      handleHandWinners(game) >>
      endHand(game)

  private def handleSinglePlayerTurn(game: Game, player: Player)(using console: Console[IO]): IO[Unit] =
    def _handleSinglePlayerTurn(player: Player): IO[Unit] =
      getPlayerAction(player, game.canDoubleDown, game.canSplit).flatMap(action =>
        handlePlayerAction(game, player, action).flatMap:
          case true => _handleSinglePlayerTurn(player)
          case _    => IO.unit
      )

    renderMessage(PlayerTurn(player.name)) >>
      renderMessage(ShowCard(player.toString)) >> (
      player match
      //  case p: Bot => game.computeBotTurn(p).traverse_(card => processCardDrawing(game, card)) TODO scommentare quando si implementa computBotTurn
        case _      => _handleSinglePlayerTurn(player)
      )

  private def finalizePlayerTurn(game: Game, player: Player): IO[Boolean] =
    IO(player.stand()) >>
      IO(game.transferBalance(player)) >>
      IO.pure(false)

  private def processPostDrawState(game: Game, player: Player, autoStand: Boolean): IO[Boolean] =
    IO(game.evaluatePlayerBust(player)).flatMap:
      case true                                                                         => //gestione di un player busted, può essere sia per draw che per double down
        renderMessage(ShowBusted(player)) >>
          IO(game.transferBalance(player)) >>
          IO.pure(false)
      case _ if !autoStand || (autoStand && player.score.playableValue == WinningScore) => finalizePlayerTurn(game, player) // sia per double down, che per draw quando il player arriva a 21, deve andare in stand
      case _                                                                            => IO.pure(true) //per draw quando il player ha meno di 21, bisogna richiedere la azione successiva

  private def drawAndProcess(game: Game, player: Player, drawEffect: Player => Option[Card], autoStand: Boolean)(using console: Console[IO]): IO[Boolean] = drawEffect(player) match
    case Some(card) =>
      processCardDrawing(game, s"$card\n$player") >>
        processPostDrawState(game, player, autoStand)
    case _          => IO.pure(false)

  private def processSplit(game: Game, player: Player)(using console: Console[IO]): IO[Boolean] = game.splitPlayer(player) match
    case Some(cardPlayer, _) =>
      val splitPlayers = List(player, game.getNextPlayer(player).get)
      val blackjackPlayers = game.playersWithBlackjack(splitPlayers)
      renderMessage(ShowCard(s"$cardPlayer\n$player")) >>
        IO.whenA(blackjackPlayers.nonEmpty)(handleBlackjacksWinners(game, splitBlackjackPlayers = blackjackPlayers)) >>
        IO.pure(!blackjackPlayers.contains(player))
    case _                   => IO.pure(false)

  private def processCardDrawing(game: Game, cardMessage: String)(using console: Console[IO]): IO[Unit] =
    IO.unlessA(game.isCutCardInDeck)(renderMessage(ShowCutCard)) >>
      renderMessage(ShowCard(cardMessage))

  private def ejectPlayer(game: Game, isToEject: Player => Boolean)(using console: Console[IO]): IO[Unit] =
    game.players.filter(isToEject).traverse_(player =>
      renderMessage(RemovePlayer(player.name)) >> //use of >> to concatenate the two effects without using a nested for-yield
        IO(game.removePlayer(player))
    )

  private def ejectBrokePlayers(game: Game)(using console: Console[IO]): IO[Unit] =
    ejectPlayer(game, player => player.balance.totalValue <= 0)

  private def handleLeavingPlayers(game: Game)(using console: Console[IO]): IO[Unit] =
    IO.whenA(game.players.nonEmpty):
      for
        leavingNames   <- getLeavingPlayers(game.isNameValid)
        leavingPlayers = game.players.filter(p => leavingNames.contains(p.name))
        _              <- game.balances(leavingPlayers).traverse_(nameAndBalance => renderMessage(ShowFinalBalance(nameAndBalance._1, nameAndBalance._2)))
        _              <- ejectPlayer(game, player => leavingNames.contains(player.name))
      yield ()
