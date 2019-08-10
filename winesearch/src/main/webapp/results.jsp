

<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.lucene.document.Document"%>
<%@page import="java.util.Arrays"%>
<html>
<body>
	
	<% List<Document> results = (ArrayList<Document>)request.getAttribute("results");
	List<String> query = Arrays.asList(((String)request.getAttribute("query")).split(" "));
 	
	out.print("<h1>Results for <i>\"" + String.join(" ", query)+ "\"</i></h1>");
	out.print("<font size=\"1\">" + request.getAttribute("resultCount")+ " results found</font><br><br>");
    for(Document result : results)
    {
        out.print("Id: " + result.get("id"));
        out.print("<br/>");
        out.print("Title: " + result.get("title"));
        out.print("<br/>");
        for(String word : result.get("description").split(" ")){
        	if(query.contains(word)){
        		out.print("<b>" + word + "</b>");
        	}
        	else{
        		out.print(word);
        	}
        	out.print(" ");
        }
        
        out.print("<br/>");
        out.print("<br/>");
    }
 
%>
	
	
	
	
</body>
</html>