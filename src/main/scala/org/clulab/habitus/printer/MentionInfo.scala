package org.clulab.habitus.printer

import com.typesafe.config.ConfigFactory
import ai.lum.common.ConfigUtils._
import org.clulab.habitus.utils.Pairable
import org.clulab.odin.Mention
import org.clulab.struct.Interval

case class MentionInfo(contextWindow: String, sentenceText: String, inputFilename: String, label: String) extends Pairable

object MentionInfo {

  val config = ConfigFactory.load()
  val windowSize: Int = config[Int]("VarDatesReader.contextWindowSize")

  def apply(mention: Mention, inputFilename: String): MentionInfo = {
    val sentences = mention.document.sentences
    val mentionRange = Interval(mention.sentence).indices
    val sentRange = Range(mentionRange.start - windowSize, mentionRange.end + windowSize).intersect(sentences.indices)
    val contextWindow = sentRange.map(sentences(_).getSentenceText).mkString(" ")
    val sentenceText = sentences(mention.sentence).getSentenceText
    val label = mention.label
    new MentionInfo(contextWindow, sentenceText, inputFilename, label)
  }
}


