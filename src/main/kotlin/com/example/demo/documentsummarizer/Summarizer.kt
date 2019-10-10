package com.example.demo.documentsummarizer

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.stats.ClassicCounter
import edu.stanford.nlp.stats.Counter
import edu.stanford.nlp.util.CoreMap
import java.util.*
import kotlin.math.ln
import kotlin.math.round

class Summarizer(
        val dfCounter: Counter<String>
) {

    private var pipeline = StanfordCoreNLP()
    private val numDocuments: Int = dfCounter.getCount("__all__").toInt()

    init {
        val props = Properties()
        props["annotators"] = "tokenize,ssplit,pos,lemma,ner"
        props["tokenize.language"] = "en"
        props["pos.model"] = "/home/joseph.mantuano/Development/Repositories/Git/demo/document-summarizer/src/main/resources/edu/stanford/nlp/models/pos-tagger/english-bidirectional/english-bidirectional-distsim.tagger"

        pipeline = StanfordCoreNLP(props)
    }

    private fun getTermFrequencies(sentences: List<CoreMap>): Counter<String> {

        val ret: Counter<String> = ClassicCounter<String>()

        sentences.forEach { sentence ->
            sentence.get(CoreAnnotations.TokensAnnotation::class.java).forEach { coreLabel ->
                ret.incrementCount(coreLabel.get(CoreAnnotations.TextAnnotation::class.java))
            }
        }

        return ret
    }

    private fun rankSentences(sentences: List<CoreMap>, tfs: Counter<String>): List<CoreMap> {
        Collections.sort(sentences, SentenceComparator(tfs))
        return sentences
    }

    fun summarize(document: String, numSentences: Int): String {
        val annotation: Annotation = pipeline.process(document)
        var sentences: List<CoreMap> = annotation.get(CoreAnnotations.SentencesAnnotation::class.java)

        val termFrequencies: Counter<String> = getTermFrequencies(sentences)
        sentences = rankSentences(sentences, termFrequencies)

        val ret = StringBuilder()

        for (i in 0..numSentences) {
            ret.append(sentences[i])
            ret.append(" ")
        }
        return ret.toString().trim()
    }

    inner class SentenceComparator(
            private val termFrequencies: Counter<String>
    ) : Comparator<CoreMap> {
        override fun compare(coreMap0: CoreMap, coreMap1: CoreMap): Int {
            return round(score(coreMap0) - score(coreMap1)).toInt()
        }


        private fun score(sentence: CoreMap): Double {
            val tfidf: Double = tfIdfWeights(sentence)
            val index: Int = sentence.get(CoreAnnotations.SentenceIndexAnnotation::class.java)
            val indexWeight: Double = 5.0 / index
            val nerWeight: Double = entityMentionWeights(sentence)

            return nerWeight * indexWeight * tfidf * 100
        }

        private fun entityMentionWeights(sentence: CoreMap): Double {
            var total = 0.0

            sentence.get(CoreAnnotations.MentionsAnnotation::class.java).forEach { coreLabel ->
                coreLabel.get(CoreAnnotations.TextAnnotation::class.java)
                total += tfIdfWeight(coreLabel.get(CoreAnnotations.TextAnnotation::class.java))
            }

            return total
        }

        private fun tfIdfWeights(sentence: CoreMap): Double {
            var total = 0.0

            sentence.get(CoreAnnotations.TokensAnnotation::class.java).forEach { coreLabel ->
                if (coreLabel.get(CoreAnnotations.PartOfSpeechAnnotation::class.java)
                                .startsWith("n")) {
                    total += tfIdfWeight(coreLabel.get(CoreAnnotations.TextAnnotation::class.java))
                }
            }
            return total
        }

        private fun tfIdfWeight(word: String): Double {
            if (dfCounter.getCount(word).compareTo(0) == 0) {
                return 0.0
            }

            val tf: Double = 1 + ln(termFrequencies.getCount(word))
            val idf: Double = ln(numDocuments / (1 + dfCounter.getCount(word)))

            return tf * idf
        }

    }
}