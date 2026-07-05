package view

import cats.effect.IO
import cats.effect.std.Console

object View:

  /** Interactively prompts the user to enter their initial playing balance.
   *
   * This method prints a prompt to the terminal and reads the user's input.
   * If the input is not a valid positive number, it prints an error message
   * and recursively prompts the user again until a valid balance is provided.
   *
   * It wraps the terminal I/O side effects inside a [[cats.effect.IO]] container
   * to maintain referential transparency.
   *
   * @return an [[cats.effect.IO]] encapsulating the computation
   *         that yields the initial balance as a [[Int]].
   */
  def getInitialBalance(using console: Console[IO]): IO[Int] =
    for
      _       <- console.println("Welcome to the game!")
      _       <- console.println("Please insert your initial balance: ")
      input   <- console.readLine
      balance <- input.toIntOption match
        case Some(amount) if amount > 0 => IO.pure(amount)
        case _ => console.println("The input is not valid - please insert a positive integer: ")
                  getInitialBalance
      _       <- console.println("Your balance has been updated!")
    yield balance
