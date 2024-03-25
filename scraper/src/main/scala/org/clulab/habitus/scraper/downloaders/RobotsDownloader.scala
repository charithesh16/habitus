package org.clulab.habitus.scraper.downloaders

import net.ruippeixotog.scalascraper.browser.Browser
import org.clulab.habitus.scraper.Page
import org.clulab.habitus.scraper.domains.RobotsDomain
import org.clulab.utils.{FileUtils, Sourcer}
import sttp.client4.Response
import sttp.client4.quick._
import sttp.model.{Header, HeaderNames, MediaType, Uri}

import java.io.File
import java.net.URL
import java.nio.file.Files
import scala.io.Source
import scala.util.{Try, Using}

class RobotsDownloader() extends GetPageDownloader(new RobotsDomain()) {

  def getResponse(url: URL): Response[String] = {
    val response = quickRequest
        .get(Uri(url.toURI))
        .header(Header(HeaderNames.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0"))
        .contentType(MediaType.TextPlain)
        .send()

    response
  }

  def localDownload(page: Page, baseDirName: String, inquiryOpt: Option[String] = None): String = {
    val domain = page.url.getHost.split('.')/*.takeRight(2)*/.mkString(".") // Shorten to one .
    val dirName = cleaner.clean(domain)
    val subDirName = s"$baseDirName/$dirName"

    Files.createDirectories(new File(subDirName).toPath)

    val file = cleaner.clean(page.url.getFile) // This may automatically take off the #comment.
    val localFileName = file // This is added even if it already ended in .html.
    val localLocationName = s"$subDirName/$localFileName"

    if (!new File(localLocationName).exists) {
      // This will require Java 11.
      val response = getResponse(page.url)
      val text = {
        if (!response.isSuccess) {
          Thread.sleep(3000)

          val innerResponse = getResponse(page.url)

          if (!innerResponse.isSuccess)
            throw new RuntimeException(s"Failed to download page $Page.")
          else
            innerResponse.body
        }
        else response.body
      }

      Using.resource(FileUtils.printWriterFromFile(localLocationName)) { printWriter =>
        printWriter.println(text)
      }
      text
    }
    else {
      Using.resource(Sourcer.sourceFromFilename(localLocationName)) { sourcer =>
        sourcer.mkString
      }
    }
  }

  override def download(browser: Browser, page: Page, baseDirName: String, inquiryOpt: Option[String] = None): Unit = {
    val robotsPage = new Page(new URL(s"${page.url}/robots.txt"))

    // Download this, find the file and parse it.
    // Then write file with list of all xml files found
    val text = localDownload(robotsPage, baseDirName, inquiryOpt)
    // TODO: continue to get strings out of it if there are any and download all those files
    //
    println(text)
  }
}
