package org.clulab.habitus.variables

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.clulab.odin.Mention

class TestAreaReader extends FlatSpec with Matchers {
  val vp = VariableProcessor("/variables/master-areas.yml")

  def getMentions(text: String): Seq[Mention] = {
    val (_, mentions, _, _) = vp.parse(text)
    mentions
  }

  behavior of "AreaReader"

  val sent1 = "28,223 ha vs 35,065 ha were used as sown areas"
  sent1 should "recognize area sizes" in {
    val areaMentions = getMentions(sent1).filter(_.label matches "Assignment")
    areaMentions should have size (2)
    areaMentions.foreach({ m =>
      m.arguments("variable").head.text should be("areas")
    })
    areaMentions.head.arguments("value").head.text should equal("28,223 ha")
    areaMentions.last.arguments("value").head.text should equal("35,065 ha")

  }

  val sent2 = "The areas sown for this 2021/2022 wintering campaign are 28,223 ha vs 35,065 ha in wintering"
  sent2 should "recognize area sizes" in {
    val areaMentions = getMentions(sent2).filter(_.label matches "Assignment")
    areaMentions should have size (2)
    areaMentions.foreach({ m =>
      m.arguments("variable").head.text should be("areas")
    })
    areaMentions.head.arguments("value").head.text should equal("28,223 ha")
    areaMentions.last.arguments("value").head.text should equal("35,065 ha")

  }
}