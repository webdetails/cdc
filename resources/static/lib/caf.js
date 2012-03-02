/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 * 
 */

/** 
 * Define Array map function
 * @global
 * @param {function} func - function to register
 * @param {Object} this
 *
 */
if (!Array.prototype.map)
{
  Array.prototype.map = function(fun /*, thisp*/)
  {
    var len = this.length;
    if (typeof fun != "function")
      throw new TypeError();

    var res = new Array(len);
    var thisp = arguments[1];
    for (var i = 0; i < len; i++)
    {
      if (i in this)
        res[i] = fun.call(thisp, this[i], i, this);
    }

    return res;
  };
}

/** 
 * Utility function to add a method to a function's prototype
 * @global
 * @param {string} name - method name
 * @param {function} func - function to register
 *
 */

Function.prototype.method = Function.prototype.method || function(name, func) {
    this.prototype[name] = func;
    return this;
};


/**
 * Webdetails namespace
 * @namespace
 */
var wd = wd || {};


/**
 * The logging priority order
 * @const
 * @type Array
 */

wd.loglevels = ['debug', 'info', 'warn', 'error', 'exception'];

/**
 * Defines the threshold level for logging.
 * @member
 */

wd.loglevel = 'debug';

/**
 * 
 * Logging function. Use this to append messages to the console with the appropriate
 * log level. Logging will only occur if the log level is above the defined threshold
 * Should be used instead of console.log
 * @param {string} m - message
 * @param {string} type - Log type: 'info','debug', 'log', 'warn', 'error', 'exception'
 * @see wd.loglevel
 */

wd.log = function (m, type){
    
    type = type || "info";
    if (wd.loglevels.indexOf(type) < wd.loglevels.indexOf(wd.loglevel)) {
        return;
    }
    if (typeof console !== "undefined" ){
        
        if (type && console[type]) {
            console[type]("["+ type +"] WD: " + m);
        }else if (type === 'exception' &&
            !console.exception) {
            console.error("["+ type +"] WD: "  + (m.stack || m));
        }
        else {
            console.log("WD: " + m);
        }
    }
   
}


/**
 * Shortcut to wd.log(m,"warn");
 * @param {string} m - message
 */

wd.warn = function(m){
    return wd.log(m, "warn");
}


/**
 * Shortcut to wd.log(m,"error");
 * @param {string} m - message
 */
wd.error = function(m){
    return wd.log(m, "error");
}


/**
 * Shortcut to wd.log(m,"info");
 * @param {string} m - message
 */
wd.info = function(m){
    return wd.log(m, "info");
}



/**
 * Shortcut to wd.log(m,"debug");
 * @param {string} m - message
 */

wd.debug = function(m){
    return wd.log(m, "debug");
}
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**
 * CDE namespace
 * @namespace
 */


wd.caf = wd.caf || {};



/**
 * CDE Modules namespace
 * @namespace
 */
wd.caf.modules = wd.caf.modules || {};

/**
 * CDE imp namespace
 * @namespace
 */
wd.caf.impl = wd.caf.impl || {};



/**
 *
 * Main CDE editor class
 * @constructor
 * @mixes wd.caf.modules.panelEngine
 * @mixes wd.caf.modules.templateEngine
 * @mixes wd.caf.modules.actionEngine
 * 
 */
wd.caf.application = function(spec) {

    var defaults = {
        
        name: "CDE",
        container: undefined , // has to be passed
        template: "default",
        transition: "basic",
        notification: "default",
        actions: undefined, // all
        panels: undefined // all
        
    };

    var myself = {},
    state;
    
    myself.options = $.extend({},defaults,spec);

    
    
    function construct(){
        wd.info("Initializing application: " + myself.options.name);
    };



    myself.init = function()  {
        
        /** Placeholder for CAF */
        myself.$ph = $(myself.options.container); 
        
        window.title = myself.options.name;
        
        // Apply mixins
        wd.caf.modules.templateEngine(myself);
        wd.caf.modules.transitionEngine(myself);
        wd.caf.modules.actionEngine(myself);
        wd.caf.modules.keybindEngine(myself);
        wd.caf.modules.popupEngine(myself);
        wd.caf.modules.panelEngine(myself);
        wd.caf.modules.notificationEngine(myself);


        myself.templateEngine.start();
        myself.transitionEngine.start();
        myself.popupEngine.start();
        myself.panelEngine.start();
        myself.notificationEngine.start();
        
        // Bind keys
        
        myself.keybindEngine.listKeybinds().map(function(keybind){
            keybind.bind();
            
        });
        
        
        
    };
    construct();
    return myself;
};

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/
/**
 * Registry class, that will manage the registred components
 * 
 * @constructor
 * @param {wd.caf.editor} myself The Editor object
 * @param {Object} spec Any configuration options you need to provide.
 */
