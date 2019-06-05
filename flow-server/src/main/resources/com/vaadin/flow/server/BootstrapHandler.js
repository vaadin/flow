(function() {
	var apps = {};
	var widgetsets = {};
	window.Vaadin = window.Vaadin || {};
	window.Vaadin.Flow = window.Vaadin.Flow || {
        clients: {},
		initApplication: function(appId, config) {
			var testbenchId = appId.replace(/-\d+$/, '');
			
			if (apps[appId]) {
				if (window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients && window.Vaadin.Flow.clients[testbenchId] && window.Vaadin.Flow.clients[testbenchId].initializing) {
					throw "Application " + appId + " is already being initialized";
				}
			}
	
			console.debug("init application", appId, config);
			
			window.Vaadin.Flow.clients[testbenchId] = {
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
	
			console.debug("Setting pending startup", appId);
			widgetsets["client"] = {
				pendingApps: [appId]
			};
	
			return app;
		},
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
			console.debug("Widgetset registered", widgetset);
			var ws = widgetsets[widgetset];
			if (ws && ws.pendingApps) {
				ws.callback = callback;
				for(var i = 0; i < ws.pendingApps.length; i++) {
					var appId = ws.pendingApps[i];
					console.debug("Starting from register widgetset", appId);
					callback(appId);
				}
				ws.pendingApps = null;
			}
		},
        getBrowserDetailsParameters: function() {
            var params = {  };

            /* Screen height and width */
            params['v-sh'] = window.screen.height;
            params['v-sw'] = window.screen.width;

            /* Current time */
            var date = new Date();
            params['v-curdate'] = date.getTime();

            /* Current timezone offset (including DST shift) */
            var tzo1 = date.getTimezoneOffset();

            /* Compare the current tz offset with the first offset from the end
            of the year that differs --- if less that, we are in DST, otherwise
            we are in normal time */
            var dstDiff = 0;
            var rawTzo = tzo1;
            for(var m = 12; m > 0; m--) {
                date.setUTCMonth(m);
                var tzo2 = date.getTimezoneOffset();
                if (tzo1 != tzo2) {
                    dstDiff = (tzo1 > tzo2 ? tzo1 - tzo2 : tzo2 - tzo1);
                    rawTzo = (tzo1 > tzo2 ? tzo1 : tzo2);
                    break;
               }
            }

            /* Time zone offset */
            params['v-tzo'] = tzo1;

            /* DST difference */
            params['v-dstd'] = dstDiff;

            /* Time zone offset without DST */
            params['v-rtzo'] = rawTzo;

            /* DST in effect? */
            params['v-dston'] = (tzo1 != rawTzo);

            /* Time zone id (if available) */
            try {
                params['v-tzid'] = Intl.DateTimeFormat().resolvedOptions().timeZone;
            } catch (err) {
                params['v-tzid'] = '';
            }

            /* Window name */
            if (window.name) {
                params['v-wn'] = window.name;
            }

            /* Detect touch device support */
            var supportsTouch = false;
            try {
                document.createEvent("TouchEvent");
                supportsTouch = true;
            } catch (e) {
                /* Chrome and IE10 touch detection */
                supportsTouch = 'ontouchstart' in window
                   || (typeof navigator.msMaxTouchPoints !== 'undefined');
            }
            params['v-td'] = false;

            /* Stringify each value (they are parsed on the server side) */
            Object.keys(params).forEach(function(key) {
                var value = params[key];
                if (typeof value !== 'undefined') {
                    params[key] = value.toString();
                }
            });
            return params;
        }
	};
	
	console.debug('Flow bootstrap loaded');
	
	{{GWT_STAT_EVENTS}}
	
	var uidl = {{INITIAL_UIDL}};
	var config = {{CONFIG_JSON}};
	config.uidl = uidl;

    window.Vaadin.Flow.initApplication("{{APP_ID}}", config);
})();
