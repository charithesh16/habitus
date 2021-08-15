package org.clulab

import org.clulab.odin._
import org.clulab.processors.{Sentence, Document}
import java.io._

package object utils {

  def displayMentions(mentions: Seq[Mention], doc: Document): Unit = {
    val mentionsBySentence = mentions groupBy (_.sentence) mapValues (_.sortBy(_.start)) withDefaultValue Nil
    println
    for ((s, i) <- doc.sentences.zipWithIndex) {
      println(s"sentence #$i")
      println(s.getSentenceText)
      println("Tokens: " + (s.words.indices, s.words, s.tags.get).zipped.mkString(", "))
      println("Entities: " + s.entities.get.mkString(", "))
      println("Norms: " + s.norms.get.mkString(", "))
      printSyntacticDependencies(s)
      println

      val sortedMentions = mentionsBySentence(i).sortBy(_.label)
      val (events, entities) = sortedMentions.partition(_ matches "Event")
      val (tbs, rels) = entities.partition(_.isInstanceOf[TextBoundMention])
      val sortedEntities = tbs ++ rels.sortBy(_.label)
      println("entities:")
      sortedEntities foreach displayMention

      println
      println("events:")
      events foreach displayMention
      println("=" * 50)
    }
  }


  def outputMentionsToTSV(mentions: Seq[Mention], doc: Document, filename: String): Seq[String] = {
    val mentionsBySentence = mentions groupBy (_.sentence) mapValues (_.sortBy(_.start)) withDefaultValue Nil
    var seqMention = Seq[String]()
    for ((s, i) <- doc.sentences.zipWithIndex) {

      val sortedMentions = mentionsBySentence(i).sortBy(_.label)
      val (events, entities) = sortedMentions.partition(_ matches "Event")
      val (tbs, rels) = entities.partition(_.isInstanceOf[TextBoundMention])
      val sortedEntities = tbs ++ rels.sortBy(_.label)
      // start outputing mentions into tsv files in the given output folder
      sortedEntities.foreach(
        entity => {
          seqMention = seqMention :+ s"${convertMentionToString(entity)} ${s.getSentenceText} \t $filename"
        }
      )
    }
    return seqMention
  }


  def writeFile(filename: String, lines: Seq[String]): Unit = {
    val file = new File(filename)
    println(s"Wriing mentions to $file")
    val bw = new BufferedWriter(new FileWriter(file))
    for (line <- lines) {
      bw.write(line+"\n")
    }
    bw.close()
  }


  def convertMentionToString(mention: Mention): String = {
    val boundary = "\t"
    var mentionStr = ""
    mentionStr += s"${mention.text}" + boundary // add the variable of a mention
    mention match {
      case tb: TextBoundMention =>
        tb.norms.head.foreach { x =>
          mentionStr += s"$x"
        }
      case em: EventMention =>
        mentionStr += s"${em.trigger.text}"
      case _ => ()
    }
    mentionStr += s"$boundary"
    return mentionStr
  }


  def printSyntacticDependencies(s:Sentence): Unit = {
    if(s.dependencies.isDefined) {
      println(s.dependencies.get.toString)
    }
  }


  def displayMention(mention: Mention) {
    val boundary = s"\t${"-" * 30}"
    println(s"${mention.labels} => ${mention.text}")
    println(boundary)
    println(s"\tRule => ${mention.foundBy}")
    val mentionType = mention.getClass.toString.split("""\.""").last
    println(s"\tType => $mentionType")
    println(boundary)
    mention match {
      case tb: TextBoundMention =>
        println(s"\t${tb.labels.mkString(", ")} => ${tb.text}")
        tb.norms.head.foreach {x =>
          println(s"\tNorm => $x")
        }
      case em: EventMention =>
        println(s"\ttrigger => ${em.trigger.text}")
        displayArguments(em)
      case rel: RelationMention =>
        displayArguments(rel)
      case _ => ()
    }
    println(s"$boundary\n")
  }


  def displayArguments(b: Mention): Unit = {
    b.arguments foreach {
      case (argName, ms) =>
        ms foreach { v =>
          println(s"\t$argName ${v.labels.mkString("(", ", ", ")")} => ${v.text}")
        }
    }
  }
}
