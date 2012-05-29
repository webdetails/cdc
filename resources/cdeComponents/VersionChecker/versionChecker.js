/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

var CurrentVersionComponent = BaseComponent.extend({
  ph: null,
  versionCheckUrl: '/pentaho/content/cdc/getVersion',
  
  update : function(){
    var self = this;
    this.ph = $('#' + this.htmlObject).empty();
    
    $.get(this.versionCheckUrl, function(result){
      var msgHolder = $('<div/>').html(result);
      self.ph.append(msgHolder);
    });
    
  }
  
});

var VersionCheckComponent = BaseComponent.extend({
  
  ph: null,
  
  versionCheckUrl: '/pentaho/content/cdc/checkVersion',
  
  versionInfo: null,
    
  getMsgUpdate : function(url){
      return 'You are currently running an outdated version. Please update to the new version <a href="'+ url + '">here</a>.';
  },
  
  getMsgLatest: function(){
    return 'Your version is up to date.';
  },
  
  getMsgInconclusive: function(msg){
    return 'Only ctools branches support version checking ' + (msg ? '(' + msg + ') .' : '.');
  },
  
  getMsgError: function(errorMsg){
    return 'There was an error checking for newer versions: ' + errorMsg;
  },
  
  update: function(){
    var self = this;
    this.ph = $("#" + this.htmlObject).empty();
    
    $.get(this.versionCheckUrl, function(result){
      
      if(!result){
        //TODO:log
        return;
      }
      
      result = JSON.parse(result);
      self.versionInfo = result;
      
      var msg = '';
      //error | inconclusive | update | latest
      switch(result.result){
        case 'update':
          msg = self.getMsgUpdate(result.downloadUrl);
          break;
        case 'latest':
          msg = self.getMsgLatest();
          break;
        case 'error':
          msg = self.getMsgError(result.msg);
          break;
        case 'inconclusive':
          msg = self.getMsgInconclusive(result.msg);
          break;
      }
      
      var msgHolder = $('<div/>').html(msg);
      self.ph.append(msgHolder);
    });
  }
  
  
});
