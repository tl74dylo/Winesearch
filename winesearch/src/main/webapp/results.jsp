<header>

</header>

<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.lucene.document.Document"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.HashMap"%>

<html>
<link rel="stylesheet" type="text/css" href="style.css" media="screen" />
<body>
	
	<% 
	List<Document> results = (ArrayList<Document>)request.getAttribute("results");
	
	String queryString = (String)request.getAttribute("query");
	List<String> query = Arrays.asList((queryString).split(" "));
 	
	out.print("<h1>Results for <i>\"" + queryString+ "\"</i></h1>");
	out.print("<font size=\"1\">" + request.getAttribute("resultCount")+ " results found</font><br><br>");
	int i = 1;
	boolean eval = (boolean)request.getAttribute("eval");
	System.out.println("Eval: " + eval);
	
	HashMap<String, Integer> control = (HashMap<String, Integer>)request.getAttribute("control");
	System.out.println("control " + control);
	
	%>
	<form id="EvalForm" action="EvaluationServlet" method="post">
	<% 
    for(Document result : results)
    {
    	String documentId = result.get("id");
        out.print("Id: " + documentId);
        
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
        // Zeige Segmented-Control nur im Evaluierungsmodus an
       
        if(eval){
        	String checked1 = "";
        	String checked0 = "";
        	String checkedNeg1 = "";
        	if(control != null){
        		switch(control.get(documentId)){
        			case 1: 
        				checked1 = "checked";
        				break;
        			case -1:
        				checkedNeg1 = "checked";
        				break;
        			default:
        				checked0 = "checked";
        		}
        	}
        	else{
        		checked0 = "checked";
        	}
        	
        	
        	 
        
        %>
      	
	    
		<div class="segmented-control" style="width: 100%; color: #5FBAAC">
		
			<input type="radio" name="sc-1-<%=i %>" id="sc-1-<%=i %>-1" value="<%=result.get("id")%>;1" <%=checked1 %>>
		
			<input type="radio" name="sc-1-<%=i %>" id="sc-1-<%=i %>-2" value="<%=result.get("id")%>;0" <%=checked0 %>>
		
			<input type="radio" name="sc-1-<%=i %>" id="sc-1-<%=i %>-3" value="<%=result.get("id")%>;-1"<%=checkedNeg1 %>>
		
		
		  <label for="sc-1-<%=i %>-1" data-value="Relevant">Relevant</label>
		
		  <label for="sc-1-<%=i %>-2" data-value="Nicht Bewertet">Nicht Bewertet</label>
		
		  <label for="sc-1-<%=i %>-3" data-value="Irrelevant">Irrelevant</label>
		
		</div>

		
		<% 
        }
        out.print("<br/>");
        out.print("<br/>");
		i = i+1;
    }
 
%>
</form>


<script src="https://code.jquery.com/jquery-1.10.2.js"
	type="text/javascript"></script>
<script type="text/javascript">

$(document).ready(function() {
    $('#EvalForm').submit(function(event) {
    var values = $('#EvalForm').serialize().concat("&query=<%=queryString%>");
    console.log(values);

    $.ajax({
     type: 'POST',
     url: 'EvaluationServlet',
     data: values,
     encode: true
    })
    .done(function(data){
    	console.log(data);
    });
    event.preventDefault();
    
	});
});
</script>	

<script type="text/javascript">
function autoSubmit(){
	if(<%=eval%>){
		$('#EvalForm').submit();
	}
	else{
		console.log("Kein autoSubmit")
	}
	
}
setInterval("autoSubmit()",5000);
</script>
	
	
</body>
</html>
