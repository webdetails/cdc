/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */


var cdcFunctions = {};

cdcFunctions.activeLevel = 1;
cdcFunctions.jrmIndex = 0;
cdcFunctions.cmIndex = 0;
cdcFunctions.ccIndex = 0;




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
  
  return [ 
    {name: "Home",  id:"home",  link: cdcFunctions.getDashboardLink("home"), action:function() {Dashboards.log("Link1");}, sublinks: [] },         
    {name: "Settings",  id:"settings",  link:  cdcFunctions.getDashboardLink("settings"), action:function() {Dashboards.log("Link2");}, sublinks: [] },
    {name: "Cache Clean",  id:"cacheClean",  link: cdcFunctions.getDashboardLink("cacheClean"), action:function() {Dashboards.log("Link3");}, sublinks: [] },
    {name: "Cluster Info",  id:"clusterInfo",  link: cdcFunctions.getDashboardLink("clusterInfo"), action:function() {Dashboards.log("Link5");}, sublinks: [] },
    {name: "About",  id:"about", classes: "rightMenuItem",  link: cdcFunctions.getDashboardLink("about"), action:function() {Dashboards.log("Link6");}, sublinks: [] }
    ];
/*
  debugMode = Dashboards.propertiesArrayToObject( window.location.search.slice(1).split('&').map(function(i){return i.split('=')})).debug;  
  debug = ( debugMode == 'true' ) ? "&debug=true" : "";
  var solution = "CDC";
  
  if(window.location.href.search('pentaho-cdf-dd') > -1){
    return [ 
      {name: "Home",  id:"home",  link: "Render?solution=" + solution + "&path=&file=cdcHome.wcdf", action:function() {Dashboards.log("Link1");}, sublinks: [] },         
      {name: "Settings",  id:"settings",  link:  "Render?solution=" + solution + "&path=&file=cdcSettings.wcdf" + debug, action:function() {Dashboards.log("Link2");}, sublinks: [] },
      {name: "Cache Clean",  id:"cacheClean",  link: "Render?solution=" + solution + "&path=&file=cdcCacheClean.wcdf", action:function() {Dashboards.log("Link3");}, sublinks: [] },
      {name: "Cluster Info",  id:"clusterInfo",  link: "Render?solution=" + solution + "&path=&file=cdcClusterInfo.wcdf" + debug, action:function() {Dashboards.log("Link5");}, sublinks: [] },
      {name: "About",  id:"about", classes: "rightMenuItem",  link: "Render?solution=" + solution + "&path=&file=cdcAbout.wcdf" + debug, action:function() {Dashboards.log("Link6");}, sublinks: [] },
      ];
  } else {
   return [ 
    {name: "Home",  id:"home",  link: "home", action:function() {Dashboards.log("Link1");}, sublinks: [] },         
    {name: "Settings",  id:"settings",  link:  "settings", action:function() {Dashboards.log("Link2");}, sublinks: [] },
    {name: "Cache Clean",  id:"cacheClean",  link: "cacheclean", action:function() {Dashboards.log("Link3");}, sublinks: [] },
    {name: "Cluster Info",  id:"clusterInfo",  link: "clusterinfo", action:function() {Dashboards.log("Link5");}, sublinks: [] },
    {name: "About",  id:"about", classes: "rightMenuItem",  link: "about", action:function() {Dashboards.log("Link6");}, sublinks: [] }
    ];
  }
  * */
};

cdcFunctions.getDashboardLink = function(pageName){
  
  if(window.location.href.search('/pentaho-cdf-dd/') > -1) { //dev mode  
    var debugMode = Dashboards.propertiesArrayToObject( window.location.search.slice(1).split('&').map(function(i){return i.split('=')})).debug;  
    var debug = ( debugMode == 'true' ) ? "&debug=true" : "";
    var solution = "CDC";
    
    var devName = 'cdc' + pageName.charAt(0).toUpperCase() + pageName.slice(1);
    return 'Render?solution=' + solution + '&path=&file=' + devName + '.wcdf' + debug;
    
  }
  else {
    return pageName;
  }
};


/******** home functions *********/


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


}

cdcFunctions.getDefinitionRequest = function (map, name) {
    return cdcFunctions.makeRequest('../ws-run/HazelcastConfigurationService/getMapOption',  {map: map, name: name});
};

