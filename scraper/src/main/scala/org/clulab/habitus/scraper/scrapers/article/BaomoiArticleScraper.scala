package org.clulab.habitus.scraper.scrapers.article

import org.clulab.habitus.scraper.domains.BaomoiDomain
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
import org.json4s.JsonAST.{JField, JString}

import scala.util.matching.Regex
class BaomoiArticleScraper extends PageArticleScraper (BaomoiDomain){
  implicit val formats: DefaultFormats.type = DefaultFormats

  def scrape(browser: Browser, page: Page, html: String): ArticleScrape = {
    val doc = browser.parseString(html)

    val jObject = (doc >> elementList("script"))
      .find { element =>
        element.hasAttr("type") && element.attr("type") == "application/json"
      }
      .map { element =>
        val json = element.innerHtml
        val jObject = JsonMethods.parse(json).asInstanceOf[JObject]

        jObject
      }
      .get

    val props = (jObject \ "props")
    val title = (props \ "pageProps" \ "resp" \ "data" \ "head"\"meta"\"title").extract[String]
    val pageProps = (props \ "pageProps" \ "resp" \ "data" \ "content" \ "bodys").extract[JArray]
    val paragraphs_temp = (for {
      JObject(fields) <- pageProps.arr
      JField("type", JString(t)) <- fields if t == "text"
      JField("content", JString(content)) <- fields
    } yield content).toList

    val metaArray = ((props \ "pageProps" \ "resp" \ "data" \ "head"\"jsonLd").extract[JArray])
    val dateline = Some((metaArray(1) \ "datePublished").extract[String])
    val bylineOpt = Some((metaArray(1) \ "author" \ "name").extract[String])

    var text = ""
    val paragraphs = removeHtmlTagsFromList(paragraphs_temp)
    text = paragraphs
      .map { paragraph =>
        paragraph.trim
      }
      .filter(_.nonEmpty)
      .mkString("\n\n")
    ArticleScrape(page.url, Some(title), Some(dateline.get), bylineOpt, text,Some(""))
  }

  def removeHtmlTags(text: String): String = {
    val htmlTagPattern: Regex = "<.*?>".r
    htmlTagPattern.replaceAllIn(text, "")
  }

  def removeHtmlTagsFromList(textList: List[String]): List[String] = {
    textList.map(removeHtmlTags)
  }

}
