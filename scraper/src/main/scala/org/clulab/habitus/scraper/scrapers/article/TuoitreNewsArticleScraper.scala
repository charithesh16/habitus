package org.clulab.habitus.scraper.scrapers.article

import org.clulab.habitus.scraper.domains.TouitreNewsDomain
import org.json4s.DefaultFormats
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element, elementList}
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.scrapes.ArticleScrape
class TuoitreNewsArticleScraper extends PageArticleScraper (TouitreNewsDomain){
  implicit val formats: DefaultFormats.type = DefaultFormats

  def scrape(browser: Browser, page: Page, html: String): ArticleScrape = {

    val doc = browser.parseString(html)
    val title = doc.title
    val datelineElement = (doc >> element("article.art-header div.date"))
    val dateline = datelineElement.text;
    val bylineOpt = (doc >> elementList("p.author")).headOption.map(_.text)
    val paragraphs = doc >> elementList("article.art-body #content-body > p")
    var text = paragraphs
      .map { paragraph =>
        paragraph.text.trim
      }
      .filter(_.nonEmpty)
      .mkString("\n\n")
    ArticleScrape(page.url, Some(title), Some(dateline), bylineOpt, text)
  }
}