cdcFunctions.setDefinitionRequest = function(map, name, value) {
    return cdcFunctions.makeRequest('../ws-run/HazelcastConfigurationService/setMapOption',  {map: map, name: name, value: value});

};

cdcFunctions.isRunningFallbackCall = function () {

    return cdcFunctions.makeRequest('../ws-run/HazelcastMonitorService/isRunningFallback',  {});
}

cdcFunctions.isCDAActive = function(){
	Dashboards.setParameter('map','cda');
    var result = cdcFunctions.getDefinitionRequest('cda', 'enabled');
	Dashboards.setParameter('resultVar', result);
	if(resultVar == undefined) return false;
	return resultVar.result;
};

cdcFunctions.isMondrianActive = function(){
	Dashboards.setParameter('map','mondrian');
    var result = cdcFunctions.getDefinitionRequest('mondrian', 'enabled');
	Dashboards.setParameter('resultVar', result);
	if(resultVar == undefined) return false;
	return resultVar.result;
};


cdcFunctions.isRunningFallback = function(){
	var result = cdcFunctions.isRunningFallbackCall();
    Dashboards.setParameter('resultVar', result);
	if(resultVar == undefined || resultVar.status != "OK") return false;
	return resultVar.result;
};


cdcFunctions.enableCDACache = function(){
	Dashboards.setParameter('map','cda');
	Dashboards.setParameter('name','enabled');
	Dashboards.setParameter('value',true);
	var result = cdcFunctions.setDefinitionRequest('cda', 'enabled', true);
	Dashboards.setParameter('resultVar', result);
	if(resultVar != undefined) {
		jAlert(resultVar.result,'');
	}
	
};

cdcFunctions.disableCDACache = function(){
	Dashboards.setParameter('map','cda');
	Dashboards.setParameter('name','enabled');
	Dashboards.setParameter('value',false);
	var result = cdcFunctions.setDefinitionRequest('cda', 'enabled', false);
	Dashboards.setParameter('resultVar', result);
	if(resultVar != undefined) {
		jAlert(resultVar.result,'');
	}
};

cdcFunctions.enableMondrianCache = function(){
	Dashboards.setParameter('map','mondrian');
	Dashboards.setParameter('name','enabled');
	Dashboards.setParameter('value',true);
	var result = cdcFunctions.setDefinitionRequest('mondrian', 'enabled', true);
	Dashboards.setParameter('resultVar', result);
	if(resultVar != undefined) {
		jAlert(resultVar.result,'');
	}
};

cdcFunctions.disableMondrianCache = function(){
	Dashboards.setParameter('map','mondrian');
	Dashboards.setParameter('name','enabled');
	Dashboards.setParameter('value',false);
	var result = cdcFunctions.setDefinitionRequest('mondrian', 'enabled', false);
	Dashboards.setParameter('resultVar', result);
	if(resultVar != undefined) {
		jAlert(resultVar.result,'');
    }
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
	
	valuesArray.metadata[0] = { 'colIndex':0, 'colType':"String", 'colName':"Series"};
	valuesArray.metadata[1] = { 'colIndex':1, 'colType':"Numeric", 'colName':"Memory"};
	valuesArray.metadata[2] = { 'colIndex':2, 'colType':"Numeric", 'colName':"Count"};

	
	clone.valuesArray = valuesArray;
	
	return clone;
};

cdcFunctions.cloneCacheMemoryText = function(member, memberResults){
	var placeHolder = member.find('.cacheMemory'); 
	
	var percentage = cdc.clusterInfo.memoryPercent(memberResults.mapInfo.entryMemory,memberResults.javaRuntimeInfo);
	
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
	
	valuesArray.metadata[0] = { 'colIndex':0, 'colType':"String", 'colName':"Memory"};
	valuesArray.metadata[1] = { 'colIndex':1, 'colType':"Numeric", 'colName':"Bytes"};

	
	clone.valuesArray = valuesArray;
	
	return clone;
};


