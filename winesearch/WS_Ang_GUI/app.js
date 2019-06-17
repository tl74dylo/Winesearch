var app = angular.module("searchApp", []);
var app2 = angular.module('PopupDemo', ['ui.bootstrap']);
 
app.filter('searchFor', function(){
    // All filters must return a function. The first parameter
    // is the data that is to be filtered, and the second is an
    // argument that may be passed with a colon (searchFor:searchString)
    return function(dataArr, searchString){
 
        if(!searchString){
            return dataArr;
        }
        
        var result = [];
        searchString = searchString.toLowerCase();
 
        angular.forEach(dataArr, function(item){
            if(item.name.toLowerCase().indexOf(searchString) !== -1){
                result.push(item);
            }
        });
        return result;
    };
});
 
function searchController($scope){
    // data model, wird dann über AJAX aufgerufen
    $scope.wines = [
        {name:"Cabernet Sauvignion",color:"red"},
        {name:"Dornfelder",color:"white"},
        {name:"Temperanillo",color:"red"}
    ];}
