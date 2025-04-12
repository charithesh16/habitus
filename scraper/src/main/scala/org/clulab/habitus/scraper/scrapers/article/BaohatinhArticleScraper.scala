package org.clulab.habitus.scraper.scrapers.article

import org.clulab.habitus.scraper.domains.BaohatinhDomain
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element, elementList}
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.domains.AnninhthudoDomain
import org.clulab.habitus.scraper.scrapes.ArticleScrape
import org.json4s.jackson.JsonMethods
import org.json4s.{DefaultFormats, JObject}
import org.clulab.habitus.scraper.domains.BaobackanDomain
import org.json4s.DefaultFormats
import org.clulab.habitus.scraper.domains.BNewsDomain
import org.json4s.DefaultFormats
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
class BaohatinhArticleScraper extends PageArticleScraper (BaohatinhDomain){
  implicit val formats: DefaultFormats.type = DefaultFormats

  def scrape(browser: Browser, page: Page, html: String): ArticleScrape = {
    val doc = browser.parseString(html)
    val title = doc.title

    val dateline = (doc >> elementList("div.article__meta div.meta > time")).headOption.map(_.attr("datetime"))
    val bylineOpt = (doc >> elementList("div.article__meta div.meta span.author")).headOption.map(_.text)
    val paragraphs = doc >> elementList("div.article__body > p")
    val text = paragraphs
      .map { paragraph =>
        paragraph.text.trim
      }
      .filter(_.nonEmpty)
      .mkString("\n\n")

    ArticleScrape(page.url, Some(title), Some(dateline.get), bylineOpt, text,Some(""))
  }
}