cdcFunctions.renderClusterInfo = function(){
//	var clone1, clone2, clone3;
//	var dummyElement = $("#dummyElement");
//	var dummyElementSuperClient = $("#dummyElementSuperClient");
//	
//	var holder = $("#"+this.htmlObject).empty();
//        var summaryContainer = $("<div><div>Cluster Summary</div></div>");
//        var summary = $("<div></div>");
	var results = Dashboards.getParameterValue(this.resultvar);
	
	if(results == "") return;
	if(results.status != "OK") return;
	
	results = results.result;	
  cdc.clusterInfo.draw(results);
			
};

/*********** settings *********/
cdcFunctions.getMaxSizePolicies = function(){
    var result = cdcFunctions.makeRequest('../ws-run/HazelcastConfigurationService/getMaxSizePolicies', {});
    Dashboards.setParameter('resultVar', result);
	if(resultVar != undefined){
		if(resultVar.status != "OK"){
			return [];
		} else {
			var result = [];
			for(var i = 0; i < resultVar.result.length; i++){
				var splitted = resultVar.result[i].split('_');
				var string = "";
				for(var j = 0; j < splitted.length; j++){
					string+=splitted[j].charAt(0).toUpperCase() + splitted[j].slice(1);
					if(j < splitted.length-1) string +=" ";
				}
				result.push([resultVar.result[i],string]);
			}
			return result;
		}
	}
	return [];
};


cdcFunctions.getEvictionPolicies = function(){
    var result = cdcFunctions.makeRequest('../ws-run/HazelcastConfigurationService/getEvictionPolicies', {});
    Dashboards.setParameter('resultVar', result);
	if(resultVar != undefined){
		if(resultVar.status != "OK"){
			return [];
		} else {
			var result = [];
			for(var i = 0; i < resultVar.result.length; i++){
				var splitted = resultVar.result[i].split('_');
				var string = "";
				for(var j = 0; j < splitted.length; j++){
					string+=splitted[j].charAt(0).toUpperCase() + splitted[j].slice(1);
					if(j < splitted.length-1) string +=" ";
				}
				result.push([resultVar.result[i],string]);
			}
			return result;
		}
	}
	return [];
};

cdcFunctions.getDefinition = function(name,param){
Dashboards.setParameter('name',name);
	var result = cdcFunctions.getDefinitionRequest(Dashboards.getParameterValue('map'), name);

    Dashboards.setParameter('resultVar', result);
	
	if(resultVar != undefined){
		if(resultVar.status != "OK"){
			return;
		}
		else {
			Dashboards.setParameter(param, resultVar.result);
			return;
		}
	}
	return;
};

cdcFunctions.setDefinition = function(name,param){
	Dashboards.setParameter('name',name);
	Dashboards.setParameter('value',Dashboards.getParameterValue(param));
	var result = cdcFunctions.setDefinitionRequest(Dashboards.getParameterValue('map'), name, Dashboards.getParameterValue(param));
	Dashboards.setParameter('resultVar', result);

	
	if(resultVar != undefined){
		if(resultVar.status != "OK"){
			return;
		}
	}
	return;
};

cdcFunctions.getEnabledValue = function(){
	this.getDefinition('enabled','activeCacheOn');
};

cdcFunctions.getMaxSizePolicy = function(){
	this.getDefinition('maxSizePolicy','maxSizePolicyParam');
};

cdcFunctions.getMaxSizeValue = function(){
	this.getDefinition('maxSize','maxSizeValueParam');
};

cdcFunctions.getEvictionPolicy = function(){
	this.getDefinition('evictionPolicy','evictionPolicyParam');
};

cdcFunctions.getEvictionValue = function(){
	this.getDefinition('evictionPercentage','evictionValueParam');
};

cdcFunctions.getTimeToLiveValue = function(){
	this.getDefinition('timeToLive','timeToLiveParam');
};


cdcFunctions.setEnabledValue = function(){
	this.setDefinition('enabled','cacheOnParam');
};

cdcFunctions.setMaxSizePolicy = function(){
	this.setDefinition('maxSizePolicy','maxSizePolicyParam');
};

cdcFunctions.setMaxSizeValue = function(){
	this.setDefinition('maxSize','maxSizeValueParam');
};

cdcFunctions.setEvictionPolicy = function(){
	this.setDefinition('evictionPolicy','evictionPolicyParam');
};

cdcFunctions.setEvictionValue = function(){
	this.setDefinition('evictionPercentage','evictionValueParam');
};

