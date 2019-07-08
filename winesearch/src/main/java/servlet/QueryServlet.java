package servlet;

import searchAndIndex.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;	//MultiFieldQueryParser importiert
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class QueryServlet
 */
public class QueryServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
    public QueryServlet() {
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String query = request.getParameter("query");
		System.out.println(FSDirectory.open(Paths.get("index")));
		PrintWriter writer = response.getWriter();
		writer.println("Hallo " + query);
		
		
		
		try {
			searchIndexedFiles(writer, query);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		writer.close();
		
	}
	
	public void searchIndexedFiles(PrintWriter writer, String queryString) throws Exception{
		   
		   // Pruefe, ob Eingabe gueltig und trim gleich die Eingabe
		   if(queryString.length() == -1 || queryString == null || (queryString = queryString.trim()).length() == 0) {
		   writer.println("Ungueltige Eingabe");
		   return;
		   }
		   
		   // Pruefe, ob gueltiger Index vorhanden ist
		   String field = "contents";
		   
		   IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("index")));
		   IndexSearcher searcher = new IndexSearcher(reader);
		   Analyzer analyzer = new StandardAnalyzer();
		   
		   // Fuehre Query aus
		   MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[] {"id","country", "description", "designation", "points", "price", "province", "taster", "title", "variety", "winery"}, analyzer);
		   Query query = parser.parse(queryString);
		   
		   // Sammle die Dokumente
		   TopDocs results = searcher.search(query, 50);
		   ScoreDoc[] hits = results.scoreDocs;
		   int numTotalHits = Math.toIntExact(results.totalHits.value);
		   writer.println(numTotalHits + " passende Ergebnisse");
		   
		   // Durchlaufe Ergebnis (nur die ersten 50)
		   for (int i= 0; i< 50; i++) {
		   Document doc = searcher.doc(hits[i].doc);
		         String path = doc.get("id");
		         if (path != null) {
		           writer.println((i+1) + ". ");
		           String title = doc.get("title");
		           if (title != null) {
		          writer.println("Id: " + doc.get("id"));
		             writer.println("   Title: " + doc.get("title"));
		             writer.println("   Content: " + doc.get("description"));
		             writer.println("\n");
		           }
		         } else {
		           System.out.println((i+1) + ". " + "No path for this document");
		         }
		   }
		   
		   
		   
		  }
	  

}
