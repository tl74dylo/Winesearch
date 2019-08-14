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
	private int best = 0;
	private int price = 0;
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
		String query = request.getParameter("query").toLowerCase();
		String queryneu = null;
		String evalMode = request.getParameter("eval");
		boolean eval = ((evalMode!= null) ? true: false);

		
		//PrintWriter writer = response.getWriter();
		logge(query);
		System.out.println("geloggt");
		if (eval == true) {
		queryneu = check(query);
		} else {
			queryneu = query;
		}
		System.out.println("query: "+query);
		 ArrayList<Document> resultsList = new ArrayList<Document>();
		try {
			//resultsList = searchIndexedFiles(writer, query);
			resultsList = searchIndexedFiles(queryneu);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		Map <String, Integer> map = setupRequest(request, query, eval);
		if(price == 1) {
			sortcheap(resultsList);
		} else if (price==2) {
			sortpricey(resultsList);
		}
		if(best == 1 && eval == true) {
			sortbest(resultsList);
		} else if (best == 2 && eval == true) {
			sortworst(resultsList);
		}
		sort(resultsList, map);
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
		   
		int count = (hits.length >= 50) ? 50 : hits.length;
		   for (int i= 0; i< count; i++) {
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

		   
		   return resultsList;
		   
		  }
	
	
	/* setupRequest
	 * - setzt Attribute fuer request 
	 * - abhÃ¤ngig ob Evaluierungsmodus aktiv ist 
	 * - liest vorhandenes JSON Evaluierungsfile fuer Query aus, wenn vorhanden
	 */
	
	private Map <String, Integer> setupRequest(HttpServletRequest request, String query, boolean eval) {
		boolean fileExists = false;
		Map <String, Integer> relevance = null;
		// Pruefe im Evaluierungsmodus, ob es fuer diese Query schon ein passendes JSON Dokument gibt
		if(eval){
			String fileName = "Eval_" + query.replaceAll(" ", "_") + ".json";
			fileExists = (new File(fileName)).exists();
			
			if(fileExists){
				relevance = parseEvaluationList(fileName);
				System.out.println("File exists");
				request.setAttribute("control", relevance);
			}
		}
		
		request.setAttribute("query", query);
		request.setAttribute("eval", eval);
		request.setAttribute("resultCount", numOfResults);
		return relevance;
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
	
	private String check(String query) {	//Vorverarbeitungsfunktion fuer Queries		
		checkprice(query);
		String queryneu = checkfrom(query);
		queryneu = checkvintage(queryneu);
		queryneu = checktype(queryneu);
		queryneu = checkbest(queryneu);
		queryneu = checkcountry(queryneu);
		queryneu = checktaste(queryneu);
		queryneu = checkfood(queryneu);
		queryneu = checkrecommend(queryneu);
		
		System.out.println(query +" (original)");	
		System.out.println(queryneu+" (neu)");
		return queryneu;
		
	}
	
	private String checkfrom(String query) {
		String [] queryarr = query.split(" ");
		StringBuilder sb = new StringBuilder();
		sb.append(query);
		int test = 0;
		int jahr = 0;
		for (int i=0; i<queryarr.length; i++) {
			try  {
			jahr = Integer.parseInt(queryarr[i+1]);
			} catch (NumberFormatException nfe) {
				
			} catch (ArrayIndexOutOfBoundsException aie) {
				
			}
			if (jahr > 1900 && jahr < 2100) {
				test = 1;
			}
			if (queryarr[i].contains("from") && test == 1) {
				sb.append(" vintage");
				return sb.toString();
			}
		}
		
		
		return sb.toString();
	}
	
	//Vorverarbeitung fuer recommendations
	
	private String checkrecommend(String query) {
		Date date = new Date();
		DateFormat df = new SimpleDateFormat("yyyy");
		String jahr = df.format(date);
		int recommendations = 0;
		String [] queryarr = query.split(" ");
		StringBuilder sb = new StringBuilder();
		sb.append(query);
		for (int i=0; i<queryarr.length; i++) {
			if(recommendations == 0 && queryarr[i].contains("recommend")) {
				sb.append(" description:'drin from' description:'drin in' description:'drink now' description:'drink best after'"+" "+jahr);
				recommendations = 1;
			}
		}
		return sb.toString();
	}
	
		//checks if vintage is requested
	private String checkvintage(String query) {
		String [] queryarr = query.split(" ");
		StringBuilder sb = new StringBuilder();
			int vintage = 0;					//Kenngroesse fuer doppelvorkommen von woertern
				if (queryarr.length>1) {		//iteriert über jedes Wort der Query, falls query laenger als 1 (da Vintage ein Jahr braucht)
					for (int j = 0; j<queryarr.length; j++) {
					/*if (queryarr[queryarr.length-1].equals("vintage") && vintage == 0){	//check ob das letzte Wort == vintage und Kenngroesse noch 0
						for (int k = 0; k<(queryarr.length-1); k++) {
						
							try {
								int zahl = Integer.parseInt(queryarr[k]);	//falls wort == zahl, erfolg
								if (zahl > 1900 && zahl < 2100) {	
								sb.append("title:");						//falls im Zeitraum, neue Query einfuegen
								sb.append(queryarr[k]+" ");
							}
							} catch (NumberFormatException nfe) {			//falls keine Zahl, wort zur neuen Query hinzufuegen
								sb.append(queryarr[k]+" ");
							}
								
						}
						vintage = 1;
					} else */ if (vintage != 1 && queryarr[j].equals("vintage")) {
						for (int l = 0; l<j; l++) {
							try {
								int zahl = Integer.parseInt(queryarr[l]);
								if (zahl > 1900 && zahl < 2100) {
								sb.append("title:");
								sb.append(queryarr[l]+" ");
								}
							} catch (NumberFormatException nfe) {
								sb.append(queryarr[l]+" ");
							}
						}
						sb.append("vintage ");
						for(int l = j+1; l<queryarr.length; l++) {
							try {
								int zahl = Integer.parseInt(queryarr[l]);
								if (zahl > 1900 && zahl < 2100) {
									sb.append("title:");
									sb.append(queryarr[l]+" ");
									}
							} catch (NumberFormatException nfe) {
								sb.append(queryarr[l]+" ");
							}
						}
						vintage = 1;
						 }
					
					}
						
				} 
				if (sb.length() == 0){
				sb.append(query);
				}
				return sb.toString();
	}
		
		
		//checks if only red or white wine is requested
	private String checktype(String query) {
		StringBuilder sb = new StringBuilder();
		sb.append(query);
		String [] queryarr = query.split(" ");
		int type = 0;
		for (int i=0; i<queryarr.length; i++) {
			if (queryarr[i].equals("red") || queryarr[i].equals("rouge")) {
				if (type != 1) {
				type += 1;
				}
			} else if (queryarr[i].equals("blanc") || queryarr[i].equals("white")) {
				if (type != 2) {
					type += 2;
				}
			}
			
		}
		
		if (type == 1) {
			sb.append(" -blanc");
			sb.append(" -white");
		} else if (type == 2 ){
			sb.append(" -red");
			sb.append(" -rouge");
		}
		
		String queryneu = sb.toString();
		return queryneu;
	}
		
	
	//country check, doesn't check every country, but a fair amount
		
	private String checkcountry(String query) {
		StringBuilder sb = new StringBuilder();
		sb.append(query);
		String queryarr[] = query.split(" ");
		for (int i=0; i<queryarr.length; i++) {
			switch(queryarr[i]) {
				case "france":
					sb.append(" +country:france");
					break;
				case "french":
					sb.append(" +country:france");
					break;
				case "germany":
					sb.append(" +country:germany");
					break;
				case "german":
					sb.append(" +country:germany");
					break;
				case "america":
					sb.append(" +country:us");
					break;
				case "american":
					sb.append(" +country:us");
					break;
				case "us":
					sb.append(" +country:us");
					break;
				case "italy":
					sb.append(" +country:italy");
					break;
				case "italian":
					sb.append(" +country:italy");
					break;
				case "greece":
					sb.append(" +country:greece");
					break;
				case "greek":
					sb.append(" +country:greece");
					break;
				case "spain":
					sb.append(" +country:spain");
					break;
				case "spanish":
					sb.append(" +country:spain");
					break;
				case "mexico":
					sb.append(" +country:mexico");
					break;
				case "mexican":
					sb.append(" +country:mexico");
					break;
				case "australia":
					sb.append(" +country:australia");
					break;
				case "australian":
					sb.append(" +country:australia");
					break;
				case "austria":
					sb.append(" +country:austria");
					break;
				case "austrian":
					sb.append(" +country:austria");
					break;
				case "portugal":
					sb.append(" +country:portugal");
					break;
				case "portuguese":
					sb.append(" +country:portugal");
					break;
				case "argentina":
					sb.append(" +country:argentina");
					break;
				case "argentinean":
					sb.append(" +country:argentina");
					break;
				case "romania":
					sb.append(" +country:romania");
					break;
				case "romanian":
					sb.append(" +country:romania");
					break;
				case "england":
					sb.append(" +country:england");
					break;
				case "english":
					sb.append(" +country:england");
					break;
				case "georgia":
					sb.append(" +country:georgia");
					break;
				case "georgian":
					sb.append(" +country:georgia");
					break;
				case "canada":
					sb.append(" +country:canada");
					break;
				case "canadian":
					sb.append(" +country:canada");
					break;
				case "zealand":
					sb.append(" +country:zealand");
					break;
				case "africa":
					sb.append(" +country:africa");
					break;
				case "african":
					sb.append(" +country:africa");
					break;
				case "israel":
					sb.append(" +country:israel");
					break;
				case "chile":
					sb.append(" +country:chile");
					break;
				case "chilean":
					sb.append(" +country:chile");
					break;
				case "bulgaria":
					sb.append(" +country:bulgaria");
					break;
				case "bulgarian":
					sb.append(" +country:bulgaria");
					break;
				case "moldova":
					sb.append(" +country:moldova");
					break;
				case "moldavian":
					sb.append(" +country:moldova");
					break;
				case "slovenia":
					sb.append(" +country:slovenia");
					break;
				case "slovenian":
					sb.append(" +country:slovenia");
					break;
				case "hungary":
					sb.append(" +country:hungary");
					break;
				case "hungarian":
					sb.append(" +country:hungary");
					break;
				case "cyprus":
					sb.append(" +country:cyprus");
					break;
				default:
					break;
			}
				
		}
		
		return sb.toString();
	}
	
	//prueft zu welchem essen der wein passt
	
	private String checkfood(String query) {
		StringBuilder sb = new StringBuilder();
		sb.append(query);
		String queryarr[] = query.split(" ");
		for (int i=0; i<queryarr.length; i++) {
			switch(queryarr[i]) {
				case "seafood":
					sb.append(" +description:seafood");
					break;
				case "pasta":
					sb.append(" +description:pasta");
					break;
				case "venison":
					sb.append(" +description:venison");
					break;
				case "cheese":
					sb.append(" +description:cheese");
					break;
				case "clams":
					sb.append(" +description:clams");
					break;
				case "tempranillo":
					sb.append(" +description:tempranillo");
					break;
				case "meat":
					sb.append(" +description:meat");
					break;
				case "organic":
					sb.append(" +description:organic");
					break;
				default:
					break;
						
			}
		}
		return sb.toString();
	}
	
	//prueft den Geschmack und die textur
	
	private String checktaste(String query) {
		StringBuilder sb = new StringBuilder();
		sb.append(query);
		String queryarr[] = query.split(" ");
		for (int i=0; i<queryarr.length; i++) {
			switch(queryarr[i]) {
				case "sweet":
					sb.append(" +description:sweet");
					break;
				case "fruity":
					sb.append(" +description:fruity");
					break;
				case "dry":
					sb.append(" +description:dry");
					break;
				case "berry":
					sb.append(" +description:berry");
					break;
				case "cherry":
					sb.append(" +description:cherry");
					break;
				case "citrus":
					sb.append(" +description:citrus");
					break;
				case "smooth":
					sb.append(" +description:smoth");
					break;
				case "rough":
					sb.append(" +description:rough");
					break;
				default:
					break;
			}
		}
		
		return sb.toString();
	}
	
	//checkt ob teuer/billig
	private void checkprice (String query) {
		price = 0;
		String queryarr[] = query.split(" ");
		for (int i=0; i<queryarr.length; i++) {
			if (queryarr[i].contains("cheap")) {
				price = 1;
				break;
			} else if (queryarr[i].contains("expensive") || queryarr[i].contains("pricey")) {
				price = 2;
				break;
			}
		}
	}
		
		
		//prüft ob best vorkommt
	
	private String checkbest (String query) {
		best = 0;
		StringBuilder sb = new StringBuilder();
		String queryarr[] = query.split(" ");
		for (int i=0; i<queryarr.length; i++) {
			if (queryarr[i].equals("best") || queryarr[i].equals("good")) {
			best = 1;
		} else if (queryarr[i].equals("worst") || queryarr[i].equals("bad")) {
			best = 2;
		} else {
			sb.append(queryarr[i]+" ");
			}
		}	
		String queryneu = sb.toString();
		return queryneu;
	}
	
	private void sort(ArrayList<Document> resultList, Map<String, Integer> map){
		HashMap<String, Integer> hashmap = (HashMap<String, Integer>)map;
		int tausch = 0;
		do {
			tausch = 0;
		try {
		for (int i= 0; i<=100;i++){
		if (hashmap.get(resultList.get(i).get("id")) < hashmap.get(resultList.get(i+1).get("id"))) {
			Document temp = resultList.get(i);
			resultList.set(i, resultList.get(i+1));
			resultList.set((i+1), temp);
			tausch = 1;
				}
			}
		} catch (IndexOutOfBoundsException ioe) {
			
		} catch (NullPointerException npe) {
			
		}
		} while (tausch == 1);
	}
	
	//sortiert nach Punkten
	
	private void sortbest(ArrayList<Document> resultList) {
		int tausch = 0;
		double c = 0.0;
		double d = 0.0;
		do {
			 tausch = 0;
		try {
			for (int i= 0; i<=100;i++){
				int a = Integer.parseInt(resultList.get(i).get("points"));
				int b = Integer.parseInt(resultList.get(i+1).get("points"));
				try {
					 c = Double.parseDouble(resultList.get(i).get("price"));
					} catch (NumberFormatException nfe) {
						c = 0.0;
					}
					try {
					 d = Double.parseDouble(resultList.get(i+1).get("price"));
					} catch (NumberFormatException nfe) {
						d = 0.0;
					}
				
					if (price == 1 && a<b && c<d){
						
					} else if (price == 2 && a<b && c>d) {
						
					} else if (a<b) {
					Document temp = resultList.get(i);
					resultList.set(i, resultList.get(i+1));
					resultList.set((i+1), temp);
					tausch = 1;
						}
				}
			
		} catch (IndexOutOfBoundsException ioe) {
			
		} catch (NullPointerException npe) {
			
		;}
	} while (tausch == 1);
	
	}  
	
	//sortiert nach schlechtem Wein
	private void sortworst(ArrayList<Document> resultList) {
		int tausch = 0;
		double c = 0.0;
		double d = 0.0;
		do {
			 tausch = 0;
		try {
			for (int i= 0; i<=100;i++){
				int a = Integer.parseInt(resultList.get(i).get("points"));
				int b = Integer.parseInt(resultList.get(i+1).get("points"));
				try {
					 c = Double.parseDouble(resultList.get(i).get("price"));
					} catch (NumberFormatException nfe) {
						c = 0.0;
					}
					try {
					 d = Double.parseDouble(resultList.get(i+1).get("price"));
					} catch (NumberFormatException nfe) {
						d = 0.0;
					}
				
					if (price == 1 && a>b && c<d){
						
					} else if (price == 2 && a>b && c>d) {
						
					} else if (a>b) {
					Document temp = resultList.get(i);
					resultList.set(i, resultList.get(i+1));
					resultList.set((i+1), temp);
					tausch = 1;
						}
				}
			
		} catch (IndexOutOfBoundsException ioe) {
			
		} catch (NullPointerException npe) {
			
		;}
	} while (tausch == 1);
	
	}
	
	//sortiert nach preiswert
	
	private void sortcheap(ArrayList<Document> resultList) {
		int tausch = 0;
		double a = 0.0;
		double b = 0.0;
		do {
			 tausch = 0;
		try {
			for (int i= 0; i<=100;i++){
				try {
				 a = Double.parseDouble(resultList.get(i).get("price"));
				} catch (NumberFormatException nfe) {
					a = 0.0;
				}
				try {
				 b = Double.parseDouble(resultList.get(i+1).get("price"));
				} catch (NumberFormatException nfe) {
					b = 0.0;
				}
				if(a>b) {
					Document temp = resultList.get(i);
					resultList.set(i, resultList.get(i+1));
					resultList.set((i+1), temp);
					tausch = 1;
						}
				}
			
		} catch (IndexOutOfBoundsException ioe) {
			
		} catch (NullPointerException npe) {
			
		}
	} while (tausch == 1);
	}
	
	//sortiert nach teuer
	
	private void sortpricey(ArrayList<Document> resultList) {
		int tausch = 0;
		double a = 0.0;
		double b = 0.0;
		do {
			 tausch = 0;
		try {
			for (int i= 0; i<=100;i++){
				try {
					 a = Double.parseDouble(resultList.get(i).get("price"));
					} catch (NumberFormatException nfe) {
						a = 0.0;
					}
					try {
					 b = Double.parseDouble(resultList.get(i+1).get("price"));
					} catch (NumberFormatException nfe) {
						b = 0.0;
					}
				if(a<b) {
					Document temp = resultList.get(i);
					resultList.set(i, resultList.get(i+1));
					resultList.set((i+1), temp);
					tausch = 1;
						}
				}
			
		} catch (IndexOutOfBoundsException ioe) {
			
		} catch (NullPointerException npe) {
			
		;}
	} while (tausch == 1);
	}
	  

}
