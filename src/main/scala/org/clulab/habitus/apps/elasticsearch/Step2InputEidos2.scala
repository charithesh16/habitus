package org.clulab.habitus.apps.elasticsearch

import ai.lum.common.FileUtils._
import org.clulab.habitus.apps.utils.{AttributeCounts, DateString, JsonRecord}
import org.clulab.habitus.elasticsearch.ElasticsearchIndexClient
import org.clulab.habitus.elasticsearch.data.{CausalRelation, CauseOrEffect, DatasetRecord, LatLon, Location}
import org.clulab.habitus.elasticsearch.utils.Elasticsearch
import org.clulab.odin.{EventMention, Mention}
import org.clulab.processors.{Document, Sentence}
import org.clulab.utils.{Sourcer, StringUtils}
import org.clulab.wm.eidos.attachments.{Decrease, Increase, NegChange, Negation, PosChange}
import org.clulab.wm.eidos.document.AnnotatedDocument
import org.clulab.wm.eidos.serialization.jsonld.{JLDDeserializer, JLDRelationCausation}
import org.clulab.wm.eidoscommon.utils.{FileEditor, FileUtils, Logging, TsvReader, TsvWriter}
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods

import java.io.File
import java.net.URL
import scala.util.{Try, Using}

object Step2InputEidos2 extends App with Logging {

  case class LocalTsvRecord(
    sentenceIndex: Int,
    sentence: String,
    belief: Boolean,
    sentimentScoreOpt: Option[Float],
    sentenceLocations: Array[Location],
    contextLocations: Array[Location],
    vector: Array[Float]
  )

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  val contextWindow = 3
  val baseDirectory = "../corpora/uganda-local"
  val inputFilename = "../corpora/uganda-local/uganda-2-vectors.tsv"
  val credentialsFilename = "../credentials/elasticsearch-credentials.properties"
  val deserializer = new JLDDeserializer()
  val url = new URL("http://localhost:9200")
  // val url = new URL("https://elasticsearch.keithalcock.com")
  val indexName = "habitus3"
  val datasetName = "uganda.tsv"
  val regionName = "uganda"
  val alreadyNormalized = true

  def jsonFileToJsonld(jsonFile: File): File =
      FileEditor(jsonFile).setExt("jsonld").get

  def jsonFileToRecord(jsonFile: File): JsonRecord = {
    val json = FileUtils.getTextFromFile(jsonFile)
    val jValue = JsonMethods.parse(json)
    val url = (jValue \ "url").extract[String]
    val titleOpt = (jValue \ "title").extractOpt[String]
    val datelineOpt = (jValue \ "dateline").extractOpt[String]
    val bylineOpt = (jValue \ "byline").extractOpt[String]
    val text = (jValue \ "text").extract[String]

    // Don't use them all in order to save space.
    JsonRecord(url, titleOpt, datelineOpt, bylineOpt, "")
  }

  def jsonFileToTerm(jsonFile: File): String = {
    val path = jsonFile.getPath
    val term = StringUtils.afterLast(
      StringUtils.beforeLast(
        StringUtils.beforeLast(
          StringUtils.beforeLast(
            path, '/'
          ), '/'
        ), '/'
      ), '/'
    )

    term
  }

  def jsonldFileToAnnotatedDocument(jsonldFile: File): AnnotatedDocument = {
    val json = FileUtils.getTextFromFile(jsonldFile)
    val corpus = deserializer.deserialize(json)
    val annotatedDocument = corpus.head

    annotatedDocument
  }

  def rawTextToCleanText(rawText: String): String = rawText
      .trim
      .replaceAll("\r\n", " ")
      .replaceAll("\n", " ")
      .replaceAll("\r", " ")
      .replaceAll("\t", " ")
      .replaceAll("\u2028", " ") // unicode line separator
      .replaceAll("\u2029", " ") // unicode paragraph separator
      .map { letter =>
        if (letter.toInt < 32) ' '
        else letter
      }
      .trim

  def getSentenceText(document: Document, sentence: Sentence): String = {
    val rawText = document.text.get.slice(sentence.startOffsets.head, sentence.endOffsets.last)
    val cleanText = rawTextToCleanText(rawText)

    cleanText
  }

  def attributeCountsToTsvWriter(attributeCounts: AttributeCounts, tsvWriter: TsvWriter): Unit = {
    tsvWriter.print(
      attributeCounts.increaseCount.toString, attributeCounts.decreaseCount.toString,
      attributeCounts.posChangeCount.toString, attributeCounts.negChangeCount.toString,
      ""
    )
  }

  def mentionToAttributeCounts(mention: Mention): AttributeCounts = {
    val increaseCount = mention.attachments.count(_.isInstanceOf[Increase])
    val decreaseCount = mention.attachments.count(_.isInstanceOf[Decrease])
    val posCount = mention.attachments.count(_.isInstanceOf[PosChange])
    val negCount = mention.attachments.count(_.isInstanceOf[NegChange])
    val negationCount = mention.attachments.count(_.isInstanceOf[Negation])

    AttributeCounts(increaseCount, decreaseCount, posCount, negCount, negationCount)
  }

