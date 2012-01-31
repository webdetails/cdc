var cdcFunctions = {};

cdcFunctions.fillMondrianSelector1 = function(){
  Dashboards.update(render_getOlapCatalogs); //trigger call to get available cubes on var
  
  var availableCatalogs = catalogsResult.result.catalogs; 
  var catalogArray = [];
  
  for(var i = 0; i < availableCatalogs.length; i++){
    catalogArray.push([availableCatalogs[i].name]);
  }
  
  return catalogArray;
};


cdcFunctions.fillMondrianSelector2 = function(){
  var selectedSchema = mondrianLevel1Param;

  if(catalogsResult == "") Dashboards.update(render_getOlapCatalogs);
    
  var availableCatalogs = catalogsResult.result.catalogs;
  var cubesArray = [];
  
  for(var i = 0; i < availableCatalogs.length; i++){
    if(availableCatalogs[i].name == selectedSchema){
      for(var j = 0; j < availableCatalogs[i].cubes.length; j++){
	cubesArray.push([availableCatalogs[i].cubes[j].name]);
      }
      break; 
    }
  }
  
  return cubesArray;
};

cdcFunctions.fillMondrianSelector3 = function(){
  Dashboards.update(render_getCubeStructure);
  
  var availableDimensions = cubeStructureResult.result.dimensions;
  var dimensionsArray = [];
  
  for(var i = 0; i < availableDimensions.length; i++){
    dimensionsArray.push([availableDimensions[i].name]);
  } 
  return dimensionsArray;
};



cdcFunctions.expandLevel = function(levelIndex){
  Dashboards.log("Expanding index:"+levelIndex); 
    
  var objectName = "render_mondrianLevel"+levelIndex;
  var selector = window[objectName+"Selector"];
  var allButton = window[objectName+"AllButton"];
  var refineButton = window[objectName+"RefineButton"];
  var linkButton = window[objectName+"LinkButton"];

  (selector == undefined) ? 0 : Dashboards.update(selector);
  (allButton == undefined) ? 0 : Dashboards.update(allButton);
  (refineButton == undefined) ? 0 : Dashboards.update(refineButton);
  (linkButton == undefined) ? 0 : Dashboards.update(linkButton);
       
};


cdcFunctions.collapseLevel = function(levelIndex){
    Dashboards.log("Collapsing index:"+levelIndex);
  
  
  
};



cdcFunctions.generateLink = function(levelIndex){
  Dashboards.log("Generating Link from index:"+levelIndex);
  
  
};


