package com.example.demo.documentsummarizer

import edu.stanford.nlp.io.IOUtils
import edu.stanford.nlp.io.ReaderInputStream
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.stats.ClassicCounter
import edu.stanford.nlp.stats.Counter
import edu.stanford.nlp.tagger.maxent.MaxentTagger
import edu.stanford.nlp.util.CoreMap
import edu.stanford.nlp.util.XMLUtils
import org.springframework.util.ResourceUtils
import org.w3c.dom.Element
import org.xml.sax.SAXParseException
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.regex.Pattern
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class DocumentFrequencyCounter {

    private var pipeline = StanfordCoreNLP()

    init {
        val props = Properties()
        props["annotators"] = "tokenize,ssplit,pos,lemma,ner"
        props["tokenize.language"] = "en"
        props["pos.model"] = ResourceUtils.getFile("/edu/stanford/nlp/models/pos-tagger/english-bidirectional/english-bidirectional-distsim.tagger")
        pipeline = StanfordCoreNLP(props)
    }

    companion object {
        private val tagger = MaxentTagger("edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger")
        private val headingSeparator: Pattern = Pattern.compile("[-=]{3,}")
        private val paragraphMarker: Pattern = Pattern.compile("</?(?:TEXT|P)>(\n|$)")

        private const val MAX_SENTENCE_LENGTH: Int = 100
        private const val TAG_DOCUMENT = "DOC"
        private const val TAG_TEXT = "TEXT"

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    }

    fun buildDFCounter(inFile: String, outFile: String) {
        val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        val futures = ArrayList<Future<Counter<String>>>()

        futures.add(pool.submit(DocumentFrequencyCounter().FileIDBuilder(File(inFile))))

        var finished = 0
        val overall = ClassicCounter<String>()

        for (future in futures) {
            System.err.printf("%s: Polling future #%d",
                    dateFormat.format(Date()), finished + 1)

            val result = future.get()
            finished++

            System.err.printf("%s: Finished future #%d",
                    dateFormat.format(Date()), finished)

            System.err.printf("\tMerging counter.. ")
            overall.addAll(result)
            System.err.printf("done.%n")
        }

        pool.shutdown()

        System.err.printf("\n%s: Saving to '%s'.. ", dateFormat.format(Date()),
                outFile)
        val oos = ObjectOutputStream(FileOutputStream(outFile))
        oos.writeObject(overall)
        System.err.printf("done.%n")
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    fun loadDfCounter(outFile: String): Counter<String> {
        val ois = ObjectInputStream(FileInputStream(outFile))
        return ois.readObject() as Counter<String>
    }

    private fun getIDFMapForDocument(document: String): Counter<String> {
//        val preprocessor = DocumentPreprocessor(
//                StringReader(
//                        headingSeparator.matcher(document)
//                                .replaceAll("")))

        val annotation: Annotation = pipeline.process(document)
        val sentences: List<CoreMap> = annotation.get(CoreAnnotations.SentencesAnnotation::class.java)

        val idfMap = ClassicCounter<String>()

        sentences.forEach { sentence ->
            sentence.get(CoreAnnotations.TokensAnnotation::class.java).forEach { coreLabel ->
                val pos = coreLabel.get(CoreAnnotations.PartOfSpeechAnnotation::class.java)
                if (pos.startsWith("N")) {
                    idfMap.incrementCount(coreLabel.get(CoreAnnotations.TextAnnotation::class.java).toLowerCase())
                }
            }

            sentence.get(CoreAnnotations.MentionsAnnotation::class.java).forEach { coreLabel ->
                idfMap.incrementCount(coreLabel.get(CoreAnnotations.TextAnnotation::class.java).toLowerCase())
            }
        }

        return idfMap
    }

//    private fun getEntityCount(document: String): Counter<String> {
//        val coreDocument = CoreDocument(document)
//        pipeline.annotate(coreDocument)
//
//        val idfMap = ClassicCounter<String>()
//
//        for (sentence in coreDocument.sentences()) {
//            if (sentence.text().length > MAX_SENTENCE_LENGTH)
//                continue
//            sentence.entityMentions().forEach { coreEntity ->
//                if (coreEntity.entityType() == "PERSON" ||
//                        coreEntity.entityType() == "ORGANIZATION") {
//                    idfMap.incrementCount(coreEntity.text())
//                }
//            }
//        }
//
//        return idfMap
//    }


    @Throws(TransformerException::class)
    private fun getFullTextContent(e: Element): String {
        val transFactory = TransformerFactory.newInstance()
        val transformer = transFactory.newTransformer()
        val buffer = StringWriter()

        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        transformer.transform(DOMSource(e), StreamResult(buffer))

        var str = buffer.toString().trim()

        // Remove paragraph markers
        str = paragraphMarker.matcher(str).replaceAll("")

        return str
    }

    @Throws(SAXParseException::class, IOException::class, TransformerException::class)
    private fun getIDMapForFile(file: Reader): Counter<String> {
        val parser = XMLUtils.getXmlParser()
        val xml = parser.parse(ReaderInputStream(file))
        val docNodes = xml.documentElement.getElementsByTagName(TAG_DOCUMENT)

        val idfMap: Counter<String> = ClassicCounter<String>()

        for (i in 0 until docNodes.length) {
            val doc = docNodes.item(i) as Element
            val texts = doc.getElementsByTagName(TAG_TEXT)

            assert(texts.length.compareTo(1) == 0)

            val text = texts.item(0) as Element
            val textContent = getFullTextContent(text)

            idfMap.addAll(getIDFMapForDocument(textContent))
//            idfMap.addAll(getEntityCount(textContent))
            idfMap.incrementCount("__all__")
        }
        return idfMap
    }

    inner class FileIDBuilder(
            val file: File
    ) : Callable<Counter<String>> {
        override fun call(): Counter<String> {
            var fileContents = IOUtils.slurpFile(file)
            fileContents = "<docs> $fileContents </docs>"

            return getIDMapForFile(StringReader(fileContents))
        }
    }
}