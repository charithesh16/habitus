package org.clulab.habitus.scraper.scrapers.article

import org.clulab.habitus.scraper.domains.BaolaocaiDomain
import org.json4s.DefaultFormats

import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.scrapes.ArticleScrape
import org.json4s.jackson.JsonMethods
import org.json4s.{ JArray, JObject}
class BaolaocaiArticleScraper extends PageArticleScraper(BaolaocaiDomain){
  implicit val formats: DefaultFormats.type = DefaultFormats

  def scrape(browser: Browser, page: Page, html: String): ArticleScrape = {
    val doc = browser.parseString(html)
    val title = doc.title
    val datelineElement = (doc >> elementList("div.article__meta time"))
    val dateline = datelineElement.headOption.map(_.attr("datetime"))
    val bylineOpt = (doc >> elementList("div.article__meta div.author")).headOption.map(_.text)
    val paragraphs = doc >> elementList("div.article__body > p")
    val text = paragraphs
      .map { paragraph =>
        paragraph.text.trim
      }
      .filter(_.nonEmpty)
      .mkString("\n\n")

    ArticleScrape(page.url, Some(title), dateline, bylineOpt, text,Some(""))
  }
}
