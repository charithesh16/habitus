package org.clulab.habitus.apps.grid

import org.clulab.utils.{FileUtils, Sourcer}

import scala.util.Using

object Csv2Tsv extends App {
  val csvFilename = args.lift(0).getOrElse("../corpora/grid/uq500-karamoja/csvcheck.csv")
  val tsvFilename = args.lift(1).getOrElse("../corpora/grid/uq500-karamoja/csvcheck.tsv")

  trait State
  object OutsideFieldState extends State
  object InsideFieldState extends State
  object InsideQuotedFieldState extends State
  object InsideQuotedQuoteState extends State

  Using.resource(Sourcer.sourceFromFilename(csvFilename)) { source =>
    val lines = source.getLines

    Using.resource(FileUtils.printWriterFromFile(tsvFilename)) { printWriter =>

      def printString(string: String): Unit = printWriter.print(string)

      def printChar(char: Char): Unit = printWriter.print(char)

      def printLine(): Unit = printWriter.println

      def throwChar(char: Char, state: State): Nothing = {
        throw new RuntimeException(s"Char '$char' is invalid in state $state.")
      }

      def throwString(string: String): Nothing = {
        throw new RuntimeException(string)
      }

      val nextState = lines.foldLeft(OutsideFieldState: State) { (state, line) =>
        println(line)
        val nextState = line.foldLeft(state: State) { (state, char) =>
          println(char)
          val nextState = state match {
            case OutsideFieldState =>
              // I should only see a quote or some char that starts the field.
              val nextState = char match {
                case '\n' => throwChar(char, state)
                case '\r' => throwChar(char, state)
                case '\t' => throwChar(char, state)
                case ','  => throwChar(char, state)
                case '"'  => InsideQuotedFieldState
                case '\\' => throwChar(char, state)
                case _    => printChar(char); InsideFieldState
              }
              nextState
            case InsideFieldState =>
              // I need to escape special characters and watch for the looming comma.
              val nextState = char match {
                case '\n' => printString("\\n"); InsideFieldState
                case '\r' => printString("\\r"); InsideFieldState
                case '\t' => printString("\\t"); InsideFieldState
                case ','  => printChar('\t'); OutsideFieldState
                case '"'  => throwChar(char, state)
                case '\\' => printString("\\\\"); InsideFieldState
                case _    => printChar(char); InsideFieldState
              }
              nextState
            case InsideQuotedFieldState =>
              // I need to escape special characters and watch for the looming end quote or doubled false alarms.
              val nextState = char match {
                case '\n' => printString("\\n"); InsideQuotedFieldState
                case '\r' => printString("\\r"); InsideQuotedFieldState
                case '\t' => printString("\\t"); InsideQuotedFieldState
                case ','  => printChar(char); InsideQuotedFieldState
                case '"'  => InsideQuotedQuoteState
                case '\\' => printString("\\\\"); InsideQuotedFieldState
                case _    => printChar(char); InsideQuotedFieldState
              }
              nextState
            case InsideQuotedQuoteState =>
              // I just saw a quote while InsideQuotedFieldState and need to decide what to do.
              val nextState = char match {
                case '\n' => throwChar(char, state)
                case '\r' => throwChar(char, state)
                case '\t' => throwChar(char, state)
                case ','  => printChar('\t'); OutsideFieldState // We are now outside the field.
                case '"'  => printChar('"'); InsideQuotedFieldState // It was a double quote and we're still inside the field.
                case '\\' => throwChar(char, state)
                case _    => throwChar(char, state)
              }
              nextState
          }
          nextState
        }
        // The line is finished.
        nextState match {
          case OutsideFieldState => printLine(); OutsideFieldState
          case InsideFieldState => printLine(); OutsideFieldState
          case InsideQuotedFieldState => printString("\\n"); InsideQuotedFieldState
          case InsideQuotedQuoteState => printLine(); OutsideFieldState
        }
        nextState
      }
      // The file is finished.
      nextState match {
        case OutsideFieldState => assert(true)
        case InsideFieldState => assert(false)
        case InsideQuotedFieldState => throwString("Quoted field was not terminated correctly.")
        case InsideQuotedQuoteState => assert(false)
      }
    }
  }
}
