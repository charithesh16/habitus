package org.clulab.habitus.scraper.scrapers.article

import org.clulab.habitus.scraper.domains.VietnamInvestmentReviewDomain
import org.json4s.DefaultFormats
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element, elementList}
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.scrapes.ArticleScrape
class VietnamInvestmentReviewArticleScraper extends PageArticleScraper(VietnamInvestmentReviewDomain){
  implicit val formats: DefaultFormats.type = DefaultFormats

  def scrape(browser: Browser, page: Page, html: String): ArticleScrape = {

    val doc = browser.parseString(html)
    val title = doc.title
    val datelineElement = doc >> element("div.article-detail > p")
    val dateline = datelineElement.text;
    val byline = (doc >> element(".article-detail-author > a"))
    val bylineOpt: Option[String] = Some("author")
    val paragraphs = doc >> elementList("#__MB_MASTERCMS_EL_3 p")
    var text = paragraphs
      .map { paragraph =>
        paragraph.text.trim
      }
      .filter(_.nonEmpty)
      .mkString("\n\n")
    if (page.url.getFile.startsWith("/media-outreach/")) {
      text = (doc >> element("#abody")).text;
    }
    ArticleScrape(page.url, Some(title), Some(dateline), bylineOpt, text)
  }
}
