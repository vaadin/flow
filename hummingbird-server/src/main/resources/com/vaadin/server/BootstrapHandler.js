var apps = {};
var widgetsets = {};
	
	
var log;
if (typeof console === undefined || !window.location.search.match(/[&?]debug(&|$)/)) {
	/* If no console.log present, just use a no-op */
	log = function() {};
} else if (typeof console.log === "function") {
	/* If it's a function, use it with apply */
	log = function() {
		console.log.apply(console, arguments);
	};
} else {
	/* In IE, its a native function for which apply is not defined, but it works
	 without a proper 'this' reference */
	log = console.log;
}

var isWidgetsetLoaded = function(widgetset) {
	var className = widgetset.replace(/\./g, "_");
	return (typeof window[className]) != "undefined";
};

var loadWidgetset = function(url, widgetset) {
	if (widgetsets[widgetset]) {
		return;
	}
	log("load widgetset", url, widgetset);
	setTimeout(function() {
		if (!isWidgetsetLoaded(widgetset)) {
			alert("Failed to load the widgetset: " + url);
		}
	}, 15000);

	var scriptTag = document.createElement('script');
	scriptTag.setAttribute('type', 'text/javascript');
	scriptTag.setAttribute('src', url);
	document.getElementsByTagName('head')[0].appendChild(scriptTag);
	
	widgetsets[widgetset] = {
		pendingApps: []
	};
};

var isInitializedInDom = function(appId) {
	var appDiv = document.getElementById(appId);
	if (!appDiv) {
		return false;
	}
	for ( var i = 0; i < appDiv.childElementCount; i++) {
		var className = appDiv.childNodes[i].className;
		/* If the app div contains a child with the class
		"v-app-loading" we have only received the HTML
		but not yet started the widget set
		(UIConnector removes the v-app-loading div). */
		if (className && className.indexOf("v-app-loading") != -1) {
			return false;
		}
	}
	return true;
};

window.vaadin = window.vaadin || {
	initApplication: function(appId, config) {
		var testbenchId = appId.replace(/-\d+$/, '');
		
		if (apps[appId]) {
			if (window.vaadin && window.vaadin.clients && window.vaadin.clients[testbenchId] && window.vaadin.clients[testbenchId].initializing) {
				throw "Application " + appId + " is already being initialized";
			}
			if (isInitializedInDom(appId)) {
				throw "Application " + appId + " already initialized";
			}
		}

		log("init application", appId, config);
		
		window.vaadin.clients[testbenchId] = {
				isActive: function() {
					return true;
				},
				initializing: true
		};
		
		var getConfig = function(name) {
			var value = config[name];
			return value;
		};
		
		/* Export public data */
		var app = {
			getConfig: getConfig
		};
		apps[appId] = app;
		
		if (!window.name) {
			window.name =  appId + '-' + Math.random();
		}
		var bootstrapApp = function() {
			var vaadinDir = getConfig('vaadinDir');

			var versionInfo = getConfig('versionInfo');

			var widgetset = "client";
			var clientEngineFileName = getConfig("clientEngineFile");
			widgetsetUrl = vaadinDir + "client/" +clientEngineFileName;
			loadWidgetset(widgetsetUrl, widgetset);

			if (widgetsets[widgetset].callback) {
				log("Starting from bootstrap", appId);
				widgetsets[widgetset].callback(appId);
			}  else {
				log("Setting pending startup", appId);
				widgetsets[widgetset].pendingApps.push(appId);
			}
		};
		bootstrapApp();

		if (getConfig("debug")) {
			/* TODO debug state is now global for the entire page, but should
			somehow only be set for the current application */
			window.vaadin.debug = true;
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
		log("Widgetset registered", widgetset);
		var ws = widgetsets[widgetset];
		if (ws && ws.pendingApps) {
			ws.callback = callback;
			for(var i = 0; i < ws.pendingApps.length; i++) {
				var appId = ws.pendingApps[i];
				log("Starting from register widgetset", appId);
				callback(appId);
			}
			ws.pendingApps = null;
		}
	}
};

log('Vaadin bootstrap loaded');

{{GWT_STAT_EVENTS}}

var uidl = {{INITIAL_UIDL}};
var config = {{CONFIG_JSON}};
config.uidl = uidl;
vaadin.initApplication("{{APP_ID}}", config);
