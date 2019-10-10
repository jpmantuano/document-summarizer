package com.example.demo.documentsummarizer

import edu.stanford.nlp.simple.Sentence
import org.springframework.boot.autoconfigure.SpringBootApplication
import kotlin.math.ln


@SpringBootApplication
class DocumentSummarizerApplication

fun main() {
//
//    val sentence = Sentence("fumes poisonous bribery arrest")
//    println(sentence.words())

    print(ln(0.0) * 100)

//    DocumentFrequencyCounter().buildDFCounter(file, OUT_FILE)
//    val dfCounter = DocumentFrequencyCounter().loadDfCounter(OUT_FILE)
//
//    val summarizer = Summarizer(dfCounter)
//    val result = summarizer.summarize(document, 1)
//
//    println(result)
}

val file = "/home/joseph.mantuano/Development/Repositories/Git/demo/document-summarizer/src/main/resources/test_data/technology-49551880.xml"
const val OUT_FILE = "/home/joseph.mantuano/Development/Repositories/Git/demo/document-summarizer/src/main/resources/counter/df-counts.ser"


val document = "Boris Johnson has written to the EU suggesting the backstop could be replaced by some form of commitment to prevent a hard Irish border " +
        "in his first major move to explain the UK government’s new position to Brussels.\n" +
        "Ahead of talks with Angela Merkel and Emmanuel Macron, " +
        "Johnson released a four-page letter setting out his position that the backstop is “anti-democratic and inconsistent " +
        "with the sovereignty of the UK”, because it could keep the UK indefinitely in a customs union with no means of exit.\n" +
        "'Reckless' plan to cut off free movement alarms EU nationals\n" +
        "He proposed that alternative customs arrangements could be put in place at the Irish border within the two-year " +
        "transitional period after Brexit, but suggested some unspecified commitments could give confidence that there will be no " +
        "hard border on the island if this system is not ready by that point. \n" +
        "The release of the letter, addressed to Donald Tusk, the European council president, appears intended to portray " +
        "Johnson as willing to negotiate with Brussels, even though he is making a demand for the abolition of the backstop that they have repeatedly rebuffed."