cdcFunctions.setTimeToLiveValue = function(){
	this.setDefinition('timeToLive','timeToLiveParam');
};


cdcFunctions.resetDefinitions = function(){
	Dashboards.update(render_activateButton);
	Dashboards.update(render_maxSizePolicy);
	Dashboards.update(render_maxSizeValue);	
	Dashboards.update(render_evictionPolicy);	
	Dashboards.update(render_evictionValue);		
	Dashboards.update(render_timeToLiveValue);	
};

cdcFunctions.saveDefinitions = function(){
	this.setEnabledValue();
	this.setMaxSizePolicy();
	this.setMaxSizeValue();
	this.setEvictionPolicy();
	this.setEvictionValue()
	this.setTimeToLiveValue()
	Dashboards.update(render_saveRequest);
};


cdcFunctions.applyDefinitions = function(){
	Dashboards.update(render_applyRequest);
};

cdc = typeof cdc != "undefined"  ? cdc : {};
cdc.clusterInfo = {
  draw: function(cinfo){
    var meminfo = [],
        countinfo = [],
        nodes = cinfo.otherMembers.slice(),
        ph = $('#clusterMembersHolder');
  
    nodes.unshift(cinfo.localMember);
    nodes[0].local = true;
    ph.empty();
    $.each(nodes,function(i,e){
      var info = e.mapInfo;
			if(!e.isSuperClient){
				/* collate the memory and count information */
				meminfo.push(["Node " + i,
						cdc.clusterInfo.memoryPercent(info.ownedMemory,e.javaRuntimeInfo),
						cdc.clusterInfo.memoryPercent(info.backupMemory,e.javaRuntimeInfo)
				]);
				countinfo.push(["Node " + i, info.ownedCount, info.backupCount]);
			}
			/* draw the detail item for this cache node */
			cdc.clusterInfo.drawNode(i,e,ph);
			
    });
    cdc.clusterInfo.drawSummary(nodes,meminfo,countinfo,$("#summaryData"));
  },

  drawNode: function(i,node,ph) {
    var $elem = $("<div id='node" + i + "' class='nodeDetail span-24 last'></div>"),
        $header = $("<div class='WDdataCell'><div class='WDdataCellHeader'>" + 
        "<div class='memberName'>Node " + i + "</div>" +
        "</div></div>"),
        $body,
        clone;
    $header.appendTo($elem);
    $("<div class='WDdataCell2 cacheIP'> Cache IP: " + node.address.substr(1) + "</div>").appendTo($elem);
    if(node.isSuperClient) {
      $header.find('.WDdataCellHeader').append("<div class='superClient'>Lite Mode</div>");
    } else {
      $body = $("#dummyElementBody").clone();
      $body.css('margin-top','0px');
      $body.css('border-top','0px solid');
      $body.find("[id]").add($body).removeAttr("id");
      //cache composition chart
      cdc.clusterInfo.drawMemoryTable(node,$body.find('.cacheComposition'));       
      //clone1 = cdcFunctions.cloneCacheCompositionChart($body, node);
      //cache memory
      cdcFunctions.cloneCacheMemoryText($body, node);
      //java runtime memory
      cdc.clusterInfo.drawRuntimeTable(node,$body.find('.javaRuntimeMemory'));
      //clone = cdcFunctions.cloneJavaRuntimeMemoryChart($body, node);
      
      //append member
      $body.appendTo($elem);
      
      //updateQueue.push(clone1);
    }
    $elem.appendTo(ph);
    
    console.log($elem);
  },

  drawSummary: function(nodes,mem,count,ph) {
    var metadata = [
      {index: 0, colType: "String", colName: "Node"},
      {index: 1, colType: "Numeric", colName: "Owned"},
      {index: 2, colType: "Numeric", colName: "Backup"},
    ];
    render_memorySummaryChart.valuesArray = {resultset: mem, metadata: metadata}
    render_memorySummaryChart.update()
    render_itemSummaryChart.valuesArray = {resultset: count, metadata: metadata}
    render_itemSummaryChart.update()

    var count = nodes.filter(function(e){return e.isSuperClient}).length;
    ph.html("<div>CDC is using " + nodes.length + " cache node" + (nodes.length > 0 ? "s" : "") +
    ", of which " + count + (count == 1 ? " is in lite mode" : " are lite nodes") +
    " (lite nodes don't hold cache data).</div>");
  },

  drawMemoryTable: function(node, ph) {

    var $table = $("<table class='composition'>");
    $table.append("<thead><tr><th class='column0'></th ><th class='column1 count'></th><th class='column2'></th></tr></thead>");
    var body = $("<tbody>").appendTo($table),
        backupMemory = cdc.clusterInfo.byteMagnitude(node.mapInfo.backupMemory.toFixed(0)),
        backupPercent = cdc.clusterInfo.memoryPercent(node.mapInfo.backupMemory.toFixed(0), node.javaRuntimeInfo); 
        ownedMemory = cdc.clusterInfo.byteMagnitude(node.mapInfo.ownedMemory.toFixed(0)),
        ownedPercent = cdc.clusterInfo.memoryPercent(node.mapInfo.ownedMemory.toFixed(0), node.javaRuntimeInfo); 
      
    var $countLine1 = $("<tr></tr>"); 
    var $countLine2 = $("<tr></tr>"); 
    
    var $memoryLine1 = $("<tr></tr>"); 
    var $memoryLine2 = $("<tr></tr>");  
       
    $("<td class='column0' rowspan='2'><b>Count</b></td>").appendTo($countLine1);
    var $countOwnedValue = $("<td class='column1 count'>"+ cdc.clusterInfo.addCommas(node.mapInfo.ownedCount) +"</td>").appendTo($countLine1);
    var $countOwnedValueBar = $("<td class='column2'></td>").appendTo($countLine1);
    
	var $countBackupValue = $("<td class='column1 count'>"+ cdc.clusterInfo.addCommas(node.mapInfo.backupCount) +"</td>").appendTo($countLine2);
	var $countBackupValueBar = $("<td class='column2'></td>").appendTo($countLine2);

    


    $("<td class='column0' rowspan='2'><b>Memory</b></td>").appendTo($memoryLine1);  
    //   $("<td class='column0'><b>Backup</b></td>").appendTo($memoryLine2);
 
	var $memoryOwnedPercentage= $("<td class='column1 count'>" + ownedMemory + " (" +ownedPercent.toFixed(1) + "%)</td>").appendTo($memoryLine1);
	var $memoryOwnedPercentageBar = $("<td class='column2'></td>").appendTo($memoryLine1);
	
    var $memoryBackupPercentage= $("<td class='column1 count'>" + backupMemory + " (" +backupPercent.toFixed(1) + "%)</td>").appendTo($memoryLine2);
    var $memoryBackupPercentageBar = $("<td class='column2'></td>").appendTo($memoryLine2);
    
    
	$countLine1.appendTo(body);
	$countLine2.appendTo(body);

	$("<tr><td colspan=3 ></td></tr>").appendTo(body);
	

	$memoryLine1.appendTo(body);	
    $memoryLine2.appendTo(body);
    
	var maxCount = Math.max.apply(Math,[node.mapInfo.ownedCount,node.mapInfo.backupCount]);
      
    ph.append($table);
    
    cdc.clusterInfo.appendBar(node.mapInfo.ownedCount, maxCount, 0, $countOwnedValueBar,"rgb(3,39,42)");
    cdc.clusterInfo.appendBar(node.mapInfo.backupCount, maxCount, 0, $countBackupValueBar,"rgb(130,146,153)");
    
    cdc.clusterInfo.appendBar(ownedPercent, 100, 0, $memoryOwnedPercentageBar,"rgb(3,39,42)");
    cdc.clusterInfo.appendBar(backupPercent, 100, 0, $memoryBackupPercentageBar,"rgb(130,146,153)");
    
  },
  
  
  appendBar: function(val, max, min, container, color){
	  var bar = $("<div>&nbsp;</div>").addClass('dataBarContainer').appendTo(container);
	  var wtmp = 70;
	  var htmp = 15;       
	  
	  var leftVal  = Math.min(val,0),
	    rightVal = Math.max(val,0);
	  
	  var xx = pv.Scale.linear(min,max).range(0,wtmp); 
	  
	  var paperSize = xx(Math.min(rightVal,max)) - xx(min);
	  paperSize = (paperSize>1)?paperSize:1;
	  var paper = Raphael(bar.get(0), paperSize , htmp);
	  var c = paper.rect(xx(leftVal), 0, xx(rightVal)-xx(leftVal), htmp);
	  
	  c.attr({
	  	fill: color,
	  	stroke: color,
	  	title: "Value: "+ val
	  });
  
  },
  
  drawRuntimeTable: function(node, ph) {
  
      var $table = $("<table class='runtime'>");
      $table.append("<thead><tr class='column0'><th></th class='column1'><th></th></tr></thead>");
      var body = $("<tbody>").appendTo($table);
      
      var $freeLine = $("<tr></tr>"); 
      var $totalLine = $("<tr></tr>"); 
      var $maxLine = $("<tr></tr>"); 
         
      $("<td class='column0'><b>Free</b></td>").appendTo($freeLine);
      $("<td class='column1 count'>"+ cdc.clusterInfo.addCommas(node.javaRuntimeInfo.freeMemory) +"</td>").appendTo($freeLine);
      var $freeValueBar = $("<td class='column2'></td>").appendTo($freeLine);
      
      
      $("<td class='column0'><b>Total</b></td>").appendTo($totalLine);
      $("<td class='column1 count'>"+ cdc.clusterInfo.addCommas(node.javaRuntimeInfo.totalMemory) +"</td>").appendTo($totalLine);
      var $totalValueBar = $("<td class='column2'></td>").appendTo($totalLine);
      
      $("<td class='column0'><b>Max</b></td>").appendTo($maxLine);
      $("<td class='column1 count'>"+ cdc.clusterInfo.addCommas(node.javaRuntimeInfo.maxMemory) +"</td>").appendTo($maxLine);
      var $maxValueBar = $("<td class='column2'></td>").appendTo($maxLine);
      
      $freeLine.appendTo(body);
      $totalLine.appendTo(body);  
      $maxLine.appendTo(body);

          
      cdc.clusterInfo.appendBar(node.javaRuntimeInfo.freeMemory, node.javaRuntimeInfo.maxMemory, 0, $freeValueBar,"rgb(3,39,42)");
          
      cdc.clusterInfo.appendBar(node.javaRuntimeInfo.maxMemory, node.javaRuntimeInfo.maxMemory, 0, $maxValueBar,"rgb(3,39,42)");
          
      cdc.clusterInfo.appendBar(node.javaRuntimeInfo.totalMemory, node.javaRuntimeInfo.maxMemory, 0, $totalValueBar,"rgb(3,39,42)");
      
           

	ph.append($table);
    },

  byteMagnitude: function(n) {
    if (n == 0) {
      return "0 B";
    }
    var units = ['B','KiB', 'MiB', 'GiB', 'TiB'];
    var magnitude = Math.min(units.length, Math.floor(Math.log(n)/Math.log(1024)));
    return (n/Math.pow(1024,magnitude)).toFixed(0) + " " + units[magnitude];
  },

  memoryPercent: function(cacheSize, runtime) {
    /*
     * When calculating the used memory percentage, we have to
     * decide what value we're using as reference. The java runtime
     * gives us several important pieces of information in this
     * regard: We're told how much memory java has allocated, how
     * much of that is free, and how much total it CAN allocate.
     *
     * Using the allocated and free memory, we can calculate the
     * amount of memory in actual usage. From that and the cache
     * size, we can determine how much of the used memory is overhead,
     * and the difference between that overhead and the maximum
     * allowed memory is the amount of memory that we actually have
     * available to cache data in. This is the final number we'll
     * use as reference for the used memory. 
     */
    var used = runtime.totalMemory - runtime.freeMemory,
        overhead = used - cacheSize,
        maxSize = runtime.maxMemory - overhead;
        
    return cacheSize / maxSize * 100;
  },
  
  addCommas: function(nStr){
  	nStr += '';
  	x = nStr.split('.');
  	x1 = x[0];
  	x2 = x.length > 1 ? '.' + x[1] : '';
  	var rgx = /(\d+)(\d{3})/;
  	while (rgx.test(x1)) {
  		x1 = x1.replace(rgx, '$1' + ',' + '$2');
  	}
  	return x1 + x2;
  }
}



