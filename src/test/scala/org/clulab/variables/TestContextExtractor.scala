package org.clulab.variables

import org.clulab.odin.Mention
import org.scalatest.{FlatSpec, Matchers}

//
// TODO: write tests for all sentences, similar to this: https://github.com/clulab/reach/blob/master/main/src/test/scala/org/clulab/reach/TestActivationEvents.scala
//

class TestContextExtractor extends FlatSpec with Matchers {
  val vp = VariableProcessor()

//pass1: test sentences which have only one event overall in the document
  val sent1 ="""
In Matto Grosso  with sowing between 7 and 22 July, in  maturity came in early November ( Tab.I ) .
In United States , United States  maturity came in early November ( Tab.I ) .
In Senegal maturity  came in early November ( Tab.I ) for 1995.
In Senegal maturity  came in early November ( Tab.I ) for 1995.
In Senegal maturity  came in early November ( Tab.I ) for 1995.
"""

  def getMSFreq(text: String): Seq[String] = {
    val (doc, mentions) = vp.parse(text)
    val mse=vp.extractContextAndFindMostFrequentEntity(doc,mentions,Int.MaxValue,"LOC")
    (mse)
  }

  sent1 should "for the event sowing between 7 and 22 July find senegal as the most frequent entity in entire document" in {
    val mse = getMSFreq(sent1)
    mse.head should be ("senegal")
  }
  def getMSFreq1Sent(text: String): Seq[String] = {
    val (doc, mentions) = vp.parse(text)
    val mse=vp.extractContextAndFindMostFrequentEntity(doc,mentions,1,"LOC")
    (mse)
  }

  sent1 should "find united states as the most frequent entity within 1 sentence distance" in {
    val mse = getMSFreq1Sent(sent1)
    mse.head should be ("united states")
  }

  def getMSFreq0Sent(text: String): Seq[String] = {
    val (doc, mentions) = vp.parse(text)
    val mse=vp.extractContextAndFindMostFrequentEntity(doc,mentions,0,"LOC")
    (mse)
  }

  sent1 should "find matto grosso  as the most frequent entity within 0 sentence distance" in {
    val mse = getMSFreq0Sent(sent1)
    mse.head should be ("matto grosso")
  }
  // test document which have more than one event mention  in the document
  val sent2 ="""
In Matto Grosso  with sowing between 7 and 22 July, in  maturity came in early November ( Tab.I ) .
In United States , United States  maturity came in early November ( Tab.I ) .
In Senegal maturity  came in early November ( Tab.I ) for 1995.
In Senegal maturity  came in early November ( Tab.I ) for 1995.
In Burkino Faso with sowing between 8 and 12 July, in  maturity came in early November ( Tab.I ) .
In Senegal maturity  came in early November ( Tab.I ) for 1995.
"""

  sent2 should "for document with two events find senegal as the most frequent entity in entire document" in {
    val mse = getMSFreq(sent2)
    mse(0) should be ("senegal")
    mse(1) should be ("senegal")
  }

  sent2 should "for document with two events find matto grosso as the most frequent entity in event1 and burkino fast for event 0" in {
    val mse = getMSFreq0Sent(sent2)
    mse(0) should be ("matto grosso")
    mse(1) should be ("burkino faso")
  }
}