  def newCauseOrEffect(text: String, attributeCounts: AttributeCounts): CauseOrEffect = {
    new CauseOrEffect(text,
      attributeCounts.increaseCount, attributeCounts.decreaseCount,
      attributeCounts.posChangeCount, attributeCounts.negChangeCount
    )
  }

  def parseLocations(locationString: String): Array[Location] = {
    if (locationString.isEmpty) Array.empty
    else {
      val locations = locationString.split(')').map { commaAndNameAndLatLon =>
        val trimmedCommaAndNameAndLatLon = commaAndNameAndLatLon.trim
        val toDrop = if (trimmedCommaAndNameAndLatLon.startsWith(",")) 1 else 0
        val nameAndLatLon = trimmedCommaAndNameAndLatLon.drop(toDrop)
        val Array(name, latLon) = nameAndLatLon.split('(').map(_.trim)
        val Array(latString, lonString) = latLon.split(",").map(_.trim)
        // Sometimes we have NaN for these.  We know it's a place, but not where.
        val latOpt = Try(latString.toFloat).toOption
        val lonOpt = Try(lonString.toFloat).toOption
        val latLonOpt = latOpt.flatMap { lat =>
          lonOpt.map { lon =>
            LatLon(lat, lon)
          }
        }

        Location(name,latLonOpt)
      }

      locations
    }
  }

  def parseVector(vectorString: String): Array[Float] = {
    val values = vectorString.split(", ")
    val floats = values.map(_.toFloat)

    floats
  }

  def normalize(floats: Array[Float]): Array[Float] = {
    if (alreadyNormalized) floats
    else {
      val sumSquare = floats.foldLeft(0f) { case (sum, float) => sum + float * float }
      val divisor = math.sqrt(sumSquare)
      val normalized = floats.map { float => (float / divisor).toFloat }

      normalized
    }
  }

  def getCausalRelations(causalMentionGroup: Seq[Mention]): Array[CausalRelation] = {
    val causalRelations = causalMentionGroup.zipWithIndex.map { case (causalMention, causalIndex) =>
      val causalAttributeCounts = mentionToAttributeCounts(causalMention)
      assert(causalAttributeCounts.increaseCount == 0)
      assert(causalAttributeCounts.decreaseCount == 0)
      assert(causalAttributeCounts.posChangeCount == 0)
      assert(causalAttributeCounts.negChangeCount == 0)

      val causeMentions = causalMention.arguments("cause")
      assert(causeMentions.length == 1)
      val effectMentions = causalMention.arguments("effect")
      assert(effectMentions.length == 1)

      val causeMention = causeMentions.head
      val effectMention = effectMentions.head

      val causeText = causeMention.text
      val cleanCauseText = rawTextToCleanText(causeText)
      val effectText = effectMention.text
      val cleanEffectText = rawTextToCleanText(effectText)

      val causeAttributeCounts = mentionToAttributeCounts(causeMention)
      assert(causeAttributeCounts.negatedCount == 0)
      val effectAttributeCounts = mentionToAttributeCounts(effectMention)
      assert(effectAttributeCounts.negatedCount == 0)

      val cause = newCauseOrEffect(cleanCauseText, causeAttributeCounts)
      val effect = newCauseOrEffect(cleanEffectText, effectAttributeCounts)
      val causalRelation = CausalRelation(
        causalIndex,
        causalAttributeCounts.negatedCount,
        cause,
        effect
      )

      causalRelation
    }

    causalRelations.toArray
  }

  def getLocationsAndDistance(sentenceIndex: Int, range: Range, urlSentenceIndexToTsvRecordMap: Map[(String, Int),
      LocalTsvRecord], url: String): (Array[Location], Option[Int]) = {
    val tsvRecord = urlSentenceIndexToTsvRecordMap(url, sentenceIndex)

    if (tsvRecord.sentenceLocations.nonEmpty)
      (tsvRecord.sentenceLocations, Some(0))
    else {
      val locationsIndexOpt = range.find { sentenceIndex =>
        urlSentenceIndexToTsvRecordMap(url, sentenceIndex).sentenceLocations.nonEmpty
      }
      val locations = locationsIndexOpt.map { sentenceIndex =>
        urlSentenceIndexToTsvRecordMap(url, sentenceIndex).sentenceLocations
      }.getOrElse(Array.empty)
      val distanceOpt = locationsIndexOpt.map { index => math.abs(sentenceIndex - index) }

      (locations, distanceOpt)
    }
  }