wd.caf.registry = (function(){
    
    var myself = {};
    
    var _registry;
    
    
    /**
     * Registers an entity
     * @memberof wd.caf.registry
     * @param {string} the module space to register into
     * @param {wd.caf.entity} entity to register
     */
    
    myself.registerEntity = function(module,entity){
        if (!_registry) {
            _registry = {};
        }
        if (!_registry[module]) {
            _registry[module] = {};  
        }
        _registry[module][entity.getName()] = entity;  
    };


    /**
     * Checks if an entity is registered
     * @memberof wd.caf.registry
     * @param {string} the module space to register into
     * @param {wd.caf.entity} entity to register
     * @type Boolean
     */
    
    myself.hasEntity = function(module,entityName){
        return Boolean(_registry && _registry[module] &&
            _registry[module] && _registry[module][entityName]);
    };


    /**
     * Returns an entity from the registry
     * @memberof wd.caf.registry
     * @param {string} the module space to register into
     * @param {string} Entity name
     * @type {wd.caf.entity} Entity or null
     */

    myself.getEntity = function(module,entityName){
        try {
            return _registry[module][entityName];
        } catch (e) {
            return null;
        }
    };
 

    /**
     * Sets entity defaults
     * @memberof wd.caf.registry
     * @param {string} the module space
     * @param {string} Entity name
     * @param {Object} Defaults, Object or Function
     */
    myself.setEntityDefaults = function(module, entityName, defaults) {
        var entity = this.getEntity(module,entityName);
        if(entity) {
            entity.setDefaults(defaults);
        }
    };
    
    /**
     * Lists the entities for a specified module, and returns them in the correct order
     * @memberof wd.caf.registry
     * @param {string} Module
     * @type Array
     */
    
    myself.listEntities = function(module) {
        
        var arr = [],i;
        for (i in _registry[module]){
            if (_registry[module].hasOwnProperty(i)){
                arr.push(_registry[module][i]);       
            }
            
        }
        
        return  arr.slice().sort(function(a,b){
            return a.getOrder() - b.getOrder();
        });
    };



    /**
     * Get Template list
     * @memberof wd.caf.registry
     * @type Array
     */
    myself.listTemplates = function(){
        return myself.listEntities("templates");
    }
    
    
    /**
     * Register a template on the registry. Shortcut for registerEntity("templates",template)
     * @memberof wd.caf.registry
     * @param {string} template
     */   
    myself.registerTemplate = function(template){
        
        return myself.registerEntity("templates",template);
        
    }


    /**
     * Returns a template from the registry
     * @memberof wd.caf.registry
     * @param {string} Entity name
     * @type {wd.caf.template} Entity or null
     */
    myself.getTemplate = function(template){
        return myself.getEntity("templates", template);
    }
    
    
    
    /**
     * Get Panel list
     * @memberof wd.caf.registry
     * @type Array
     */
    myself.listPanels = function(){
        return myself.listEntities("panels");
    }
    
    
    /**
     * Register a panel on the registry. Shortcut for registerEntity("panels",panel)
     * @memberof wd.caf.registry
     * @param {string} panel
     */   
    myself.registerPanel = function(panel){
        
        return myself.registerEntity("panels",panel);
        
    }


    /**
     * Returns a panel from the registry
     * @memberof wd.caf.registry
     * @param {string} Entity name
     * @type {wd.caf.panel} Entity or null
     */
    myself.getPanel = function(panel){
        return myself.getEntity("panels", panel);
    }
    
    

    /**
     * Get Action list
     * @memberof wd.caf.registry
     * @type Array
     */
    myself.listActions = function(){
        return myself.listEntities("actions");
    }
    
    
    /**
     * Register a action on the registry. Shortcut for registerEntity("actions",action)
     * @memberof wd.caf.registry
     * @param {string} action
     */   
    myself.registerAction = function(action){
        
        return myself.registerEntity("actions",action);
        
    }


    /**
     * Returns a action from the registry
     * @memberof wd.caf.registry
     * @param {string} Entity name
     * @type {wd.caf.action} Entity or null
     */
    myself.getAction = function(action){
        return myself.getEntity("actions", action);
    }
    
    
    
    
    /**
     * Get Transition list
     * @memberof wd.caf.registry
     * @type Array
     */
    myself.listTransitions = function(){
        return myself.listEntities("transitions");
    }
    
    
    /**
     * Register a transition on the registry. Shortcut for registerEntity("transitions",transition)
     * @memberof wd.caf.registry
     * @param {string} transition
     */   
    myself.registerTransition = function(transition){
        
        return myself.registerEntity("transitions",transition);
        
    }


    /**
     * Returns a transition from the registry
     * @memberof wd.caf.registry
     * @param {string} Entity name
     * @type {wd.caf.transition} Entity or null
     */
    myself.getTransition = function(transition){
        return myself.getEntity("transitions", transition);
    }
    
       
    
    /**
     * Get Keybind list
     * @memberof wd.caf.registry
     * @type Array
     */
    myself.listKeybinds = function(){
        return myself.listEntities("keybinds");
    }
    
    
    /**
     * Register a keybind on the registry. Shortcut for registerEntity("keybinds",keybind)
     * @memberof wd.caf.registry
     * @param {string} keybind
     */   
    myself.registerKeybind = function(keybind){
        
        return myself.registerEntity("keybinds",keybind);
        
    }


    /**
     * Returns a keybind from the registry
     * @memberof wd.caf.registry
     * @param {string} Entity name
     * @type {wd.caf.keybind} Entity or null
     */
    myself.getKeybind = function(keybind){
        return myself.getEntity("keybinds", keybind);
    }

    
    
    /**
     * Get Popup list
     * @memberof wd.caf.registry
     * @type Array
     */
    myself.listPopups = function(){
        return myself.listEntities("popups");
    }
    
    
    /**
     * Register a popup on the registry. Shortcut for registerEntity("popups",popup)
     * @memberof wd.caf.registry
     * @param {string} popup
     */   
    myself.registerPopup = function(popup){
        
        return myself.registerEntity("popups",popup);
        
    }


    /**
     * Returns a popup from the registry
     * @memberof wd.caf.registry
     * @param {string} Entity name
     * @type {wd.caf.popup} Entity or null
     */
    myself.getPopup = function(popup){
        return myself.getEntity("popups", popup);
    }



    
    /**
     * Get Notification list
     * @memberof wd.caf.registry
     * @type Array
     */
    myself.listNotifications = function(){
        return myself.listEntities("notifications");
    }
    
    
    /**
     * Register a notification on the registry. Shortcut for registerEntity("notifications",notification)
     * @memberof wd.caf.registry
     * @param {string} notification
     */   
    myself.registerNotification = function(notification){
        
        return myself.registerEntity("notifications",notification);
        
    }


    /**
     * Returns a notification from the registry
     * @memberof wd.caf.registry
     * @param {string} Entity name
     * @type {wd.caf.notification} Entity or null
     */
    myself.getNotification = function(notification){
        return myself.getEntity("notifications", notification);
    }



    return myself;


})();

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */


/**
 * @class
 */

