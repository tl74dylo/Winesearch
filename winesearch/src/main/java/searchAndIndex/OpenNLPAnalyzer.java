package searchAndIndex;

import opennlp.tools.postag.POSModel;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizer;
import org.apache.lucene.analysis.opennlp.tools.NLPPOSTaggerOp;
import org.apache.lucene.analysis.opennlp.tools.NLPSentenceDetectorOp;
import org.apache.lucene.analysis.opennlp.tools.NLPTokenizerOp;
import org.apache.lucene.analysis.opennlp.tools.OpenNLPOpsFactory;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.util.AttributeFactory;

import java.io.IOException;

public class OpenNLPAnalyzer extends Analyzer {
    protected TokenStreamComponents createComponents(String fieldName) {
        try {

            ResourceLoader resourceLoader = new ClasspathResourceLoader(ClassLoader.getSystemClassLoader());
            

            TokenizerModel tokenizerModel = OpenNLPOpsFactory.getTokenizerModel("en-token.bin", resourceLoader);
            NLPTokenizerOp tokenizerOp = new NLPTokenizerOp(tokenizerModel);

            SentenceModel sentenceModel = OpenNLPOpsFactory.getSentenceModel("en-sent.bin", resourceLoader);
            NLPSentenceDetectorOp sentenceDetectorOp = new NLPSentenceDetectorOp(sentenceModel);

            Tokenizer source = new OpenNLPTokenizer(
                    AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY, sentenceDetectorOp, tokenizerOp);

            POSModel posModel = OpenNLPOpsFactory.getPOSTaggerModel("en-pos-maxent.bin", resourceLoader);
            NLPPOSTaggerOp posTaggerOp = new NLPPOSTaggerOp(posModel);

            TokenFilter filter = new OpenNLPPOSFilter(source, posTaggerOp);

            return new TokenStreamComponents(source, filter);
        }
        catch (IOException e) {
           System.out.println("Error: TokenStream Initialization");
           e.printStackTrace();
           return null;
        }
        
    }
}
