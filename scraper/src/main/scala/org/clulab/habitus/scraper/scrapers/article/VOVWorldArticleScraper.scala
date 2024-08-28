package org.clulab.habitus.scraper.scrapers.article

import org.clulab.habitus.scraper.domains.VOVWorldDomain
import org.json4s.DefaultFormats
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element, elementList}
import org.clulab.habitus.scraper.domains.VOANewsDomain
import org.clulab.habitus.scraper.scrapes.ArticleScrape
import org.json4s.jackson.JsonMethods
import org.json4s.{DefaultFormats, JArray, JObject}
import net.ruippeixotog.scalascraper.dsl.DSL._

import org.clulab.habitus.scraper.Page
class VOVWorldArticleScraper extends PageArticleScraper (VOVWorldDomain){
  implicit val formats: DefaultFormats.type = DefaultFormats

  def scrape(browser: Browser, page: Page, html: String): ArticleScrape = {
    val doc = browser.parseString(html)
    val title = doc.title
    val dateline = (doc >> elementList("div.article__meta time")).headOption.map(_.text)
    val bylineOpt = (doc >> elementList("div.links ul li a")).headOption.map(_.text)

    val paragraphs = (doc >> elementList("div.article__body")).headOption.map(_.text)
    val text = paragraphs
//    if (paragraphs.isEmpty) {
//      text = (doc >> element("#article-content div.wsw")).text
//    } else {
//      text = paragraphs
//        .map { paragraph =>
//          paragraph.text.trim
//        }
//        .filter(_.nonEmpty)
//        .mkString("\n\n")
//    }
    ArticleScrape(page.url, Some(title), Some(dateline.get), bylineOpt, text.get)
  }
}
