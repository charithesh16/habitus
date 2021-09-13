package org.clulab.variables

import org.clulab.dynet.Utils
import org.clulab.odin.{ExtractorEngine, Mention}
import org.clulab.processors.{Document, Processor}
import org.clulab.processors.clu.CluProcessor
import org.clulab.sequences.LexiconNER


class Context(var location: String, var entity: String, var relativeDist: Int, var count: Int) {

}

class VariableProcessor(val processor: Processor, val extractor: ExtractorEngine) {
  def parse(text: String): (Document, Seq[Mention]) = {

    // pre-processing
    val doc = processor.annotate(text, keepText = false)

    // extract mentions from annotated document
    val mentions = extractor.extractFrom(doc).sortBy(m => (m.sentence, m.getClass.getSimpleName))
    val allContexts = extractContext(doc)
    (doc, mentions)
  }

  def extractContext(doc: Document): Unit = {
    for ((s, i) <- doc.sentences.zipWithIndex) {
      println(s"sentence #$i")
      println(s.getSentenceText)
      println("Entities: " + s.entities.get.mkString(", "))
      val bLocIndexes = s.entities.get.indices.filter(index => s.entities.get(index) == "B-LOC")
      println(s"value of blocindexes is $bLocIndexes")
      //val lengths = bLocIndexes.map { start => countLocs(start) }
      println("Tokens: " + (s.words(3)))
      for ((es,ix) <- s.entities.get.zipWithIndex) {
          if (es == "B-LOC") {
            println(s"found a location called $s.words(ix)")
          }
      }
      }
    }
}

  object VariableProcessor {
    def apply(): VariableProcessor = {
      // Custom NER for variable reading
      val kbs = Seq(
        "variables/FERTILIZER.tsv"
      )
      val lexiconNer = LexiconNER(kbs,
        Seq(
          true // case insensitive match for fertilizers
        )
      )

      // create the processor
      Utils.initializeDyNet()
      val processor: Processor = new CluProcessor(optionalNER = Some(lexiconNer))

      // read rules from yml file in resources
      val source = io.Source.fromURL(getClass.getResource("/variables/master.yml"))
      val rules = source.mkString
      source.close()

      // creates an extractor engine using the rules and the default actions
      val extractor = ExtractorEngine(rules)

      new VariableProcessor(processor, extractor)
    }
  }
