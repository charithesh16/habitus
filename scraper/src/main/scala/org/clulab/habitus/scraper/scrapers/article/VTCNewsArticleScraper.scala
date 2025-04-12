package org.clulab.habitus.scraper.scrapers.article

import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element, elementList}
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.domains.VTCNewsDomain
import org.clulab.habitus.scraper.scrapes.ArticleScrape
import org.json4s.{DefaultFormats, JObject}
import org.json4s.jackson.JsonMethods
import org.clulab.habitus.scraper.domains.TheInvestorDomain
import org.json4s.DefaultFormats
import org.json4s.DefaultFormats
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element, elementList}
import org.clulab.habitus.scraper.domains.VOANewsDomain
import org.clulab.habitus.scraper.scrapes.ArticleScrape
import org.json4s.jackson.JsonMethods
import org.json4s.{DefaultFormats, JArray, JObject}
import net.ruippeixotog.scalascraper.dsl.DSL._

class VTCNewsArticleScraper extends PageArticleScraper(VTCNewsDomain){
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

//    val dateline = (jObject \ "datePublished").extract[String]
    val dateline = (doc >> elementList("span.time-update")).headOption.map(_.text)
    val bylineOpt = (doc >> elementList("div.detail-content div.detail-signature > span")).headOption.map(_.text)

    val paragraphs = doc >> elementList("div.edittor-content > p")
    var text = ""
    if (paragraphs.isEmpty) {
      text = (doc >> element("div.edittor-content")).text
    } else {
      text = paragraphs
        .map { paragraph =>
          paragraph.text.trim
        }
        .filter(_.nonEmpty)
        .mkString("\n\n")
    }
    ArticleScrape(page.url, Some(title), Some(dateline.get), bylineOpt, text,Some(""))
  }

}
