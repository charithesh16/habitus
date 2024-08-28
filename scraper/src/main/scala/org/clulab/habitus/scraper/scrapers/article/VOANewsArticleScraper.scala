package org.clulab.habitus.scraper.scrapers.article

import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element, elementList}
import org.clulab.habitus.scraper.domains.VOANewsDomain
import org.clulab.habitus.scraper.scrapes.ArticleScrape
import org.json4s.jackson.JsonMethods
import org.json4s.{DefaultFormats, JArray, JObject}
import net.ruippeixotog.scalascraper.dsl.DSL._

import org.clulab.habitus.scraper.Page

class VOANewsArticleScraper extends PageArticleScraper(VOANewsDomain){
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

    val dateline = (jObject \ "datePublished").extract[String]
    val bylineOpt = (doc >> elementList("div.links ul li a")).headOption.map(_.text)

    val paragraphs = doc >> elementList("#article-content div.wsw > p")
    var text = ""
    if (paragraphs.isEmpty) {
      text = (doc >> element("#article-content div.wsw")).text
    }else{
      text = paragraphs
        .map { paragraph =>
          paragraph.text.trim
        }
        .filter(_.nonEmpty)
        .mkString("\n\n")
    }
    ArticleScrape(page.url, Some(title), Some(dateline), bylineOpt, text)
  }

}
