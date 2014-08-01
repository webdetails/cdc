var cdcFunctions = cdcFunctions || {};

cdcFunctions.getGenericCdcServicesUrl = function(){
  return Dashboards.getWebAppPath() + "/plugin/cdc/api/services/";
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
    params.ts = new Date().getTime();
    var returnValue = "";
    $.ajax({
      url: url,
      type: "GET",
      dataType: 'json',
      async: false,
      data: params,
      success: function(data, textStatus, jqXHR){
          
          if( data == undefined) {
            Dashboards.log("Found error: Empty Data");
            return;
          }
          returnValue = $.parseJSON(data);
          if (!returnValue) {
            returnValue = data;
          }
          return;
    
      },
      error: function (XMLHttpRequest, textStatus, errorThrown) {          
        Dashboards.log("Found error: " + XMLHttpRequest + " - " + textStatus + ", Error: " +  errorThrown,"error");
      }
    }
    );

    return returnValue;

};

cdcFunctions.cubeListing = function(callback) {
  var params = {
    ts: new Date().getTime()
  };
  $.getJSON("olap/getCubes", params ,callback);
};

cdcFunctions.cubeStructure = function(catalog, cube, callback){
  var params = {
    catalog: catalog,
    cube: cube,
    ts: new Date().getTime()
  };
  $.getJSON("olap/getCubeStructure", params, callback);
};

cdcFunctions.memberStructure = function(catalog, cube, member, callback){
  var params = {
    catalog: catalog,
    cube: cube,
    member: member,
    direction: "down",
    ts: new Date().getTime()
  };
  $.getJSON("olap/getLevelMembersStructure", params, callback);
};

cdcFunctions.extractResult = function(response){
  return response.hasOwnProperty("result") ? response.result : response;
};

cdcFunctions.parseResponse = function(response){
  return response.hasOwnProperty("result") ? response : JSON.parse(response);
};