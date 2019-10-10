package com.example.demo.documentsummarizer.annotator

import edu.stanford.nlp.ling.CoreAnnotation
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.pipeline.Annotator
import org.apache.lucene.analysis.core.StopAnalyzer
import org.apache.lucene.analysis.util.CharArraySet
import java.util.*

class StopwordAnnotator(
        val annotatorClass: String,
        val properties: Properties
) : Annotator, CoreAnnotation<Pair<Boolean, Boolean>> {

    companion object {
        val ANNOTATOR_CLASS = "stopword"
        val STANDFORD_STOPWROD = ANNOTATOR_CLASS

        val STOPWORD_LIST = "stopword-list"
        val IGNORE_STOPWORD_CASE = "ignore-stopword-case"
        val CHECK_LEMMA = "check-lemma"
        private val boolPair = Pair(true, true).javaClass

        private fun getStopwords(stopwords: String, ignoreCase: Boolean): CharArraySet {
            val terms = stopwords.split(",")
            val stopwordSet = CharArraySet(terms.size, ignoreCase)

            terms.forEach { term ->
                stopwordSet.add(term)
            }

            return CharArraySet.unmodifiableSet(stopwordSet)
        }
    }

    var stopwords = StopAnalyzer.ENGLISH_STOP_WORDS_SET
    var checkLemma = false

    init {
        val checkLemma = properties.getProperty(CHECK_LEMMA)?.toBoolean() ?: false

        if (properties.containsKey(STOPWORD_LIST)) {
            val stopList = properties.getProperty(STOPWORD_LIST)
            val ignoreCase = properties.getProperty(IGNORE_STOPWORD_CASE)?.toBoolean() ?: false
            stopwords = getStopwords(stopList, ignoreCase)
        }
    }

    override fun annotate(annotation: Annotation) {
        if (stopwords.isNotEmpty() &&
                annotation.containsKey(CoreAnnotations.TokensAnnotation::class.java)) {
            val tokens = annotation.get(CoreAnnotations.TokensAnnotation::class.java)

            tokens.forEach { token ->
                val isStopword = stopwords.contains(token.word().toLowerCase())
                val isLemmaStopword = if (checkLemma) stopwords.contains(token.word().toLowerCase()) else false;
                val pair = Pair(isStopword, isLemmaStopword)

                token.set(StopwordAnnotator::class.java, pair)
            }
        }
    }

    override fun requires(): MutableSet<Class<out CoreAnnotation<out Any>>> {
        return if (checkLemma) {
            mutableSetOf(CoreAnnotations.TextAnnotation::class.java,
                    CoreAnnotations.TokensAnnotation::class.java,
                    CoreAnnotations.SentencesAnnotation::class.java,
                    CoreAnnotations.PartOfSpeechAnnotation::class.java,
                    CoreAnnotations.LemmaAnnotation::class.java)
        } else {
            mutableSetOf(CoreAnnotations.TextAnnotation::class.java,
                    CoreAnnotations.TokensAnnotation::class.java,
                    CoreAnnotations.SentencesAnnotation::class.java)
        }
    }

    override fun requirementsSatisfied(): MutableSet<Class<out CoreAnnotation<out Any>>> {
        return mutableSetOf(CoreAnnotations.LemmaAnnotation::class.java)
    }

    override fun getType(): Class<Pair<Boolean, Boolean>> {
        return boolPair
    }
}