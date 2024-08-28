package org.clulab.habitus.scraper.scrapers.index

import org.clulab.habitus.scraper.domains.VietnamInvestmentReviewDomain
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.scrapes.IndexScrape

class VietnamInvestmentReviewIndexScraper extends PageIndexScraper(VietnamInvestmentReviewDomain){
  def scrape(browser: Browser, page: Page, html: String): IndexScrape = {
    val doc = browser.parseString(html)
    val links = (doc >> elementList("div.article h3 > a"))
      .map(_.attr("href"))
      .map(link => {
        if(!link.startsWith("https://vir.com.vn")){
          "https://vir.com.vn/" + link
        }else{
          link
        }
      })
    val scrape = IndexScrape(links)

    scrape
  }
}
