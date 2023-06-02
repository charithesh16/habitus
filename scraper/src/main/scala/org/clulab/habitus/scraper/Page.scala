package org.clulab.habitus.scraper

import java.net.URL

class Page(val url: URL)

object Page {

  def apply(urlName: String): Page = new Page(new URL(urlName))
}
