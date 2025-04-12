package org.clulab.habitus.scraper.scrapers.index

import net.ruippeixotog.scalascraper.browser.Browser
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.domains.{BaomoiDomain, VOVWorldDomain}
import org.clulab.habitus.scraper.scrapes.IndexScrape
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.scrapes.IndexScrape

class BaomoiIndexScraper extends PageIndexScraper (BaomoiDomain){
  def scrape(browser: Browser, page: Page, html: String): IndexScrape = {
    val doc = browser.parseString(html)
    val links = (doc >> elementList("div.content-list div.bm-card-content div.h-full a"))
      .map(_.attr("href"))
      .map(link => {
        "https://baomoi.com" + link
      })
    val scrape = IndexScrape(links)

    scrape
  }
}
