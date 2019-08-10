

<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.lucene.document.Document"%>
<%@page import="java.util.Arrays"%>

<html>
<link rel="stylesheet" type="text/css" href="style.css" media="screen" />
<body>
	
	<% List<Document> results = (ArrayList<Document>)request.getAttribute("results");
	List<String> query = Arrays.asList(((String)request.getAttribute("query")).split(" "));
 	
	out.print("<h1>Results for <i>\"" + String.join(" ", query)+ "\"</i></h1>");
	out.print("<font size=\"1\">" + request.getAttribute("resultCount")+ " results found</font><br><br>");
	int i = 1;
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
        %>
      	
	    
		<div class="segmented-control" style="width: 100%; color: #5FBAAC">
		
			<input type="radio" name="sc-1-<%=i %>" id="sc-1-<%=i %>-1">
		
			<input type="radio" name="sc-1-<%=i %>" id="sc-1-<%=i %>-2" checked>
		
			<input type="radio" name="sc-1-<%=i %>" id="sc-1-<%=i %>-3">
		
		
		  <label for="sc-1-<%=i %>-1" data-value="Relevant">Relevant</label>
		
		  <label for="sc-1-<%=i %>-2" data-value="Nicht Bewertet">Nicht Bewertet</label>
		
		  <label for="sc-1-<%=i %>-3" data-value="Irrelevant">Irrelevant</label>
		
		</div>

		
		<% 
        out.print("<br/>");
        out.print("<br/>");
		i = i+1;
    }
 
%>
	
	
	
	
</body>
</html>
