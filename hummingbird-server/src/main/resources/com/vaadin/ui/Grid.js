(function(module) {
	var callbacks = {};
	var nextCallbackId = 0;
	
	module.provideRows = function(grid, id, rows, totalSize) {
		callbacks[id](rows, totalSize);
		delete callbacks[id];
	}

	module.resetDataSource = function(grid, containerSize) {
		grid.data.source = function(req) {
			var id = "" + nextCallbackId++;
			callbacks[id] = req.success;

			var event = document.createEvent("Event");
			event.initEvent("hData", false, true);
			event.id = id;
			event.index = req.index;
			event.count = req.count;

			grid.dispatchEvent(event);
		}

		grid.data.clearCache(containerSize);
	}
})(module);