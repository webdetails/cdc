/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */


var cdcFunctions = {};

cdcFunctions.activeLevel = 1;
cdcFunctions.jrmIndex = 0;
cdcFunctions.cmIndex = 0;
cdcFunctions.ccIndex = 0;

cdcFunctions.fillMondrianSelector1 = function(){
  Dashboards.update(render_getOlapCatalogs); //trigger call to get available cubes on var
  
  var availableCatalogs = catalogsResult.result.catalogs; 
  var catalogArray = [];
  
  for(var i = 0; i < availableCatalogs.length; i++){
    catalogArray.push([availableCatalogs[i].name,availableCatalogs[i].name]);
  }
  
  return catalogArray;
};


cdcFunctions.fillMondrianSelector2 = function(){
  var selectedSchema = level1Param;

  if(catalogsResult == "") Dashboards.update(render_getOlapCatalogs);
    
  var availableCatalogs = catalogsResult.result.catalogs;
  var cubesArray = [];
  
  for(var i = 0; i < availableCatalogs.length; i++){
    if(availableCatalogs[i].name == selectedSchema){
      for(var j = 0; j < availableCatalogs[i].cubes.length; j++){
	cubesArray.push([availableCatalogs[i].cubes[j].name,availableCatalogs[i].cubes[j].name]);
      }
      break; 
    }
  }
  
  return cubesArray;
};

cdcFunctions.fillMondrianSelector3 = function(){
  Dashboards.update(render_getCubeStructure);
  
  if(cubeStructureResult.result == undefined) return [];
  var availableDimensions = cubeStructureResult.result.dimensions;
  var dimensionsArray = [];
  
  for(var i = 0; i < availableDimensions.length; i++){
    dimensionsArray.push([availableDimensions[i].name,availableDimensions[i].name]);
  } 
  return dimensionsArray;
};



cdcFunctions.expandLevel = function(levelIndex){
  Dashboards.log("Expanding index:"+levelIndex); 
  $("#level"+levelIndex).show();
    
  var objectName = "render_level"+levelIndex;
  var selector = window[objectName+"Selector"];
  var allButton = window[objectName+"AllButton"];
  var refineButton = window[objectName+"RefineButton"];
  var linkButton = window[objectName+"LinkButton"];

  (selector == undefined) ? 0 : Dashboards.update(selector);
  (allButton == undefined) ? 0 : Dashboards.update(allButton);
  (refineButton == undefined) ? 0 : Dashboards.update(refineButton);
  (linkButton == undefined) ? 0 : Dashboards.update(linkButton);
  
  cdcFunctions.activeLevel = levelIndex;
       
};


cdcFunctions.collapseLevel = function(levelIndex){
    Dashboards.log("Collapsing index:"+levelIndex);
    
  
  
  
};



cdcFunctions.generateLink = function(){
    Dashboards.log("Generating Link from index:"+this.activeLevel);
    
    var path = window.location.protocol + window.location.host + "/pentaho/content/ws-run/MondrianCacheCleanService/";
    var operation = "clearSchema?";
   	var parameterList = "catalog="+level1Param;
   
 	if(this.activeLevel == 2){
		operation = "clearCube?"; 	
		parameterList+="&cube="+level2Param;
 	} else if(this.activeLevel == 3){
 		operation = "clearDimension?";
 		parameterList+="&cube="+level2Param + "&dimension=" + level3Param;
 	}
 	
 	Dashboards.fireChange('mondrianCacheCleanLinkParam',path+operation+parameterList);		    
};


