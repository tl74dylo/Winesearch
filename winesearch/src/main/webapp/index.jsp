<html>
<body>
<h1>Welcome to WineSearch</h1>
	<form action="QueryServlet" method="post">
		Enter your Query: <input type="text" name="query" size="20">
		<br>
		Evaluation mode <input type="checkbox" name="eval" id="eval" 
                    value=1><br> 
		<input type="submit" value="Submit" />
	</form>
	
	<h3>Indexing:</h3>
	<form action="IndexServlet" method="post"><input type="submit" value="Index files" name="index" size="30" />
	<output="text">Note: this may take a while.</output>
</body>
</html>