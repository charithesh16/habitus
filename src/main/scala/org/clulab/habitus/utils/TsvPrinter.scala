package org.clulab.habitus.utils
import org.clulab.odin.Mention
import org.clulab.processors.Document
import org.clulab.utils.FileUtils

import java.io.PrintWriter

class TsvPrinter(outputFilename: String) extends Printer {
  protected val printWriter: PrintWriter = FileUtils.printWriterFromFile(outputFilename)

  def close(): Unit = {
    printWriter.close()
  }

  def outputMentions(
                      mentions: Seq[Mention],
                      doc: Document,
                      inputFilename: String,
                      printVars:PrintVariables
                    ): Unit = {
    println(s"Writing mentions from doc ${inputFilename} to $outputFilename")
    outputMentions(mentions, doc, inputFilename, printWriter,printVars)
    printWriter.flush()
  }

  // extract needed information and write them to tsv in a desired format. Return nothing here!
  protected def outputMentions(mentions: Seq[Mention], doc: Document,
                               filename: String, pw: PrintWriter,printVars:PrintVariables): Unit = {

    mentions.foreach {
        // Format to print: variable \t value text \t value norms \t extracting sentence \t document name
        // \t Most frequent X within 0 sentences \t Most frequent X within 1 sentences.\t Most frequent X anywhere in the doc.\n
        // Since we only focus on the Assignment mention which includes two submentions in the same format called
        // ``variable`` and ``value`` we access the two through ``arguments`` attribute of the Mention class.
        m =>
           {
            val variable = m.arguments(printVars.mentionType).headOption
            val varText = if (variable.isDefined) variable.head.text else na
            val value = m.arguments(printVars.mentionExtractor).headOption
            val valText = if (value.isDefined) value.head.text else na
            val sentText = m.sentenceObj.getSentenceText
            val valNorms = if (value.isDefined) value.get.norms else None
            val norm = {
              if (valNorms.isDefined && valNorms.get.size >= 2) {
                valNorms.filter(_.length >= 2).get(0)
              } else {
                //
                // not all NEs have meaningful norms set
                //   For example, DATEs have norms, but CROPs do not
                // in the latter case, we revert to the lemmas or to the actual text as a backoff
                //
                if (value.get.words.nonEmpty) {
                  value.get.words.mkString(" ")
                } else {
                  value.get.text
                }
              }
            }
             // this is for cases when there is no norm
            val normString = if (norm.nonEmpty) norm + "\t" else ""
            val context = m.attachments.headOption
            val contextString = if (context.isDefined) context.get.asInstanceOf[Context].getTSVContextString else ""
            pw.println(s"$varText\t$valText\t$normString$sentText\t$filename\t$contextString")

          }
      }

  }
}