cdcFunctions.siteMap = function () {
	debugMode = Dashboards.propertiesArrayToObject( window.location.search.slice(1).split('&').map(function(i){return i.split('=')})).debug;  
    debug = ( debugMode == 'true' ) ? "&debug=true" : "";
    var solution = "CDC";
    
    if(window.location.href.search('pentaho-cdf-dd') > -1){
	  	return [ 
	  		{name: "Home",  id:"home",  link: "Render?solution=" + solution + "&path=&file=cdcHome.wcdf", action:function() {Dashboards.log("Link1");}, sublinks: [] },         
	  		{name: "Settings",  id:"settings",  link:  "Render?solution=" + solution + "&path=&file=cdcSettings.wcdf" + debug, action:function() {Dashboards.log("Link2");}, sublinks: [] },
	  		{name: "Cache Clean",  id:"cacheClean",  link: "Render?solution=" + solution + "&path=&file=cdcCacheClean.wcdf", action:function() {Dashboards.log("Link3");}, sublinks: [] },
	  		{name: "Cache Info",  id:"cacheInfo",  link: "Render?solution=" + solution + "&path=&file=cdcCacheInfo.wcdf" + debug, action:function() {Dashboards.log("Link4");}, sublinks: [] },
	  		{name: "Cluster Info",  id:"clusterInfo",  link: "Render?solution=" + solution + "&path=&file=cdcClusterInfo.wcdf" + debug, action:function() {Dashboards.log("Link5");}, sublinks: [] }
	    	];
	 } else {
		 return [ 
		 	{name: "Home",  id:"home",  link: "home", action:function() {Dashboards.log("Link1");}, sublinks: [] },         
		 	{name: "Settings",  id:"settings",  link:  "settings", action:function() {Dashboards.log("Link2");}, sublinks: [] },
		 	{name: "Cache Clean",  id:"cacheClean",  link: "cacheclean", action:function() {Dashboards.log("Link3");}, sublinks: [] },
		 	{name: "Cache Info",  id:"cacheInfo",  link: "cacheinfo", action:function() {Dashboards.log("Link4");}, sublinks: [] },
		 	{name: "Cluster Info",  id:"clusterInfo",  link: "clusterinfo", action:function() {Dashboards.log("Link5");}, sublinks: [] }
		 	];
	 }
};


/******** home functions *********/

cdcFunctions.isCDAActive = function(){
	Dashboards.update(render_isCDAActive);
	if(resultVar == undefined) return false;
	return resultVar.result;
};

cdcFunctions.isMondrianActive = function(){
	Dashboards.update(render_isMondrianActive);
	if(resultVar == undefined) return false;
	return resultVar.result;
}


cdcFunctions.enableCDACache = function(){
	Dashboards.setParameter('enabled',true);
	Dashboards.update(render_setCDACache);
	if(resultVar != undefined) {
		alert(resultVar.result);
	}
	
};

cdcFunctions.disableCDACache = function(){
	Dashboards.setParameter('enabled',false);
	Dashboards.update(render_setCDACache);
	if(resultVar != undefined) {
		alert(resultVar.result);
	}
};

cdcFunctions.enableMondrianCache = function(){
	Dashboards.setParameter('enabled',true);
	Dashboards.update(render_setMondrianCache);
	if(resultVar != undefined) {
		alert(resultVar.result);
	}
};

cdcFunctions.disableMondrianCache = function(){
	Dashboards.setParameter('enabled',false);
	Dashboards.update(render_setMondrianCache);
	if(resultVar != undefined) {
		alert(resultVar.result);
	}
};


cdcFunctions.getBarColor = function(index){
	

};



/******** cluster info render *********/

cdcFunctions.cloneCacheCompositionChart = function(member, memberResults) {
	var placeHolder = member.find('.cacheComposition'); 
	placeHolder.attr('id',"CC"+this.ccIndex++);
	
	var objectPlaceHolderMap = {
		'cacheCompositionChart': placeHolder.attr('id')
	};
	
	var clone = render_cacheCompositionChart.clone({},{},objectPlaceHolderMap);
	clone.htmlObject = placeHolder.attr('id');
	
	var valuesArray = {};
	var resultset = [];
	var backupMemory = (memberResults.mapInfo.backupMemory / memberResults.javaRuntimeInfo.totalMemory) * 10000;
	resultset.push(['Backup',backupMemory, memberResults.mapInfo.backupCount]);
	var ownedMemory = (memberResults.mapInfo.ownedMemory / memberResults.javaRuntimeInfo.totalMemory) * 10000;
	resultset.push(['Owned',ownedMemory,memberResults.mapInfo.ownedCount]);
	
	valuesArray.resultset = resultset;
	valuesArray.metadata = [];
	
	valuesArray.metadata[0] = { 'colIndex':0, 'colType':"String", 'colName':"Series"};
	valuesArray.metadata[1] = { 'colIndex':1, 'colType':"Numeric", 'colName':"Memory"};
	valuesArray.metadata[2] = { 'colIndex':2, 'colType':"Numeric", 'colName':"Count"};

	
	clone.valuesArray = valuesArray;
	
	return clone;
};

cdcFunctions.cloneCacheMemoryText = function(member, memberResults){
	var placeHolder = member.find('.cacheMemory'); 
	
	var percentage = (memberResults.mapInfo.entryMemory*100/memberResults.javaRuntimeInfo.totalMemory)*100;
	
	placeHolder.text(percentage.toFixed(1));
};

