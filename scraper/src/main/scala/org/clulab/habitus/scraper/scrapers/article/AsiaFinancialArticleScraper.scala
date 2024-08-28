package org.clulab.habitus.scraper.scrapers.article

import org.clulab.habitus.scraper.domains.AsiaFinancialDomain
import org.json4s.DefaultFormats
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.scrapes.ArticleScrape
import org.json4s.jackson.JsonMethods
import org.json4s.{JArray, JObject}
class AsiaFinancialArticleScraper extends PageArticleScraper(AsiaFinancialDomain){
  implicit val formats: DefaultFormats.type = DefaultFormats

  def scrape(browser: Browser, page: Page, html: String): ArticleScrape = {
    val doc = browser.parseString(html)
    val title = doc.title
    val jObject = (doc >> elementList("script"))
      .find { element =>
        element.hasAttr("type") && element.attr("type") == "application/ld+json"
      }
      .map { element =>
        val json = element.innerHtml
        val jObject = JsonMethods.parse(json).asInstanceOf[JObject]

        jObject
      }
      .get
    val graph = (jObject \ "@graph").extract[JArray]
    val dateline = (graph(3) \ "datePublished").extract[String]
    val bylineOpt = (doc >> elementList("div.author-box div.author-content h3 a")).headOption.map(_.text)
    val paragraphs = doc >> elementList("div.content p")
    val text = paragraphs
      .map { paragraph =>
        paragraph.text.trim
      }
      .filter(_.nonEmpty)
      .mkString("\n\n")

    ArticleScrape(page.url, Some(title), Some(dateline), bylineOpt, text)
  }
}
