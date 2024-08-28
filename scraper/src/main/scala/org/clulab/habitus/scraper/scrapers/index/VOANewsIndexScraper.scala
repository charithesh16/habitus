package org.clulab.habitus.scraper.scrapers.index

import org.clulab.habitus.scraper.domains.VOANewsDomain
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.scrapes.IndexScrape
class VOANewsIndexScraper extends PageIndexScraper(VOANewsDomain){
  def scrape(browser: Browser, page: Page, html: String): IndexScrape = {
    val doc = browser.parseString(html)
    val links = (doc >> elementList("div.row ul li div.media-block div > a"))
      .map(_.attr("href"))
      .map(link => {
        "https://www.voanews.com"+link
      })
    val scrape = IndexScrape(links)

    scrape
  }
}
