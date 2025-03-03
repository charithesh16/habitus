package org.clulab.habitus.utils

import java.io.{Closeable, PrintWriter}

// Be careful because this is copied in org.clulab.habitus.scraper.utils.
class EscapePair(unescapedChar: Char, val escapedChar: Char) {
  val unescapedString: String = unescapedChar.toString
  val escapedString: String = EscapePair.escapementString + escapedChar

  def escape(string: String): String = string.replace(unescapedString, escapedString)

  def unescape(string: String): String = {
    val prev = string.replace(escapedString, unescapedString)
    val next =
      if (unescapedChar == EscapePair.escapementChar) {
        val stringBuffer = new StringBuffer(string.length)
        val isEscaped = string.foldLeft(false) {
          case (true, char) if char == escapedChar =>
            // Unescape the character.
            stringBuffer.append(unescapedChar)
            false
          case (true, char @ _) =>
            throw new RuntimeException("""Unexpected escaping of `$char` in "$string".""")
          case (false, char) if char == EscapePair.escapementChar =>
            true
          case (false, char @ _) =>
            stringBuffer.append(char)
            false
        }
        if (isEscaped)
          throw new RuntimeException("""Unexpected escapement at end of "$string".""")
        stringBuffer.toString
      }
      else if (!string.contains(escapedString)) string // KISS
      else {
        // Change escaped to unescaped, but only if is there is an odd number of escapements before it.
        val stringBuffer = new StringBuffer(string.length)
        val isEscaped = string.foldLeft(false) {
          case (true, char) if char == escapedChar =>
            // Unescape the character.
            stringBuffer.append(unescapedChar)
            false
          case (true, char @ _) =>
            // It must have been some other escaped character.
            stringBuffer.append(EscapePair.escapementChar)
            stringBuffer.append(char)
            false
          case (false, char) if char == EscapePair.escapementChar =>
            true
          case (false, char @ _) =>
            stringBuffer.append(char)
            false
        }
        if (isEscaped)
          stringBuffer.append(EscapePair.escapementChar)
        stringBuffer.toString
      }

    if (prev != next)
      println("Improvement!")
    next
  }
}

object EscapePair {
  val escapementChar = '\\'
  val escapementString = escapementChar.toString

  def apply(char: Char, escaped: Char) = new EscapePair(char, escaped)
}

// See https://en.wikipedia.org/wiki/Tab-separated_values.
// This does not attempt to double internal quotes or quote an entire field that contains a quote, etc.
object XsvUtils {
  var nlChar = '\n'
  var crChar = '\r'
  val tabChar = '\t'
  val commaChar = ','
  val quoteChar = '"'
  val backslashChar = '\\'

  val escapePairs = Seq(
    EscapePair(XsvUtils.backslashChar, '\\'), // This must come first.
    EscapePair(XsvUtils.nlChar, 'n'),
    EscapePair(XsvUtils.crChar, 'r'),
    EscapePair(XsvUtils.tabChar, 't')
  )
}

class XsvReader(protected val separatorChar: Char) {
}

class TsvReader() extends XsvReader(XsvUtils.tabChar) {

  def unescape(string: String): String = {
    XsvUtils.escapePairs.reverse.foldLeft(string) { (string, escapePair) => escapePair.unescape(string) }
  }

  def passthru(string: String): String = string

  def readln(line: String, length: Int = -1, escaped: Boolean = true): Array[String] = {
    // Java will truncate unused columns from the back.  Therefore, add an extra,
    // used column at the end, but then remove the extra value that results.
    // The alternative is to split on a regular expression and include -1 as the
    // final argument, but the programmer is too obstinate for that approach.
    val count = line.count(_ == separatorChar) + 1
    val values = (line + separatorChar + ' ')
      .split(separatorChar)
      .take(count)
      .map(if (escaped) unescape else passthru)

    if (length >= 0) {
      if (length < values.length)
        values.take(length) // Truncate it.
      else if (length == values.length)
        values // Return it.
      else // Expand it.
        values.padTo(length, "")
    }
    else
      values
  }
}

object TsvReader {
}

class CsvReader() extends XsvReader(XsvUtils.commaChar) {
  // TODO It is more complicated because of the multiple lines per string
}

abstract class XsvWriter(val printWriter: PrintWriter, separatorChar: Char) extends Closeable {
  protected val separatorString: String = separatorChar.toString

  def quote(text: String): String = "\"" + text.replace("\"", "\"\"") + "\""

  def mkString(values: Seq[AnyRef]): String

  // Because of type erasure, toString will need to be called on strings, unfortunately.
  def mkString(string: String, strings: String*): String = mkString(string +: strings)

  def print(values: Seq[AnyRef]): XsvWriter = {
    printWriter.print(mkString(values))
    this
  }

  def print(string: String, strings: String*): XsvWriter = print(string +: strings)

  // If there is more than one argument, assume they are all strings
  def println(string: String, strings: String*): XsvWriter = println(string +: strings)

  def println(values: Seq[AnyRef]): XsvWriter = {
    print(values)
    println()
  }

  def println(): XsvWriter = {
    printWriter.print("\n") // Force Unix line endings.
    this
  }

  def close(): Unit = printWriter.close()
}

class TsvWriter(printWriter: PrintWriter, isExcel: Boolean = true) extends XsvWriter(printWriter, XsvUtils.tabChar) {

  def escape(string: String): String = {
    XsvUtils.escapePairs.foldLeft(string) { (string, escapePair) => escapePair.escape(string) }
  }

  def stringlnPlain(values: Seq[AnyRef]): String = {
    val escapedStrings = values
      .map(_.toString)
      .map(escape)

    escapedStrings.mkString(separatorString)
  }

  def stringlnExcel(values: Seq[AnyRef]): String = {
    val quotedStrings = values
      .map(_.toString)
      .map { string =>
        val mustBeQuoted = TsvWriter.quotableStrings.exists { quotableString: String =>
          string.contains(quotableString)
        } || string.contains(XsvUtils.commaChar)

        if (mustBeQuoted) quote(string)
        else string
      }

    quotedStrings.mkString(separatorString)
  }

  def mkString(values: Seq[AnyRef]): String =
    if (isExcel) stringlnExcel(values)
    else stringlnPlain(values)
}

object TsvWriter {
  val quotableStrings = Seq(
    XsvUtils.nlChar.toString,
    XsvUtils.crChar.toString,
    XsvUtils.tabChar.toString,
    XsvUtils.quoteChar.toString
  )
}

class CsvWriter(printWriter: PrintWriter, isExcel: Boolean = true) extends XsvWriter(printWriter, XsvUtils.commaChar) {
  // TODO: Excel does not seem to be able to handle tabs.

  def mkString(values: Seq[AnyRef]): String = {
    val quotedStrings = values
      .map(_.toString)
      .map { string =>
        val mustBeQuoted = CsvWriter.quotableStrings.exists { separator: String =>
          string.contains(separator)
        }

        if (mustBeQuoted) quote(string)
        else string
      }

    quotedStrings.mkString(separatorString)
  }
}

object CsvWriter {
  val quotableStrings = Seq(
    XsvUtils.nlChar.toString,
    XsvUtils.crChar.toString,
    XsvUtils.commaChar.toString,
    XsvUtils.quoteChar.toString
  )
}