wd.caf.entity = function( spec) {
    
    var _spec = {
        name: "entity",
        type: "entity",
        order: 100,
        description: "override description"
    };
    
    
    spec = $.extend({},_spec,spec);

    var myself = {};
    
    
    /**
     * Defaults
     * @override 
    */
    var _defaults = {};
    
    
    /**
     * Calls the template initialization
     * @name template.init
     * @memberof wd.caf.template
     * @override
     */

    myself.init = function(caf){
        
        myself.log("Generic entity init","debug");
        myself.caf = caf;
        
        
    }
    
    /**
     * Get the name of the entity
     * @name entity.getName
     * @memberof wd.caf.entity
     */
    
    myself.getName = function(){
        
        return spec.name;
        
    }

    
    /**
     * Get the type of the entity
     * @name entity.getType
     * @memberof wd.caf.entity
     */
    myself.getType = function(){
        
        return spec.type;
    }
    
    
    /**
     * 
     * Gets entity description description
     * @name entity.getDescription
     * @memberof wd.caf.entity
     * 
     */
    
    myself.getDescription = function(){
        
        return spec.description;
                
    }

        
    /**
     * Get the entity order, for visual / grouping effects
     * @name entity.getOrder
     * @memberof wd.caf.entity
     */
    myself.getOrder = function(){
        
        return spec.order;
    }
    
    /**
     * Message logs with the current class name
     * @name entity.log
     * @memberof wd.caf.entity
     * @param {string} 
     * @param {string} Log level, defaults to info
     */
    myself.log = function(message,type){
        
        wd.log("["+ myself.getType() + "."+ myself.getName()  +"] " + message , type||"debug")
    }
    
    
    /**
     * Applies a specific css, useful for some entities
     * @name entity.applyCss
     * @memberof wd.caf.entity
     */

    myself.applyCss = function(){
        
        myself.log("Applying template: " + spec.cssFile, "info");
        
        var cssFiles = $.isArray(spec.cssFile)?spec.cssFile:[spec.cssFile];
        
        cssFiles.map(function(file){
            var fileref = document.createElement("link");
            fileref.setAttribute("rel", "stylesheet");
            fileref.setAttribute("type", "text/css");
            fileref.setAttribute("href", file);
        
            if (typeof fileref!="undefined"){
                document.getElementsByTagName("head")[0].appendChild(fileref)
            }
            
        });
        
        
    }

    /**
     * Sets entity defaults
     * @name entity.setDefaults
     * @memberof wd.caf.entity
     * @param {Object} Object with the defaults for this entity. Can be a Function
     */
    myself.setDefaults = function(defaults) {
    
        if (typeof defaults == 'function') {
            _defaults = defaults;
        }
        else{
            _defaults = jQuery.extend(true,{},_defaults,defaults);
        }
    };


    /**
     * Gets entity defaults, evaluating if it's a function
     * @name entity.getDefaults
     * @memberof wd.caf.entity
     * @type Object 
     */
    myself.getDefaults = function(){
        
        return typeof _defaults === "function" ? _defaults() : jQuery.extend(true,{},_defaults);
        
    }


    return myself;
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */


wd.caf.impl.actions = wd.caf.impl.actions || {};

/**
 * @class
 */


wd.caf.action = function(spec) {


    /**
     * Specific specs
     */
    
    var _spec = {
        name: "override",
        type: "action",
        order: 100,
        description: "override description"
    };

    spec = $.extend({},_spec,spec);
    var myself = wd.caf.entity(spec);
    
    
    /**
     * Describes this interface
     * @name action.init
     * @memberof wd.caf.action
     */
    myself.getOverrides = function(){
        return {
            "spec": _spec,
            "init": "Init function",
            "executeAction": "Function to call to execute the action",
            "description": "Logo placeholder"
            
        }
    }
    


    /**
     * 
     * Calls action draw
     * @name action.draw
     * @memberof wd.caf.action
     * 
     */
    
    myself.executeAction = spec.executeAction || function(){
        
        wd.debug("Generic action: " + myself.getName());
                
    }
    


    
    
    return myself;
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/
/**
 * Adds the 'actions' module to the editor.
 * @mixin
 * @param {wd.caf.editor} myself The Editor object
 * @param {Object} spec Any configuration options you need to provide.
 */
wd.caf.modules.actionEngine =  function(myself, spec) {


    /** @private*/
    var impl = myself.actionEngine = {};
    
    var actions;
    
    
    /**
     * Gets the list of actions to implement
     * @name template.init
     * @memberof wd.caf.modules.actionEngine
     */
    impl.listActions = function(){
        
        if(!actions){
            // init panels
            actions = wd.caf.registry.listActions().map(function(action){
                action.init(myself);
                return action;
            });
        }
        return actions;

    }

    /**
     * Gets the specific action
     * @name template.getAction
     * @memberof wd.caf.modules.actionEngine
     */
    impl.getAction = function(name){
        
        return wd.caf.registry.getAction(name);
                
    }



};

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */


wd.caf.impl.keybinds = wd.caf.impl.keybinds || {};

/**
 * @class
 */


wd.caf.keybind = function(spec) {


    /**
     * Specific specs
     */
    
    var _spec = {
        name: "override",
        type: "keybind",
        order: 100,
        key: "Ctrl-0",
        description: "override description"
    };

    spec = $.extend({},_spec,spec);
    var myself = wd.caf.entity(spec);
    
    
    /**
     * Describes this interface
     * @name keybind.getOverrides
     * @memberof wd.caf.keybind
     */
    myself.getOverrides = function(){
        return {
            "spec": _spec,
            "init": "Init function",
            "key": "Shortcut",
            "executeKeybind": "Function to call to execute the shortcut",
            "description": "Logo placeholder"
            
        }
    }
    

    /**
     * 
     * Gets entity key key
     * @name entity.getKey
     * @memberof wd.caf.entity
     * 
     */
    
    myself.getKey = function(){
        
        return spec.key;
                
    }


    /**
     * 
     * Calls bind
     * @name keybind.bind
     * @memberof wd.caf.keybind
     * 
     */
    
    myself.bind = function(){
        
        wd.debug("Binding key " +  spec.key + " to keybind " + myself.getName());
        $(document).bind( 'keydown', spec.key , function(){
            myself.executeKeybind()
        } );
    }
    

    /**
     * 
     * Calls executeKeybind
     * @name keybind.executeKeybind
     * @memberof wd.caf.keybind
     * 
     */
    
    myself.executeKeybind = spec.executeKeybind || function(){
        
        myself.log("Generic keybind: " + myself.getName());
                
    }
    

    

    
    
    return myself;
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/
/**
 * Adds the 'keybinds' module to the editor.
 * @mixin
 * @param {wd.caf.editor} myself The Editor object
 * @param {Object} spec Any configuration options you need to provide.
 */
wd.caf.modules.keybindEngine =  function(myself, spec) {


    /** @private*/
    var impl = myself.keybindEngine = {};
    
    var keybinds;
    
    
    /**
     * Gets the list of keybinds to implement
     * @name template.init
     * @memberof wd.caf.modules.keybindEngine
     */
    impl.listKeybinds = function(){
        
        if(!keybinds){
            // init panels
            keybinds = wd.caf.registry.listKeybinds().map(function(keybind){
                keybind.init(myself);
                return keybind;
            });
        }
        return keybinds;

    }



};

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */


wd.caf.impl.notifications = wd.caf.impl.notifications || {};

/**
 * @class
 */


wd.caf.notification = function(spec) {


    /**
     * Specific specs
     */
    
    var _spec = {
        name: "override",
        type: "notification",
        order: 100,
        description: "override description"
    };

    spec = $.extend({},_spec,spec);
    var myself = wd.caf.entity(spec);
    
    
    /**
     * Describes this interface
     * @name notification.init
     * @memberof wd.caf.notification
     */
    myself.getOverrides = function(){
        return {
            "spec": _spec,
            "init": "Init function",
            "description": "Logo placeholder",
            "notify": "Sends a noticiation, optionally specifying a type",
            "error": "Sends a error notification",
            "warn": "Sends a warn notification",
            "info": "Sends a info notification",
            "success": "Sends a success notification",
            "debug": "Sends a debug notification"
            
            
        }
    }
    


    /**
     * 
     * Calls notify
     * @name notification.draw
     * @memberof wd.caf.notification
     * @param Message
     * @param level: error, info, warn, debug, success
     * 
     */
    
    myself.notify = spec.notify || function(msg, level){
        
        wd.warn("Generic notification, override me: " + msg);
                
    }
    

    /**
     * 
     * Calls success notification
     * @name notification.success
     * @memberof wd.caf.notification
     * @param Message
     * 
     */

    myself.success = function(msg){
        
        return myself.notify(msg,"success");
                
    }
    
    /**
     * 
     * Calls info notification
     * @name notification.info
     * @memberof wd.caf.notification
     * @param Message
     * 
     */

    myself.info = function(msg){
        
        return myself.notify(msg,"info");
                
    }
    
    
    /**
     * 
     * Calls warn notification
     * @name notification.warn
     * @memberof wd.caf.notification
     * @param Message
     * 
     */

    myself.warn = function(msg){
        
        return myself.notify(msg,"warn");
                
    }
    
    /**
     * 
     * Calls debug notification
     * @name notification.debug
     * @memberof wd.caf.notification
     * @param Message
     * 
     */


    myself.debug = function(msg){
        
        return myself.notify(msg,"debug");
                
    }
    
    
    /**
     * 
     * Calls error notification
     * @name notification.error
     * @memberof wd.caf.notification
     * @param Message
     * 
     */

    myself.error = function(msg){
        
        return myself.notify(msg,"error");
                
    }
    
    return myself;
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/
/**
 * Adds the 'notifications' module to the editor.
 * @mixin
 * @param {wd.caf.editor} myself The Editor object
 * @param {Object} spec Any configuration options you need to provide.
 */
wd.caf.modules.notificationEngine =  function(myself, spec) {


    /** @private*/
    var impl = myself.notificationEngine = {};
    
    var notification;
    
    
    
    /**
     * Sets up transition engine
     * @name transitionEngine.start
     * @memberof wd.caf.modules.transitionEngine
     */
    impl.start = function(){
        
        notification = wd.caf.registry.getNotification(myself.options.notification);
        
        notification.init(myself);
        notification.applyCss();
        notification.setupNotification();



    }


    
    /**
     * Gets the specific notification
     * @name template.getNotification
     * @memberof wd.caf.modules.notificationEngine
     */
    impl.getNotification = function(){
        
        return notification;
                
    }


};

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/



/**
 * @class
 *
 */
wd.caf.impl.notifications.defaultNotification = function(spec){
    
    
    /**
     * Specific specs
     */
    
    var _spec = {
        name:"default",
        cssFile: ["../js/notifications/notificationDefault.css"]
    }

    spec = $.extend({},_spec,spec);
    var myself = wd.caf.notification(spec);
    
    
    var notificationDiv;
    

    /**
     * 
     * Specific notification initialization
     * @name notification.setupNotification
     * @memberof wd.caf.impl.notifications.defaultNotification
     *
     */
    
    myself.setupNotification = spec.setupNotification || function(){

        myself.log("Default setup notification");
     
     
        notificationDiv = $("<div/>").addClass("notificationDefault").appendTo("body");
     

    }
    
    
    myself.notify = spec.notify || function(msg, level){
        
        level = level || "info"
        
        notificationDiv.empty().text(msg).removeClass("error warn info debug success").addClass(level+" basicNotificationShown");
        
        // Hide it
        setTimeout(function(){
            notificationDiv.removeClass("basicNotificationShown");
        },2000);
        
        
    }

    

        
    return myself;
        
};

    
wd.caf.registry.registerNotification(wd.caf.impl.notifications.defaultNotification() );
    
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */


wd.caf.impl.panels = wd.caf.impl.panels || {};

/**
 * @class
 */

wd.caf.panel = function(spec) {


    /**
     * Specific specs
     */
    
    var _spec = {
        name: "override",
        type: "panel",
        order: 100,
        description: "override description",
        color: "red"
    };

    /* private stuff*/
    var $placeholder;
    
    spec = $.extend({},_spec,spec);
    var myself = wd.caf.entity(spec);
    
    
    /**
     * Describes this interface
     * @name panel.getOverrides
     * @memberof wd.caf.panel
     */
    myself.getOverrides = function(){
        return {
            "spec": _spec,
            "init": "Init function",
            "panel": "Function to call to execute the panel",
            "description": "Logo placeholder",
            "drawPanel($ph)": "Draws the panel on the specified place"
            
        }
    }
    
    
    /**
     * Returns the color of this panel
     * @name panel.getColor
     * @memberof wd.caf.panel
     */
    myself.getColor = function(){
        
        return spec.color;
        
    }
    
    
    /**
     * Draws the panel
     * @name panel.draw
     * @memberof wd.caf.panel
     */
    myself.draw = spec.draw || function($ph){
        myself.log("Panel draw action","warn");
        $ph.append(myself.getName());
    }


    /**
     * Selects this panel
     * @name panel.select
     * @memberof wd.caf.panel
     */
    myself.select = function($ph){
        myself.caf.panelEngine.selectPanel(myself);
    }
    

    
    /**
     * Sets current planel placeholder
     * @name panel.setPlaceholder
     * @memberof wd.caf.panel
     * @param the placeholder
     */
    myself.setPlaceholder = function(ph){
        $placeholder = ph;
    }


    /**
     * Gets current planel placeholder
     * @name panel.getPlaceholder
     * @memberof wd.caf.panel
     
     */
    myself.getPlaceholder = function(){
        return $placeholder;
    }


    
    return myself;
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/
/**
 * Adds the 'panels' module to the editor.
 * @mixin
 * @param {wd.caf.editor} myself The Editor object
 * @param {Object} spec Any configuration options you need to provide.
 */
wd.caf.modules.panelEngine =  function(myself, spec) {


    /** @private*/
    var impl = myself.panelEngine = {};
    
    var panels;
    var selectedPanel;
    
    
    
    /**
     * Start the panel engine, initializing the panels
     * @name template.init
     * @memberof wd.caf.modules.panelEngine
     */
    impl.start = function(){
            

        // Select the first panel
        
        var panels = impl.listPanels();
        if (panels.length > 0) {
          impl.selectPanel(panels[0]);
        }
            
        
    }
    
    
    /**
     * Gets the list of panels to implement
     * @name template.listPanels
     * @memberof wd.caf.modules.panelEngine
     */
    impl.listPanels = function(){
        
        if(!panels){
            // init panels
            panels = wd.caf.registry.listPanels().map(function(panel){
                panel.init(myself);
                return panel;
            });
        }
        return panels;
    }

    
    /**
     * Gets the specific panel
     * @name template.getPanel
     * @memberof wd.caf.modules.panelEngine
     */
    impl.getPanel = function(name){
        
        return wd.caf.registry.getPanel(name);
                
    }


    /**
     * Gets the selected panel
     * @name template.getPanel
     * @memberof wd.caf.modules.panelEngine
     */
    impl.getSelectedPanel = function(){
        
        return selectedPanel;
                
    }

    /**
     * Selects a panel
     * @name template.selectPanel
     * @memberof wd.caf.modules.panelEngine
     */

    impl.selectPanel = function(toPanel){
        
        var fromPanel = selectedPanel;


        if(selectedPanel){
            selectedPanel.getPlaceholder().removeClass("selectedPanel");
        }
        
        selectedPanel = toPanel;


        selectedPanel.getPlaceholder().addClass("selectedPanel");


        // Notify transition engine
        myself.transitionEngine.switchPanel(fromPanel,toPanel);

        
    }


};

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/


/**
 * UnderConstruction panel
 * @class
 *
 */
wd.caf.impl.panels.underConstruction = function(spec){
    
    /**
     * Specific specs
     */
    
    var _spec = {
        name: "underConstruction",
        description: "Under Construction",
        order: 10,
        color: "red"
    };


    spec = $.extend({},_spec,spec);
    var myself = wd.caf.panel(spec);
      
      
    /**
     * Describes this interface
     * @name panel.init
     * @memberof wd.caf.panel
     */
    myself.draw = spec.draw || function($ph){
        
        var d = $('<div/>').addClass("underConstruction").text("Under Construction");
        $ph.append(d);
    }
      

    return myself;
        
};


/**
 * Div content panel
 * @class
 *
 */
wd.caf.impl.panels.divContent = function(spec){
    
    /**
     * Specific specs
     */
    
    var _spec = {
        name: "divContent",
        description: "Div Content",
        order: 50,
        color: "cyan",
        selector: undefined // replace me
    };

    spec = $.extend({},_spec,spec);    
    var myself = wd.caf.panel(spec);
      
      
    /**
     * Describes this interface
     * @name panel.init
     * @memberof wd.caf.panel
     */
    myself.draw = spec.draw || function($ph){
        
        $(myself.getSelector()).detach().addClass("cafDivContentPanel").appendTo($ph);
        
    }
      
      
    myself.getSelector = function(){
        return spec.selector;
    }

    return myself;
        
};



/**
 * External div content panel
 * @class
 *
 */
wd.caf.impl.panels.externalDivContent = function(spec){
    
    /**
     * Specific specs
     */
    
    var _spec = {
        name: "externalDivContent",
        description: "External Div Content",
        order: 50,
        color: "cyan",
        url: undefined, // replace me
        selector: "" // If undefined, the entire page is inserted
    };

    spec = $.extend({},_spec,spec);
    var myself = wd.caf.impl.panels.divContent(spec);
      
      
    /**
     * Describes this interface
     * @name panel.init
     * @memberof wd.caf.panel
     */
    myself.draw = spec.draw || function($ph){
        
        myself.log("Loading external page: " + myself.getUrl()+" " + myself.getSelector())
        $("<div/>").addClass("cafExternalDivContentPanel").load(myself.getUrl()+" " + myself.getSelector()).appendTo($ph);
        
    }
      
   
    myself.getUrl = function(){
        return spec.url;
    }
    

    return myself;
              


};


/**
 * Iframe content panel
 * @class
 *
 */
wd.caf.impl.panels.iframeContent = function(spec){
    
    /**
     * Specific specs
     */
    
    var _spec = {
        name: "iframeContent",
        description: "Iframe Content",
        order: 50,
        color: "cyan",
        url: undefined // replace me
    };

    spec = $.extend({},_spec,spec);
    var myself = wd.caf.panel(spec);
      
    
    /**
     * Describes this interface
     * @name panel.init
     * @memberof wd.caf.panel
     */
    myself.draw = spec.draw || function($ph){
        
        var iframe = $("<iframe/>").addClass("cafIframeContentPanel");
        
        iframe.attr("src",myself.getUrl())
        .load(function(){iframe.iframeAutoHeight({debug:true})})
        .appendTo($ph);
        
    }
    
    
      
    myself.getUrl = function(){
        return spec.url;
    }

    return myself;
        
};



/**
* panelKeybind Keybind
* @class
*
*/
wd.caf.impl.keybinds.panelKeybind = function(spec){
    
    /**
 * Specific specs
 */
    
    var _spec = {
        name: "genericPanelKeybind",
        description: "Generic Panel Keybind"
    };


    spec = $.extend({},_spec,spec);
    var myself = wd.caf.keybind(spec);

    myself.executeKeybind = spec.executeKeybind || function(){
        
        myself.log("Calling execution for keybind " + myself.getName());
        myself.caf.panelEngine.getPanel(spec.name).select();
        
    }
    
    return myself;
    
}/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */


wd.caf.impl.popups = wd.caf.impl.popups || {};

/**
 * @class
 */

wd.caf.popup = function(spec) {


    /**
     * Specific specs
     */
    
    var _spec = {
        name: "override",
        type: "popup",
        order: 50,
        description: "Basic popup"
    };

    /* private stuff*/
    var $popup;
    var $container;
    var marginTop = 100;
    var $content; // Content of the popup

    var visible = false;
    
    spec = $.extend({},_spec,spec);
    var myself = wd.caf.entity(spec);
    
    
    /**
     * Describes this interface
     * @name popup.init
     * @memberof wd.caf.popup
     */
    myself.getOverrides = function(){
        
        return {
            "spec": _spec,
            "init": "Init function",
            "description": "Logo placeholder",
            "drawContent": "Popup paint",
            "show": "Shows the popup with the specified content",
            "hide": "Hides the popup"
        }
    }




    /**
     * Draws the content
     * @name popup.drawContent
     * @memberof wd.caf.popup
     */
    myself.drawContent = spec.drawContent || function(){

        myself.log("Here");

        return $content;
    }



    /**
     * Shows the popup
     * @name basicPopup.show
     * @memberof wd.caf.impl.popups.basicPopup
     * @param Object to control this popups' behavior. eg: {content: "Test",buttons:[...]}
     */
    myself.show = function(options){
        
         
        options = $.extend({},_options,options);
        
        myself.log("Popup show action");
        myself.caf.popupEngine.show(myself,options);
        
    }



    /**
     * Hides the popup
     * @name popup.draw
     * @memberof wd.caf.popup
     */
    myself.hide = function(){

        myself.log("Hide popup");
        myself.caf.popupEngine.hide();
    }

    return myself;
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/
/**
 * Adds the 'popups' module to the editor.
 * @mixin
 * @param {wd.caf.editor} myself The Editor object
 * @param {Object} spec Any configuration options you need to provide.
 */
wd.caf.modules.popupEngine =  function(myself, spec) {


    /** @private*/
    var impl = myself.popupEngine = {};
    
    var popups;
    
    /* private stuff*/
    var $popup;
    var $container;
    var marginTop = 100;
    var visible = false;
    
    /**
     * Start the popup engine, initializing the popups
     * @name popupEngine.init
     * @memberof wd.caf.modules.popupEngine
     */
    impl.start = function(){
            

        // Get the placeholder for the popups and apply keybinds
        
        var _$bg = $("<div/>").addClass("cafPopupBackground");
        $container = $("<div/>").addClass("cafPopupContent");
        
        $popup = $("<div/>").addClass("cafPopup").append(
            _$bg.append($container)
            ).appendTo($("body"));
        
        
        // Setup keybinds
        _$bg.on("click",function(evt){
            if (evt.target == _$bg.get(0)){
                wd.debug("Closing popup");
                impl.hide();
            }
        });


        // Keybinding esc
        $(document).bind( 'keydown', 'esc', function(){
            if(impl.isVisible()){
                wd.debug("Pressed esc, closing popup");
                impl.hide();
                
            }
        } );

        // Make sure it's initialized
        impl.listPopups();
            
        
    }
    
    
    /**
     * Shows the popup
     * @name popup.show
     * @memberof wd.caf.modules.popupEngine
     * @param wd.caf.popup
     */
    impl.show = function(panel,options){
        
        wd.debug("Popup show action");
        
        // Add content to container
        $container.empty().append(panel.drawContent(options));
        
        // Set correct popup position
        $popup.show();
        $popup.height($(document).height());
        $container.css('margin-top', $(window).scrollTop() + marginTop);
        
        $popup.addClass("cafPopupVisible");
        
        
        visible = true;
        
    }

    
    
    /**
     * Hides the popup
     * @name popup.draw
     * @memberof wd.caf.modules.popupEngine
     */
    impl.hide = function(){

        //wd.log("Hide popup");
        $popup.removeClass("cafPopupVisible");
        if (Modernizr.csstransitions){
            setTimeout(function(){
                $popup.hide();
            },300)
        }
        else{        
            $popup.hide();
        }
        
        visible = false;
    }

    
    /**
     * Checks if popup is visible
     * @name popup.draw
     * @memberof wd.caf.modules.popupEngine
     */
    impl.isVisible = function(){

        return visible;
    }


    
    
    /**
     * Gets the list of popups to implement
     * @name popupEngine.listPopups
     * @memberof wd.caf.modules.popupEngine
     */
    impl.listPopups = function(){
        
        if(!popups){
            // init popups
            popups = wd.caf.registry.listPopups().map(function(popup){
                popup.init(myself);
                return popup;
            });
        }
        return popups;

    }


    
    /**
     * Gets the specific popup
     * @name popupEngine.getPopup
     * @memberof wd.caf.modules.popupEngine
     */
    impl.getPopup = function(name){
        
        return wd.caf.registry.getPopup(name);
                
    }
    
    
    

};

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */



wd.caf.impl.popups.basicPopup = function(spec){


    /**
     * Specific specs
     */
    var _spec = {
        name: "basic",
        description: "Basic Popup"        
    };

    /* Specific options for showing */
    var _options = {        
        header: "Header",
        content: "Default basic message",
        buttons:[]
    }


    spec = $.extend({},_spec,spec);
    var options  = $.extend({},_options,options);
    var myself = wd.caf.popup(spec);
    
    
    var $topContainer, $centerContainer, $bottomContainer;
    
    
    /**
     * Shows the popup
     * @name basicPopup.show
     * @memberof wd.caf.impl.popups.basicPopup
     * @param Object to control this popups' behavior. eg: {content: "Test",buttons:[...]}
     */
    myself.show = spec.show || function(options){
        
        options = $.extend({},_options,options);
        
        myself.log("Popup show action");
        myself.caf.popupEngine.show(myself,options);
        
    }
    
    /**
     * Draws the content
     * @name basicPopup.drawContent
     * @memberof wd.caf.impl.popups.basicPopup
     * @param Object to control this popups' behavior
     */
    myself.drawContent = spec.drawContent || function(options){

        // Write: topPoup, contentPopup, bottomPopup
        
        var $ph = $("<div/>").addClass("basicPopupContainer");
        
        $topContainer = $("<div/>").addClass("cafBasicPopupTopContainer").appendTo($ph).append(myself.drawTopContent(options));
        $centerContainer = $("<div/>").addClass("cafBasicPopupCenterContainer").appendTo($ph).append(myself.drawCenterContent(options));
        $bottomContainer = $("<div/>").addClass("cafBasicPopupBottomContainer").appendTo($ph).append(myself.drawBottomContent(options));
        
        myself.log("here");
        return $ph;
    }
    
    
    /**
     * Draws the top content
     * @name basicPopup.drawTopContent
     * @memberof wd.caf.impl.popups.basicPopup
     */
    myself.drawTopContent = spec.drawTopContent || function(options){
        return options.header;
    }
    


    /**
     * Draws the center content
     * @name basicPopup.drawTopContent
     * @memberof wd.caf.impl.popups.basicPopup
     */
    myself.drawCenterContent = spec.drawCenterContent || function(options){
        if(typeof options.content === "function"){
            // Call content function passing the current content
            return options.content(myself,options);
        }
        else{
            return options.content;
        }
    }
    
    /**
     * Draws the bottom content
     * @name basicPopup.drawTopContent
     * @memberof wd.caf.impl.popups.basicPopup
     */
    myself.drawBottomContent = spec.drawBottomContent || function(options){
    
        var buttons = options.buttons;
        if($.isArray(buttons)){
        
            var $ph = $("<div/>").addClass("cafPopupBottomContent");
            myself.log("Found " + buttons.length + " buttons. Processing ","debug");
            
            buttons.map(function(button){
                
                // Check for validation function.
                var cssClass = "cafBasicPopupButton " + (button.cssClass||"");
                
                if(button.validate){
                    var v = button.validateFunction || options.validateFunction; // one of them must exist
                    if (!v()){
                        cssClass+=" cafBasicPopupNonValidated";
                    }
                }

                $("<div/>").addClass(cssClass)
                .data("button",button)
                .text(button.label)
                .appendTo($ph);
                
            })
            
            // Bind events
            $ph.on("click","div.cafBasicPopupButton:not(.cafBasicPopupNonValidated)",function(evt){
                var button = $(evt.target).data("button");
                myself.log("Clicked on button: " + button.label);
                button.callback.call(evt,myself, options);
            });
            
            
            // Trigger validations - TODO
            myself.validate(options,$ph)
            
            return $ph;
            
        }else{
            return;
        }
        
            
    
    }
    
    myself.validate = spec.validate || function(options, $ph){
        
        myself.log("Popup validation function");
        
        // Parameter is optional here
        $ph = $ph || $bottomContainer;
        
        var cls = ["cafBasicPopupNonValidated","cafBasicPopupValidated"];
        
        // Go through all the buttons and drigger the validate function
        
        $ph.find(".cafBasicPopupButton").each(function(i,b){
            
            var $button = $(b);
            var button = $button.data("button");
            
            
            
            if(button.validate){
                
                var v = button.validateFunction || options.validateFunction; // one of them must exist
                
                var idx = v()?1:0;
                // Add one, remove other
                if(!$button.hasClass(cls[idx])){
                    $button.addClass(cls[idx])
                }
                if($button.hasClass(cls[1-idx])){
                    $button.removeClass(cls[1-idx]);
                }
                
            }
            

            
            
        })
        

    }
    
    
    return myself;


}

wd.caf.registry.registerPopup( wd.caf.impl.popups.basicPopup() );



wd.caf.impl.popups.closePopup = function(spec){


    /**
     * Specific specs
     */
    var _spec = {
        name: "close",
        description: "Close Popup"        
    };

    /* Specific options for showing */
    var _options = {        
        header: "Header",
        content: "Default Close message",
        validateFunction: undefined,
        buttons:[{
            label: "Close",
            validate: false,
            cssClass: "popupClose", 
            callback: function(popup,options){
                myself.hide()
            },
            validateFunction: undefined
        }
        ]
    }


    spec = $.extend({},_spec,spec);
    var options  = $.extend({},_options,options);
    var myself = wd.caf.impl.popups.basicPopup(spec);
    
    
    /**
     * Shows the popup
     * @name basicPopup.show
     * @memberof wd.caf.impl.popups.basicPopup
     * @param Object to control this popups' behavior. eg: {content: "Test",buttons:[...]}
     */
    myself.show = spec.show || function(options){
        
        options = $.extend({},_options,options);
        
        myself.log("Popup show action");
        myself.caf.popupEngine.show(myself,options);
        
    }
    
    return myself;
}


wd.caf.registry.registerPopup( wd.caf.impl.popups.closePopup() );



wd.caf.impl.popups.okcancelPopup = function(spec){


    /**
     * Specific specs
     */
    var _spec = {
        name: "okcancel",
        description: "Ok/Cancel Popup"        
    };

    /* Specific options for showing */
    var _options = {        
        header: "Header",
        content: "Default Ok/Cancel message",
        validateFunction: undefined,
        okCallback: function(){
            myself.log("Action not defined","warn")
        },
        buttons:[
        {
            label: "Ok",
            validate: true,
            cssClass: "popupClose", 
            callback: function(popup,options){
                // Call function. if return true, hide.
                if(options.okCallback(popup,options)){
                    myself.hide();
                }
            },
            validateFunction: undefined
        },
        {
            
            label: "Cancel",
            validate: false,
            cssClass: "popupClose", 
            callback: function(popup,options){
                myself.hide()
            },
            validateFunction: undefined
        }
        ]
    }


    spec = $.extend({},_spec,spec);
    var options  = $.extend({},_options,options);
    var myself = wd.caf.impl.popups.basicPopup(spec);
    
    
    /**
     * Shows the popup
     * @name basicPopup.show
     * @memberof wd.caf.impl.popups.basicPopup
     * @param Object to control this popups' behavior. eg: {content: "Test",buttons:[...]}
     */
    myself.show = spec.show || function(options){
        
        options = $.extend({},_options,options);
        
        myself.log("Popup show action");
        myself.caf.popupEngine.show(myself,options);
        
    }
    
    return myself;
}


wd.caf.registry.registerPopup( wd.caf.impl.popups.okcancelPopup() );/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */


wd.caf.impl.templates = wd.caf.impl.templates || {};

/**
 * @class
 */


wd.caf.template = function( spec) {


    /**
     * Specific specs
     */
    
    var _spec = {
        name: "override",
        cssFile: "override",
        type: "template"
    };

    spec = $.extend({},_spec,spec);
    var myself = wd.caf.entity(spec);
    
    
    /**
     * Describes this interface
     * @name template.init
     * @memberof wd.caf.template
     */
    myself.getOverrides = function(){
        return {
            "spec": _spec,
            "init": "Init function",
            "draw": "Function to call to draw the main template",
            "drawPanelOnContainer": "Function to draw an individual panel on container",
            "$logo": "Logo placeholder",
            "$title": "Title placeholder",
            "$actions": "Actions placeholder",
            "$panels": "Panel switcher placeholder",
            "$panelsContainer": "Panels placeholder"
            
        }
    }
    


    /**
     * 
     * Calls template draw
     * @name template.draw
     * @memberof wd.caf.template
     * 
     */
    myself.draw = spec.draw || function(){
        
        myself.log("Generic template draw on " + myself.caf.$ph ,"debug");
        
        
        // Create the main sections
        
        var wrapper = myself.createMainSections();
        
        myself.addActions();
        myself.addPanels();
        
        //myself.addKeyBinds();
        
        
        myself.caf.$ph.append(wrapper);
    }


    /**
     * Creates the main structure of the template
     * @name createMainSections
     * @memberof wd.caf.template
     * 
     */
    myself.createMainSections = spec.createMainSections || function(){
        
        
        var wrapper = $('<div class="templateWrapper"></div>');
        var header  = $('<div class="templateHead"></div>').appendTo(wrapper);

        myself.$logo = $('<div class="templateLogo"></div>').appendTo(header);
        myself.$title = $('<div class="templateTitle"></div>').appendTo(header);
        myself.$actions = $('<div class="templateActions"></div>').appendTo(header);
        myself.$panels = $('<div class="templatePanels"></div>').appendTo(header);
  
        header.append($('<div class="templateHShadow"></div>'));
        
        myself.$panelsContainer = $('<div class="templatePanelsContainer"></div>')
        .appendTo(wrapper);
        ;
        
        
        return wrapper;
        ph.append(wrapper);
        
        
    }
    
    
    /**
     * Adds the actions to the UI
     * @name addActions
     * @memberof wd.caf.template
     * 
     */
    myself.addActions = spec.addActions || function(){
        
        var actions = myself.caf.actionEngine.listActions();
        
        actions.map(function(action){
            $('<div class="cafAction cafLinks"></div>').text(action.getDescription())
            .data("action",action)
            .appendTo(myself.$actions);
        });
        
        // bind actions
        myself.$actions.on("click",".cafAction",function(event){
            $(this).data("action").executeAction();
        });
        
    }
        

    
    /**
     * Adds the panels to the UI
     * @name addPanels
     * @memberof wd.caf.template
     * 
     */
    myself.addPanels = spec.addPanels || function(){
        
        var panels = myself.caf.panelEngine.listPanels();
        
        // Build the links and draw them;
        
        panels.map(function(panel){
            $('<div class="cafPanel cafLinks"></div>').text(panel.getDescription())
            .data("panel",panel)
            .appendTo(myself.$panels);
            
            
            
            myself.drawPanelOnContainer(panel);
        });
        
        // bind panels
        myself.$panels.on("click",".cafPanel",function(event){
            var panel = $(this).data("panel");
            myself.log("Clicked on Panel " + panel.getName());
            panel.select();
        });
        
        
    }

    
        
    /**
     * Draws the panel on the template
     * @name template.drawPanelOnContainer
     * @memberof wd.caf.template
     * @param Panel to draw
     */

    myself.drawPanelOnContainer = spec.drawPanelOnContainer || function(panel){
    
    
        var container = $('<div class="panelContainer"></div>').appendTo(myself.$panelsContainer);

        $('<div class="panelTitle"></div>').text(panel.getDescription()).css("background-color",panel.getColor())
        .appendTo(container);
        myself.$panelsContainer.append(panel.draw(container));

        panel.setPlaceholder(container);


    }
    
    
    
    return myself;
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/
/**
 * Adds the 'templates' module to the editor.
 * @mixin
 * @param {wd.caf.editor} myself The Editor object
 * @param {Object} spec Any configuration options you need to provide.
 */
wd.caf.modules.templateEngine =  function(myself, spec) {


    /** @private*/
    var impl = myself.templateEngine = {};
    var template;
    var popup;

    /**
     * @name templateEngine.start
     * @memberof wd.caf.modules.templateEngine
     */
   

    impl.start = function(){
            
        template = wd.caf.registry.getTemplate(myself.options.template);
        if(!template){
            wd.log( "Template '" + myself.options.template +"' not found. Can't continue"  ,"exception");
            throw exception /* TODO: How to best throw exceptions? */
        }
            
        template.init(myself);
        template.applyCss();
        template.draw();
        
        
    }
    
    
    /**
     * Returns the current template
     * @name templateEngine.getTemplate
     * @memberof wd.caf.modules.templateEngine
     */
    impl.getTemplate = function(){
        
        return template;
    }
    
    
    /**
     * Returns the generic popup
     * @name templateEngine.getPopup
     * @memberof wd.caf.modules.templateEngine
     */    
    impl.getPoup = function() {
        return popup;
        
    }
    
    
    
    
    
};
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/



/**
 * @class
 *
 */
wd.caf.impl.templates.defaultTemplate = function(spec){
    
    
    /**
     * Specific specs
     */
    
    var _spec = {
        name:"default",
        cssFile: ["../js/templates/templateDefault.css"]
    }

    spec = $.extend({},_spec,spec);
    var myself = wd.caf.template(spec);
        
        
    return myself;
        
};

    
wd.caf.registry.registerTemplate(wd.caf.impl.templates.defaultTemplate() );
    

    /* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/



/**
 * @class
 *
 */
wd.caf.impl.templates.emptyTemplate = function(spec){
    
    
    /**
     * Specific specs
     */
    var _spec = {
        name:"empty",
        cssFile: ["../js/templates/templateDefault.css"],
        createMainSections: function() {},
        addActions: function() {},
        addPanels: function() {
          var panels = myself.caf.panelEngine.listPanels();
          panels.map(function(p){
            myself.drawPanelOnContainer(p);
          });
        },
        drawPanelOnContainer: function(panel) {
          panel.setPlaceholder($ph);
        }
    },
    /* We need a fake placeholder to add stuff to,
     * just so there's no random breakage because
     * stuff expects a placeholder that's not there.
     */
    $ph = $("<div>");

    spec = $.extend({},_spec,spec);
    var myself = wd.caf.template(spec);
        
        
    return myself;
        
};

    
wd.caf.registry.registerTemplate(wd.caf.impl.templates.emptyTemplate() );
    

    
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */


wd.caf.impl.transitions = wd.caf.impl.transitions || {};

/**
 * @class
 */


wd.caf.transition = function( spec) {


    /**
     * Specific specs
     */
    
    var _spec = {
        name: "override",
        type: "transition"
    };

    spec = $.extend({},_spec,spec);
    var myself = wd.caf.entity(spec);
    
    
    /**
     * Describes this interface
     * @name transition.init
     * @memberof wd.caf.transition
     */
    myself.getOverrides = function(){
        return {
            "spec": _spec,
            "init": "Init function",
            "setupTransition": "Specific transition initialization",
            "switchTo": "Function to call to switch to a specific panel"
            
        }
    }
   
   
    /**
     * 
     * Specific transition initialization
     * @name transition.switchTo
     * @memberof wd.caf.transition
     * @param panel to switch to
     */
    
    myself.setupTransition = spec.setupTransition || function(panel){
        
        myself.log("generic setupTransition");
                
    }


        
    /**
     * Panel switching event
     * @name transitionEngine.switchPanel
     * @memberof wd.caf.transition
     * @param Origin panel
     * @param Destination panel
     */
    myself.switchPanel = spec.switchPanel || function(fromPanel, toPanel){
        
        myself.log("beginPanelSwitch to - not done yet","warn");
    }
    
        
    
    return myself;
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/
/**
 * Adds the 'transitions' module to the editor.
 * @mixin
 * @param {wd.caf.editor} myself The Editor object
 * @param {Object} spec Any configuration options you need to provide.
 */
wd.caf.modules.transitionEngine =  function(myself, spec) {


    /** @private*/
    var impl = myself.transitionEngine = {};
    var transition;


    /**
     * Sets up transition engine
     * @name transitionEngine.start
     * @memberof wd.caf.modules.transitionEngine
     */
    impl.start = function(){
            

        transition = wd.caf.registry.getTransition(myself.options.transition);
        
        transition.init(myself);
        transition.applyCss();
        transition.setupTransition();



    }
    
    
    /**
     * Gets current transition
     * @name transitionEngine.getTransition
     * @memberof wd.caf.modules.transitionEngine
     */
    impl.getTransition = function(){
        
        return transition;
    }
    

    /**
     * Starts panel switching event. Transition is responsable for anycallback here
     * @name transitionEngine.switchPanel
     * @memberof wd.caf.modules.transitionEngine
     * @param Origin panel
     * @param Destination panel
     */
    impl.switchPanel = function(fromPanel, toPanel){
        
        impl.getTransition().switchPanel(fromPanel, toPanel);
    }
    
    
    /**
     * Ends panel switching event
     * @name transitionEngine.beginPanelSwitch
     * @memberof wd.caf.modules.transitionEngine
     * @param Origin panel
     * @param Destination panel
     */    
    impl.endPanelSwitch = function(fromPanel, toPanel){
        
        impl.getTransition().endPanelSwitch(fromPanel, toPanel);
    }

    
};
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

/**jslint globals: wd*/



/**
 * @class
 *
 */
wd.caf.impl.transitions.basicTransition = function(spec){
    
    
    /**
     * Specific specs
     */
    
    var _spec = {
        name:"basic",
        cssFile: ["../js/transitions/transitionBasic.css"]
    }

    spec = $.extend({},_spec,spec);
    var myself = wd.caf.transition(spec);
    
    

    /**
     * 
     * Specific transition initialization
     * @name transition.switchTo
     * @memberof wd.caf.transition
     * @param panel to switch to
     */
    
    myself.setupTransition = spec.setupTransaction || function(panel){

        myself.log("Basic setup transition");
        
        /*
         *  Hide all panels
         */ 
        
        myself.caf.panelEngine.listPanels().map(function(panel){
           panel.getPlaceholder().addClass("basicTransitionHidden"); 
        });
    }


        
    /**
     * Starts panel switching event
     * @name transitionEngine.switchTo
     * @memberof wd.caf.impl.transitions.basicTransition
     * @param Origin panel
     * @param Destination panel
     */
    myself.switchPanel = spec.switchPanel || function(fromPanel, toPanel){


        if(fromPanel){
            myself.log("Switching from panel " + fromPanel.getDescription() + " to panel " + toPanel.getDescription());
            fromPanel.getPlaceholder().addClass("basicTransitionHidden");
            toPanel.getPlaceholder().removeClass("basicTransitionHidden");
            
        }
        else{
            myself.log("Switching to panel " + toPanel.getDescription());
            toPanel.getPlaceholder().removeClass("basicTransitionHidden");
        }
        
    }


        
    return myself;
        
};

    
wd.caf.registry.registerTransition(wd.caf.impl.transitions.basicTransition() );
    

    