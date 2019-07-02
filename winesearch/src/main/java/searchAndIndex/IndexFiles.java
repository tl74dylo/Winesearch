package searchAndIndex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.opencsv.CSVReader;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {
  
  private IndexFiles() {}

  /** Index all text files under a directory. */
  public static void main(String[] args) {
    String usage = "java org.apache.lucene.demo.IndexFiles"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    String indexPath = "index";
    String docsPath = "LuceneTestData/";
    boolean create = true;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      } else if ("-update".equals(args[i])) {
        create = false;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final Path docDir = Paths.get(docsPath);
    if (!Files.isReadable(docDir)) {
      System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    
    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(Paths.get(indexPath));
      Analyzer analyzer = new StandardAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      if (create) {
        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);
      } else {
        // Add new documents to an existing index:
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      }

      // Optional: for better indexing performance, if you
      // are indexing many documents, increase the RAM
      // buffer.  But if you do this, increase the max heap
      // size to the JVM (eg add -Xmx512m or -Xmx1g):
      //
      // iwc.setRAMBufferSizeMB(256.0);

      IndexWriter writer = new IndexWriter(dir, iwc);
      indexCSV(writer, docDir);

      // NOTE: if you want to maximize search performance,
      // you can optionally call forceMerge here.  This can be
      // a terribly costly operation, so generally it's only
      // worth it when your index is relatively static (ie
      // you're done adding documents to it):
      //
      // writer.forceMerge(1);

    writer.close();

     Date end = new Date();
     System.out.println(end.getTime() - start.getTime() + " total milliseconds");

   } catch (IOException e) {
     System.out.println(" caught a " + e.getClass() +
      "\n with message: " + e.getMessage());
   }
 }

 /**
  * Indexes the given file using the given writer, or if a directory is given,
  * recurses over files and directories found under the given directory.
  * 
  * NOTE: This method indexes one document per input file.  This is slow.  For good
  * throughput, put multiple documents into your input file(s).  An example of this is
  * in the benchmark module, which can create "line doc" files, one document per line,
  * using the
  * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
  * >WriteLineDocTask</a>.
  *  
  * @param writer Writer to the index where the given file/dir info will be stored
  * @param path The file to index, or the directory to recurse into to find files to index
  * @throws IOException If there is a low-level I/O error
  */
 static void indexDocs(final IndexWriter writer, Path path) throws IOException {
   if (Files.isDirectory(path)) {
     Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
       @Override
       public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
         try {
           indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
         } catch (IOException ignore) {
           // don't index files that can't be read.
         }
         return FileVisitResult.CONTINUE;
       }
     });
   } else {
     indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
   }
 }


 /** Indexes a single document */
 static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
   try (InputStream stream = Files.newInputStream(file)) {
     // make a new, empty document
     Document doc = new Document();
     
     // Add the path of the file as a field named "path".  Use a
     // field that is indexed (i.e. searchable), but don't tokenize 
     // the field into separate words and don't index term frequency
     // or positional information:
     Field pathField = new StringField("path", file.toString(), Field.Store.YES);
     doc.add(pathField);
     
     // Add the last modified date of the file a field named "modified".
     // Use a LongPoint that is indexed (i.e. efficiently filterable with
     // PointRangeQuery).  This indexes to milli-second resolution, which
     // is often too fine.  You could instead create a number based on
     // year/month/day/hour/minutes/seconds, down the resolution you require.
     // For example the long value 2011021714 would mean
     // February 17, 2011, 2-3 PM.
     doc.add(new LongPoint("modified", lastModified));
     
     // Add the contents of the file to a field named "contents".  Specify a Reader,
     // so that the text of the file is tokenized and indexed, but not stored.
     // Note that FileReader expects the file to be in UTF-8 encoding.
     // If that's not the case searching for special characters will fail.
     doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
     
     if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
       // New index, so we just add the document (no old document can be there):
       System.out.println("adding " + file);
       writer.addDocument(doc);
     } else {
       // Existing index (an old copy of this document may have been indexed) so 
       // we use updateDocument instead to replace the old one matching the exact 
       // path, if present:
       System.out.println("updating " + file);
       writer.updateDocument(new Term("path", file.toString()), doc);
     }
   }
  }
 
 static void indexCSV (final IndexWriter writer, Path path) throws IOException{
	   CSVReader reader = new CSVReader(new FileReader("winemag-data-130k-v2.csv"));
    String [] nextLine;
    
    int count = 0;
    int words = 0;
    
    while ((nextLine = reader.readNext()) != null) {
       // nextLine[] is an array of values from the line
       //System.out.println(nextLine[0] + " " + nextLine[1] + " " + nextLine[2] + " " + nextLine[3] + " " + nextLine[4] +nextLine[5] + nextLine[6] + " " + nextLine[7] + " " + nextLine[8]);
    	System.out.println(nextLine[2] + " " + nextLine[2].split(" ").length);
    	count = count + 1;
    	words = words + nextLine[2].split(" ").length;
    	
    	 // make a new, empty document
        Document doc = new Document();
        
        // ID 
        Field idField = new TextField("id", nextLine[0], Field.Store.YES); //Zu lowercase geaendert fuer Konsistenz 
        doc.add(idField);
        
        // Country
        Field countryField = new TextField("country", nextLine[1], Field.Store.YES);
        doc.add(countryField);
        
        // Description
        Field descField = new TextField("description", nextLine[2], Field.Store.YES); //Schreibfehler korrigiert
        doc.add(descField);
        
        // Designation
        Field designationField = new TextField("designation", nextLine[3], Field.Store.YES);
        doc.add(designationField);
        
        // Points
        Field pointField = new TextField("points", nextLine[4], Field.Store.YES); //"price" zu "points" geaendert
        doc.add(pointField);
        
        // Price
        Field priceField = new TextField("price", nextLine[5], Field.Store.YES);
        doc.add(priceField);
        
        // Province
        Field provinceField = new TextField("province", nextLine[6], Field.Store.YES);
        doc.add(provinceField);
        
        // Taster
        Field tasterField = new TextField("taster", nextLine[9], Field.Store.YES);
        doc.add(tasterField);
        
        // Title
        Field titleField = new TextField("title", nextLine[11], Field.Store.YES);
        doc.add(titleField);
        
        // Variety
        Field varietyField = new TextField("variety", nextLine[12], Field.Store.YES);
        doc.add(varietyField);
        
        // Winery
        Field wineryField = new TextField("winery", nextLine[13], Field.Store.YES); //"points" zu "winery" geaendert
        doc.add(wineryField);
        
        
        if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):
            System.out.println("adding ID" + nextLine[0]);
            writer.addDocument(doc);
          } else {
            // Existing index (an old copy of this document may have been indexed) so 
            // we use updateDocument instead to replace the old one matching the exact 
            // path, if present:
            System.out.println("updating ID" + nextLine[0]);
            //writer.updateDocument(new Term("path", file.toString()), doc);
          }
        
        
        
        
    }
    reader.close();
    System.out.println("Average Description Length: " + words/count);
}
}
