package servlet;

import searchAndIndex.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.ArrayList;

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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
	private int numOfResults = 0;

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
		String evalMode = request.getParameter("eval");
		boolean eval = ((evalMode!= null) ? true: false);

		
		//PrintWriter writer = response.getWriter();
		logge(query);
		 ArrayList<Document> resultsList = new ArrayList<Document>();
		try {
			//resultsList = searchIndexedFiles(writer, query);
			resultsList = searchIndexedFiles(query);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		setupRequest(request, query, eval);
		request.setAttribute("results", resultsList);
		request.getRequestDispatcher("results.jsp").forward(request, response);
		
		
	}
	
	public ArrayList<Document> searchIndexedFiles( String queryString) throws Exception{
		   
		 ArrayList<Document> resultsList = new ArrayList<Document>();
		
		   // Pruefe, ob Eingabe gueltig und trim gleich die Eingabe
		   if(queryString.length() == -1 || queryString == null || (queryString = queryString.trim()).length() == 0) {
		   //writer.println("Ungueltige Eingabe");
		   return resultsList;
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
		   numOfResults = Math.toIntExact(results.totalHits.value);
		   //writer.println(numTotalHits + " passende Ergebnisse");
		   
		  
		   
		   // Durchlaufe Ergebnis (nur die ersten 50)
		try{  
		for (int i= 0; i< 50; i++) {
		   Document doc = searcher.doc(hits[i].doc);
		         String path = doc.get("id");
		         if (path != null) {
		           //writer.println((i+1) + ". ");
		           String title = doc.get("title");
		           if (title != null) {
					/*
					 * writer.println("Id: " + doc.get("id")); writer.println("   Title: " +
					 * doc.get("title")); writer.println("   Content: " + doc.get("description"));
					 * writer.println("\n");
					 */
		        	   resultsList.add(doc);
		           }
		         } else {
		           System.out.println((i+1) + ". " + "No path for this document");
		         }
		   }
		}catch (ArryIndexOutOfBoundsException aoe){
		}
		   
		   return resultsList;
		   
		  }
	
	
	/* setupRequest
	 * - setzt Attribute fuer request 
	 * - abhängig ob Evaluierungsmodus aktiv ist 
	 * - liest vorhandenes JSON Evaluierungsfile fuer Query aus, wenn vorhanden
	 */
	
	private void setupRequest(HttpServletRequest request, String query, boolean eval) {
		boolean fileExists = false;
		// Pruefe im Evaluierungsmodus, ob es fuer diese Query schon ein passendes JSON Dokument gibt
		if(eval){
			String fileName = "Eval_" + query.replaceAll(" ", "_") + ".json";
			fileExists = (new File(fileName)).exists();
			
			if(fileExists){
				System.out.println("File exists");
				request.setAttribute("control", parseEvaluationList(fileName));
			}
		}
		
		request.setAttribute("query", query);
		request.setAttribute("eval", eval);
		request.setAttribute("resultCount", numOfResults);
	}
	
	
	/*
	 * parseEvaluationList: 
	 * - liest aus Evaluation JSON Dokument die Relevanz dereinzelnen Ergebnisse aus
	 */
	private  Map<String, Integer> parseEvaluationList(String docPath){
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		JSONParser parser = new JSONParser(); 
		 try (FileReader reader = new FileReader(docPath))
	        {
	            //Read JSON file
	            Object obj = parser.parse(reader);
	            
	            
	            JSONArray topics = (JSONArray) obj;
	            System.out.println(topics);
	            //Iterate over topics array
	            for(Object topic : topics) {
	            	JSONObject jsonTopic = (JSONObject) topic;
	            	String docId = (String) jsonTopic.get("document_id");
	            	System.out.println("Relevanz " + jsonTopic.get("relevance"));
	        		int relevance = ((Long)jsonTopic.get("relevance")).intValue();
	        		System.out.println("docId: " + docId + " relevance: " + relevance);
	        		map.put(docId, relevance);
	            }
	            
	        }
		 catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } catch (ParseException e) {
	        e.printStackTrace();
	    }
		return map;
	}
	
	private void logge(String anfrage) throws UnknownHostException {
		PrintWriter writer = null;
		Date date = new Date();
		DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
		DateFormat dt = new SimpleDateFormat("hh:mm:ss");
		String datum = df.format(date);
		String zeit = dt.format(date);
		String ipadress = java.net.InetAddress.getLocalHost().getHostAddress();
		String query = anfrage;
		
		
		File log = new File(datum+".txt");
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(log, true)));
			writer.println(ipadress+", "+query+", "+zeit);
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}
	  

}
