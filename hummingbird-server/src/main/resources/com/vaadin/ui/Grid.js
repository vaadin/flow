(function(module) {
	var callbacks = {};
	var nextCallbackId = 0;
	
	module.init = function(grid) {
		var ignoreEmptySelection = grid.selection.size == 0;
		grid.addEventListener("select", function() {
			if (ignoreEmptySelection && grid.selection.size == 0) {
				return;
			}
			ignoreEmptySelection = false;
			
			var event = document.createEvent("Event");
			event.initEvent("hSelect", false, true);
			event.selection = grid.selection.selected();
			
			grid.dispatchEvent(event);
		});
	}
	
	module.provideRows = function(grid, id, rows, totalSize) {
		callbacks[id](rows, totalSize);
		delete callbacks[id];
	}

	module.resetDataSource = function(grid, initialRows, containerSize) {
		grid.data.source = function(req) {
			if (req.count == 0) {
				req.success([], containerSize);
				return;
			}
			
			if (initialRows != null) {
				var rows = initialRows;
				initialRows = null;
				
				if (req.index == 0) {
					req.success(rows, containerSize);
					return;
				}
			}
			
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