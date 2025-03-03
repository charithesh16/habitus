package org.clulab.habitus.scraper.apps

import org.clulab.utils.{FileUtils, Sourcer}

import scala.util.Using

object FilterArticleCorpusApp extends App {
  val term = "sitemap"
  val inFileName = args.lift(0).getOrElse(s"./scraper/corpora/ghana/$term/articlecorpus.txt")
  val outFileName = args.lift(1).getOrElse(s"./scraper/corpora/ghana/$term/articlecorpus-filtered.txt")
  val filter = happyGhanaFilter _

  def adomOnlineFilter(line: String): Boolean = { true &&
    line.endsWith("/") && // Articles look like a directory.
    !line.startsWith("https://www.adomonline.com/tag/") && // We're not interested in tags.
    !line.startsWith("https://www.adomonline.com/author/") && // We're not interested in authors.
    !line.startsWith("https://www.adomonline.com/category/") && // Ditto with categories.
    (line.count(_ == '/') == 4) && // We need no subdirectory at all.  TODO: this works for many
    (line != "https://www.adomonline.com/") && // Skip the home page.
    // These files can actually be returned in a search, so keep them.
    // !line.startsWith("https://www.adomonline.com/__trashed") && // These appear to have been deleted.
    !line.endsWith("-fm/") &&
    !line.endsWith("-fm-live/") &&
    !line.endsWith("-live-radio/") &&
    !line.endsWith("-tv-live/") &&
    line.contains('-') &&
    !Seq(
      "https://www.adomonline.com/20200210121612-nyinsenneawuo/",
      "https://www.adomonline.com/20200407182050-kenkanme/",
      "https://www.adomonline.com/20191001210623-wiasemunsem-200x200/",
      "https://www.adomonline.com/adomonline-com-newsletter-survey/",
      "https://www.adomonline.com/joy-learning/",
      "https://www.adomonline.com/privacy-policy/",
      "https://www.adomonline.com/adom-podcasts/",
      "https://www.adomonline.com/adom-tv-live/",
      "https://www.adomonline.com/election-headquarters/",
      "https://www.adomonline.com/contact-us/"
    ).contains(line) &&
    true
  }

  def theChronicleFilter(line: String): Boolean = { true &&
    line.endsWith("/") && // Articles look like a directory.
    line.startsWith("https://thechronicle.com.gh/") &&
    !line.startsWith("https://thechronicle.com.gh/tag/") && // We're not interested in tags.
    !line.startsWith("https://thechronicle.com.gh/category/") && // We're not interested in playlists.
    !line.startsWith("https://thechronicle.com.gh/author/") && // We're not interested in playlists.
    !line.startsWith("https://thechronicle.com.gh/checkout/") && // We're not interested in playlists.
    !Seq(
      "https://thechronicle.com.gh/",
      "https://thechronicle.com.gh/coming-soon/",
      "https://thechronicle.com.gh/contact-us/",
      "https://thechronicle.com.gh/shop/",
      "https://thechronicle.com.gh/basket/",
      "https://thechronicle.com.gh/checkout-2/",
      "https://thechronicle.com.gh/my-account/"
    ).contains(line) &&
    true
  }

  def threeNewsFilter(line: String): Boolean = { true &&
    line.endsWith("/") && // Articles look like a directory.
    line.startsWith("https://3news.com/") &&
    !line.startsWith("https://3news.com/tag/") && // We're not interested in tags.
    !line.startsWith("https://3news.com/playlist/") && // We're not interested in playlists.
    !line.startsWith("https://3news.com/tdb_templates/") &&
    !line.startsWith("https://3news.com/live/") &&
    !line.startsWith("https://3news.com/showbiz/") &&
    !line.startsWith("https://3news.com/video/") &&
    !line.startsWith("https://3news.com/sports/") &&
    !line.startsWith("https://3news.com/radio-tv/") &&
    !line.startsWith("https://3news.com/business/real-estate/") &&
    !line.startsWith("https://3news.com/news/odd-but-true/") &&
    (line.count(_ == '/') == 5) && // We need no subdirectory at all.  TODO: this works for many
    line.contains('-') &&
    !line.contains("/newspaper-headlines-") &&
    !Seq(
      "https://3news.com/showbiz/art-design/"
    ).contains(line) &&
    true
  }

  def citiFmFilter(line: String): Boolean = { true &&
    line.endsWith("/") && // Articles look like a directory.
    line.startsWith("https://citifmonline.com/") &&
    !line.startsWith("https://citifmonline.com/tag/") && // We're not interested in tags.
    !line.startsWith("https://citifmonline.com/author/") && // We're not interested in playlists.
    !line.startsWith("https://citifmonline.com/category/") && // We're not interested in playlists.
//    line.contains('-') &&
    !Seq(
    ).contains(line) &&
    true
  }

  def etvFilter(line: String): Boolean = { true &&
    line.endsWith("/") && // Articles look like a directory.
    line.startsWith("https://www.etvghana.com/") &&
    !line.startsWith("https://www.etvghana.com/tag/") && // We're not interested in tags.
    !line.startsWith("https://www.etvghana.com/author/") && // We're not interested in playlists.
    !line.startsWith("https://www.etvghana.com/category/") && // We're not interested in playlists.
    //    line.contains('-') &&
    !Seq(
    ).contains(line) &&
    true
  }

  def ghanaWebFilter(line: String): Boolean = { true &&
    line.startsWith("https://www.ghanaweb.com/") &&
    !Seq(
    ).contains(line) &&
    true
  }

  def happyGhanaFilter(line: String): Boolean = { true &&
    line.endsWith("/") && // Articles look like a directory.
    line.startsWith("https://www.happyghana.com/") &&
    !line.startsWith("https://www.happyghana.com/tag/") && // We're not interested in tags.
    !line.startsWith("https://www.happyghana.com/author/") && // We're not interested in playlists.
    !line.startsWith("https://www.happyghana.com/category/") && // We're not interested in playlists.
    //    line.contains('-') &&
    !Seq(
    ).contains(line) &&
    true
  }

  Using.resources(
    FileUtils.printWriterFromFile(outFileName),
    Sourcer.sourceFromFilename(inFileName)
  ) { (printWriter, source) =>
    val lines = source.getLines
    val filteredLines = lines.filter(filter).toVector.distinct

    filteredLines.foreach { line =>
      printWriter.println(line)
      println(line)
    }
  }
}