cdcFunctions.cloneJavaRuntimeMemoryChart = function(member, memberResults){
	var placeHolder = member.find('.javaRuntimeMemory'); 
	placeHolder.attr('id',"JRM"+this.jrmIndex++);
	
	var objectPlaceHolderMap = {
		'javaRuntimeMemoryChart': placeHolder.attr('id')
	};
	
	var clone = render_javaRuntimeMemoryChart.clone({},{},objectPlaceHolderMap);
	clone.htmlObject = placeHolder.attr('id');
	
	
	var valuesArray = {};
	var resultset = [];
	resultset.push(['Free Memory',memberResults.javaRuntimeInfo.freeMemory]);
	resultset.push(['Max Memory',memberResults.javaRuntimeInfo.maxMemory]);
	resultset.push(['Total Memory',memberResults.javaRuntimeInfo.totalMemory]);
	
	valuesArray.resultset = resultset;
	valuesArray.metadata = [];
	
	valuesArray.metadata[0] = { 'colIndex':0, 'colType':"String", 'colName':"Memory"};
	valuesArray.metadata[1] = { 'colIndex':1, 'colType':"Numeric", 'colName':"Bites"};

	
	clone.valuesArray = valuesArray;
	
	return clone;
};


cdcFunctions.renderClusterInfo = function(){
	var clone1, clone2, clone3;
	var dummyElement = $("#dummyElement");
	var dummyElementSuperClient = $("#dummyElementSuperClient");
	
	var holder = $("#"+this.htmlObject).empty();
	var results = Dashboards.getParameterValue(this.resultvar);
	
	if(results == "") return;
	if(results.status != "OK") return;
	
	results = results.result;	
			
	var localMemberResults = results.localMember;
	var localMember;
	if(localMemberResults.isSuperClient){
		localMember = dummyElementSuperClient.clone();
		localMember.find('.superClient').text("Super Client");

		
		//set header
		localMember.find('.memberName').text('IP' +localMemberResults.address.substr(1,localMemberResults.address.length));
			
		//java runtime memory
		clone1 = cdcFunctions.cloneJavaRuntimeMemoryChart(localMember, localMemberResults);
		clone1.chartDefinition.width = 700;
		clone1.chartDefinition.barSizeRatio = 1.5;
		clone1.chartDefinition.maxBarSize = 150;
		
		//append member
		localMember.show();
		holder.append(localMember);
		
		Dashboards.update(clone1);
		
		
	} else {
		localMember = dummyElement.clone();
		localMember.find('.superClient').text("");

		
		//set header
		localMember.find('.memberName').text("IP "+localMemberResults.address.substr(1,localMemberResults.address.length));
		
		//cache composition chart
		clone1 = cdcFunctions.cloneCacheCompositionChart(localMember, localMemberResults);
		//cache memory
		cdcFunctions.cloneCacheMemoryText(localMember, localMemberResults);
		//java runtime memory
		clone3 = cdcFunctions.cloneJavaRuntimeMemoryChart(localMember, localMemberResults);
		
		//append member
		localMember.show();
		holder.append(localMember);
		
		$("#"+clone1.htmlObject).css('padding', '0px 30px');
		$("#"+clone3.htmlObject).css('padding', '0px 30px');
		
		Dashboards.update(clone1);
		Dashboards.update(clone3);
	}
		
	for(var i = 0; i < results.otherMembers.length; i++){
		var memberResults = results.otherMembers[i];
		var member; 
			
		if(memberResults.isSuperClient){
			member = dummyElementSuperClient.clone();
			member.find('.superClient').text("Super Client");

			//set header
			member.find('.memberName').append('<span> IP '+memberResults.address.substr(1,memberResults.address.length)+'</span>');
				
			//java runtime memory
			clone1 = cdcFunctions.cloneJavaRuntimeMemoryChart(member, memberResults);
			clone1.chartDefinition.width = 600;
			clone1.chartDefinition.barSizeRatio = 1.5;
			//append member
			member.show();
			holder.append(member);
			
			Dashboards.update(clone1);

			
		} else {
			member = dummyElement.clone();
			member.find('.superClient').text("");

			
			//set header
			member.find('.memberName').text("IP "+memberResults.address.substr(1,memberResults.address.length));
			
			//cache composition chart
			clone1 = cdcFunctions.cloneCacheCompositionChart(member, memberResults);
			//cache memory
			cdcFunctions.cloneCacheMemoryText(member, memberResults);
			//java runtime memory
			clone3 = cdcFunctions.cloneJavaRuntimeMemoryChart(member, memberResults);
			
			//append member
			member.show();
			holder.append(member);
			
			
			$("#"+clone1.htmlObject).css('padding', '0px 30px');
			$("#"+clone3.htmlObject).css('padding', '0px 30px');
			
			Dashboards.update(clone1);
			Dashboards.update(clone3);
		}	
	}

		
};