var cdcFunctions = cdcFunctions || {};

cdcFunctions.getGenericCdcServicesUrl = function(){
	return Dashboards.getWebAppPath() + "/content/ws-run/";
};

cdcFunctions.getMondrianCacheCleanServiceUrl = function(){
	return cdcFunctions.getGenericCdcServicesUrl() + "MondrianCacheCleanService/";
};

cdcFunctions.getHazelcastConfigurationServiceUrl = function(){
	return cdcFunctions.getGenericCdcServicesUrl() + "HazelcastConfigurationService/";
};

cdcFunctions.getHazelcastMonitorServiceUrl = function(){
	return cdcFunctions.getGenericCdcServicesUrl() + "HazelcastMonitorService/";
};

cdcFunctions.getDashboardCacheCleanServiceUrl = function(){
	return cdcFunctions.getGenericCdcServicesUrl() + "DashboardCacheCleanService/";
};

cdcFunctions.makeRequest = function (url, params) {
    var returnValue = "";
    $.ajax({
      url: url,
      type: "GET",
      dataType: 'xml',
      async: false,
      data: params,
      complete: function (XMLHttpRequest, textStatus) {
                  
         var values = XMLHttpRequest.responseText;
          var changedValues = undefined;
          
          if(values == undefined) {
            Dashboards.log("Found error: Empty Data");
            return;
          }
 
          if(this.dataType == "xml" || this.dataType == "html"){
            var xmlDoc;
            try { //Firefox, Mozilla, Opera, etc.
                parser=new DOMParser();
                xmlDoc=parser.parseFromString(values,"text/xml");

            } catch(e){
                try { //Internet Explorer
                    xmlDoc=new ActiveXObject("Microsoft.XMLDOM");
                    xmlDoc.async="false";
                    xmlDoc.loadXML(values);
                    values = xmlDoc;
                } catch(e) {
                    Dashboards.log('XML is invalid or no XML parser found');
                }
            }
            returnValue=xmlDoc;

            var nodeList = returnValue.getElementsByTagName('return');
            if( nodeList.length > 0 && nodeList[0].firstChild ) {
              returnValue = nodeList[0].firstChild.nodeValue;
            }
            else return;
            
            returnValue = $.parseJSON(returnValue);
          } else if(this.dataType == "json") {
                  returnValue = $.parseJSON(returnValue);
          } else if(this.dataType != "script" && this.dataType != "text"){
                  Dashboards.log("Found error: Unknown returned format");
            return;
          }
          
          if (changedValues != undefined){
            returnValue = changedValues;
          }
    
      },
      error: function (XMLHttpRequest, textStatus, errorThrown) {          
        Dashboards.log("Found error: " + XMLHttpRequest + " - " + textStatus + ", Error: " +  errorThrown,"error");
      }
    }
    );

    return returnValue;
  };

  cdcFunctions.cubeListing = function(callback) {
    $.getJSON("OlapUtils", {
        operation:"GetOlapCubes"
      },callback);
  };

  cdcFunctions.cubeStructure = function(catalog, cube, callback){
    $.getJSON("OlapUtils", {
        operation:"GetCubeStructure",
        catalog: catalog,
        cube: cube
      }, callback);
  };

  cdcFunctions.memberStructure = function(catalog, cube, member, callback){
    $.getJSON("OlapUtils", {
        operation:"GetLevelMembersStructure",
        catalog: catalog,
        cube: cube,
        member: member,
        direction: "down"
      }, callback);
  };

  cdcFunctions.extractResult = function(response){
    return response;
  };

  cdcFunctions.parseResponse = function(response){
    return JSON.parse($("return",response).text());
  };