package org.clulab.habitus.scraper.scrapers.index

import org.clulab.habitus.scraper.domains.VietnamNewsDomain
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.scrapes.IndexScrape
class VietnamNewsIndexScraper  extends PageIndexScraper(VietnamNewsDomain){
  def scrape(browser: Browser, page: Page, html: String): IndexScrape = {
    val doc = browser.parseString(html)
    val links = (doc >> elementList("div.timeline article h2 > a"))
      .map(_.attr("href"))
    val scrape = IndexScrape(links)

    scrape
  }

}
