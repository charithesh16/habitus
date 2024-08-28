package org.clulab.habitus.scraper.scrapers.index

import org.clulab.habitus.scraper.domains.AsiaFinancialDomain
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.scrapes.IndexScrape
class AsiaFinancialIndexScraper extends PageIndexScraper(AsiaFinancialDomain){
  def scrape(browser: Browser, page: Page, html: String): IndexScrape = {
    val doc = browser.parseString(html)
    val links = (doc >> elementList("div.tt-post div.tt-post-info > a"))
      .map(_.attr("href"))
    val scrape = IndexScrape(links)

    scrape
  }

}