  val jsonFiles: Seq[File] = {
    val allJsonFiles = new File(baseDirectory).listFilesByWildcard("*.json", recursive = true)
        // TODO remove take
        // .take(1000)
    val jsonldFiles = new File(baseDirectory).listFilesByWildcard("*.jsonld", recursive = true).toSet

    allJsonFiles.filter { jsonFile => jsonldFiles(jsonFileToJsonld(jsonFile)) }
  }.toSeq.sorted
  val jsonFileRecordPairs: Seq[(File, JsonRecord)] = jsonFiles.map { jsonFile =>
    (jsonFile, jsonFileToRecord(jsonFile))
  }
  val jsonFileRecordPairGroups: Seq[(String, Seq[(File, JsonRecord)])] = jsonFileRecordPairs.groupBy { jsonFileRecordPair => jsonFileRecordPair._2.url}.toSeq
  val jsonFileRecordTermsSeq: Seq[(File, JsonRecord, Seq[String])] = jsonFileRecordPairGroups.map { case (_, jsonFileRecordPairs) =>
    // Use just one jsonFile and jsonRecord, but combine search terms
    val terms = jsonFileRecordPairs.map { jsonFileRecordPair => jsonFileToTerm(jsonFileRecordPair._1) }

    (jsonFileRecordPairs.head._1, jsonFileRecordPairs.head._2, terms)
  }
  val urlSentenceIndexToTsvRecordMap: Map[(String, Int), LocalTsvRecord] = Using.resource(Sourcer.sourceFromFilename(inputFilename)) { source =>
    val lines = source.getLines.drop(1)
    val tsvReader = new TsvReader()

    lines.map { line =>
      val Array(url, sentenceIndexString, sentence, beliefString, sentimentScore, sentenceLocationsString, contextLocationsString, vectorString) = tsvReader.readln(line, 8)
      val sentenceIndex = sentenceIndexString.toInt
      val belief = beliefString == "True"
      val sentimentScoreOpt = if (sentimentScore.isEmpty) None else Some(sentimentScore.toFloat)
      val sentenceLocations = parseLocations(sentenceLocationsString)
      val contextLocations = parseLocations(contextLocationsString)
      val vector = normalize(parseVector(vectorString))

      (url, sentenceIndex) -> new LocalTsvRecord(sentenceIndex, sentence, belief, sentimentScoreOpt, sentenceLocations, contextLocations, vector)
    }.toMap
  }
  val restClient = Elasticsearch.mkRestClient(url, credentialsFilename)

  Using.resource(restClient) { restClient =>
    val elasticsearchIndexClient = ElasticsearchIndexClient(restClient, indexName)

    jsonFileRecordTermsSeq.zipWithIndex.foreach { case ((jsonFile, jsonRecord, terms), index) =>
      println(s"$index ${jsonFile.getPath}")
      try {
        val url = jsonRecord.url
        val jsonldFile = jsonFileToJsonld(jsonFile)
        val annotatedDocument = jsonldFileToAnnotatedDocument(jsonldFile)
        val document = annotatedDocument.document
        val allMentions = annotatedDocument.allOdinMentions.toVector
        val causalMentions = allMentions.filter { mention =>
          mention.isInstanceOf[EventMention] && mention.label == JLDRelationCausation.taxonomy
        }
        val causalMentionGroups: Map[Int, Seq[Mention]] = causalMentions.groupBy(_.sentence)
        val sentences = document.sentences
        val dateOpt = jsonRecord.datelineOpt.map(DateString(_).canonicalize)

        sentences.zipWithIndex.foreach { case (sentence, sentenceIndex) =>
          val causal = causalMentionGroups.contains(sentenceIndex)
          val cleanText = getSentenceText(document, sentence)
          val tsvRecord = urlSentenceIndexToTsvRecordMap(url, sentenceIndex)
          val contextBefore = sentences
              .slice(sentenceIndex - contextWindow, sentenceIndex)
              .map(getSentenceText(document, _))
              .mkString(" ")
          val contextAfter = sentences
              .slice(sentenceIndex + 1, sentenceIndex + contextWindow + 1)
              .map(getSentenceText(document, _))
              .mkString(" ")
          val (prevLocations, prevDistanceOpt) = getLocationsAndDistance(sentenceIndex,
              Range(0, sentenceIndex).reverse, urlSentenceIndexToTsvRecordMap, url)
          val (nextLocations, nextDistanceOpt) = getLocationsAndDistance(sentenceIndex,
              Range(sentenceIndex + 1, document.sentences.length), urlSentenceIndexToTsvRecordMap, url)
          val causalRelations =
              if (causal) getCausalRelations(causalMentionGroups(sentenceIndex).sorted)
              else Array.empty[CausalRelation]
          val datasetRecord: DatasetRecord = DatasetRecord(
            datasetName,
            regionName,
            url,
            jsonRecord.titleOpt,
            terms.toArray,
            jsonRecord.datelineOpt,
            dateOpt,
            jsonRecord.bylineOpt,
            tsvRecord.sentenceIndex,
            cleanText,
            causalRelations,
            tsvRecord.belief,
            tsvRecord.sentimentScoreOpt,
            tsvRecord.sentenceLocations,
            contextBefore,
            contextAfter,
            tsvRecord.contextLocations,
            prevLocations,
            prevDistanceOpt,
            nextLocations,
            nextDistanceOpt,
            tsvRecord.vector
          )

          elasticsearchIndexClient.index(datasetRecord)
        }
      }
      catch {
        case throwable: Throwable =>
          logger.error(s"Exception for file $jsonFile", throwable)
      }
    }
  }
}
