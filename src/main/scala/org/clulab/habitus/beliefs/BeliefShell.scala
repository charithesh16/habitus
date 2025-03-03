package org.clulab.habitus.beliefs

import org.clulab.habitus.utils.displayMentions
import org.clulab.utils.{CliReader, ExitMenuItem, HelpMenuItem, Menu, SafeDefaultMenuItem, Shell, SimpleMainMenuItem}

class BeliefShell extends Shell {
  println("Creating BeliefProcessor...\n")
  private var beliefProcessor: BeliefProcessor = BeliefProcessor()

  def reload(): Unit = {
    println("Reloading VariableProcessor...")
    beliefProcessor = BeliefProcessor(beliefProcessor.processor, beliefProcessor.entityFinder)
  }

  override def work(text: String): Unit = {
    // the actual reading
    val parsingResults = beliefProcessor.parse(text)
    val doc = parsingResults.document
    val targetMentions = parsingResults.targetMentions

    // debug display the mentions
    displayMentions(targetMentions, doc)
  }

  override def mkMenu(): Menu = {
    val lineReader = new CliReader("(Habitus)>>> ", "user.home", ".habitusshellhistory")
    val mainMenuItems = Seq(
      new HelpMenuItem(":help", "show commands"),
      new SimpleMainMenuItem(":reload", "reload the variable processor", reload _),
      new ExitMenuItem(":exit", "exit system")
    )
    val defaultMenuItem = new SafeDefaultMenuItem(work)

    new Menu("Welcome to the Habitus Shell!", lineReader, mainMenuItems, defaultMenuItem)
  }
}

object BeliefShell extends App {
  val sh = new BeliefShell()
  sh.shell()
}
