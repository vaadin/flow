(function() {
	var apps = {};
	var widgetsets = {};
	
	
    var log;
    if (typeof console === "undefined" || !window.location.search.match(/[&?]debug(&|$)/)) {
    	//If no console.log present, just use a no-op
    	log = function() {};
    } else if (typeof console.log === "function") {
    	//If it's a function, use it with apply
		log = function() {
			console.log.apply(console, arguments);
		};
    } else {
    	//In IE, its a native function for which apply is not defined, but it works without a proper 'this' reference
    	log = console.log;
    }
	

	var isInitializedInDom = function(appId) {
		var appDiv = document.getElementById(appId);
		if (!appDiv) {
			return false;
		}
		for ( var i = 0; i < appDiv.childElementCount; i++) {
			var className = appDiv.childNodes[i].className;
			// If the app div contains a child with the class
			// "v-app-loading" we have only received the HTML 
			// but not yet started the widget set
			// (UIConnector removes the v-app-loading div).
			if (className && className.indexOf("v-app-loading") != -1) {
				return false;
			}
		}
		return true;
	};

	window.vaadin = window.vaadin || {};
	window.vaadin.framework = window.vaadin.framework || {
		initApplication: function(appId, config) {
			var testbenchId = appId.replace(/-\d+$/, '');
			
			if (apps[appId]) {
				if (window.vaadin && window.vaadin.framework && window.vaadin.framework.clients && window.vaadin.framework.clients[testbenchId] && window.vaadin.framework.clients[testbenchId].initializing) {
					throw "Application " + appId + " is already being initialized";
				}
				if (isInitializedInDom(appId)) {
					throw "Application " + appId + " already initialized";
				}
			}

			log("init application", appId, config);
			
			window.vaadin.framework.clients[testbenchId] = {
					isActive: function() {
						return true;
					},
					initializing: true
			};
			
			var getConfig = function(name) {
				var value = config[name];
				return value;
			};
			
			var fetchRootConfig = function(callback) {
				log('Fetching root config');
				var updatedConfig = JSON.parse(getConfig("initialUIDL"));
							
				// Copy new properties to the config object
				for (var property in updatedConfig) {
					if (updatedConfig.hasOwnProperty(property)) {
						config[property] = updatedConfig[property];
					}
				}
				
				// Try bootstrapping again, this time without fetching missing info
				bootstrapApp(false);

				// Run the fetchRootConfig callback if present.
				callback && callback(r);
				
				// send parameters as POST data
				r.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
				r.send(params);
				
				log('sending request to ', url);
			};			
			
			//Export public data
			var app = {
				getConfig: getConfig,
			};
			apps[appId] = app;
			
			if (!window.name) {
				window.name =  appId + '-' + Math.random();
			}
		
			var widgetset = getConfig('client-engine');
			widgetsets[widgetset] = {
					pendingApps: []
				};
			if (widgetsets[widgetset].callback) {
				log("Starting from bootstrap", appId);
				widgetsets[widgetset].callback(appId);
			}  else {
				log("Setting pending startup", appId);
				widgetsets[widgetset].pendingApps.push(appId);
			}

			if (getConfig("debug")) {
				// TODO debug state is now global for the entire page, but should somehow only be set for the current application  
				window.vaadin.framework.debug = true;
			}
			
			return app;
		},
		clients: {},
		getAppIds: function() {
			var ids = [ ];
			for (var id in apps) {
				if (apps.hasOwnProperty(id)) {
					ids.push(id);
				}
			}
			return ids;
		},
		getApp: function(appId) {
			return apps[appId];
		},
		registerWidgetset: function(widgetset, callback) {
			log("Client engine registered", widgetset);
			var ws = widgetsets[widgetset];
			if (ws && ws.pendingApps) {
				ws.callback = callback;
				for(var i = 0; i < ws.pendingApps.length; i++) {
					var appId = ws.pendingApps[i];
					log("Starting client engine for app", appId);
					callback(appId);
				}
				ws.pendingApps = null;
			}
		},
	};
	window.vaadin.clients = window.vaadin.framework.clients; // TestBench compatibility
	log('Vaadin bootstrap loaded');
})();
