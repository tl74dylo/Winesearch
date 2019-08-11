package servlet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/**
 * Servlet implementation class EvaluationServlet
 */
public class EvaluationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Map<String, String> topicMap = new HashMap<String, String>();
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public EvaluationServlet() {
        super();
        topicMap = parseTopicList();
        System.out.println(topicMap.get("dry red"));
        
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//response.getWriter().append("Served at: ").append(request.getContextPath());
		saveEvaluation(request);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	
	/**
	 * Speichert fuer die Query, die Relevanz der einzelnen Ergebnisse in einem JSON- File 
	 * Output "Eval_(query)" alle Leerzeichen in Query durch '_' ersetzt 
	 * query: "dry red" -> File: "Eval_dry_red"
	 * @param request der in JSON Datei gespeicher werden soll
	 */
	private void saveEvaluation(HttpServletRequest request) {
		String query = request.getParameter("query").replaceAll("[\\[,\\]]","");
		String topicId = topicMap.get(query);
		System.out.println(topicMap);
		
		FileWriter fileWriter;
		JSONArray jsonArray = new JSONArray();
		try {
			fileWriter = new FileWriter("Eval_"+ query.replaceAll(" ", "_")+".json");
			System.out.println("File erstellt");
			
		    for(int i = 1; i<=50;i++) {
		    	JSONObject obj = new JSONObject();
		    	obj.put("topic_id", topicId);
		    	obj.put("document_id", request.getParameter("sc-1-"+i).split(";")[0]);
		    	int relevance = 0;
		    	try {
		    		relevance = Integer.parseInt( request.getParameter("sc-1-"+i).split(";")[1]);
		    	}
		    	catch(NumberFormatException e) {
		    		e.printStackTrace();
		    		System.out.println("Fehler beim Auslesen der Relevanz: Relevanz muss eine Zahl sein!");
		    	}
		    	catch(Exception e) {
		    		e.printStackTrace();
		    	}
		    	obj.put("relevance", relevance);
		    	jsonArray.add(obj);
		    	
		    }
		    fileWriter.write(jsonArray.toString());
		    fileWriter.flush();
		    fileWriter.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		finally {
			
		}
	}
	
	
	/**
	 * Liest die Topic IDs aus topic.json aus und erstellt eine Map mit jeder Id fuer jede gegebene Query
	 * @return Query-Id Map 
	 */
	private  Map<String, String> parseTopicList(){
		HashMap<String, String> map = new HashMap<String, String>();
		JSONParser parser = new JSONParser(); 
		 try (FileReader reader = new FileReader("topics.json"))
	        {
	            //Read JSON file
	            Object obj = parser.parse(reader);
	            
	            
	            JSONArray topics = (JSONArray) obj;
	            System.out.println(topics);
	            //Iterate over topics array
	            for(Object topic : topics) {
	            	JSONObject jsonTopic = (JSONObject) topic;
	            	String topicId = (String) jsonTopic.get("topic_id");
	        		String query = (String) jsonTopic.get("query");
	        		System.out.println("topic_id: " + topicId + " query: " + query);
	        		map.put(query, topicId);
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
	

	

}
