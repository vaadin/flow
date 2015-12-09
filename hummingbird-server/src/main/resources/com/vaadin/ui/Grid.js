(function(module) {

	function cache() {
	}

	cache.prototype.first = -1;
	cache.prototype.count = -1;
	cache.prototype.data = [];
	cache.prototype.sortorder = "";
	cache.prototype.containsRows = function(index, count) {
		if (this.first == -1 || this.count == -1)
			return false;

		if (this.first > index) {
			// First not cached
			return false;
		}
		var lastCached = this.first + this.count - 1;
		if (lastCached < (index + count - 1)) {
			// Last not cached
			return false;
		}

		return true;

	};

	cache.prototype.getRowsNotInCache = function(index, count) {
		var last = index + count - 1;

		if (this.containsRows(index, 1)) {
			// First is in cache
			var index = this.first + this.count;
			var count = last - index + 1; // -> count: 5
		} else if (this.containsRows(last, 1)) {
			// Last is in cache
			count = this.first - index;
		} else {
			// Nothing in cache
		}
		return {
			"index" : index,
			"count" : count
		};
	};
	cache.prototype.update = function(index, data) {
		for (var i = index; i < index + data.length; i++) {
			if (this.containsRows(i, 1)) {
				var dataIndex = i - this.first;
				this.data[dataIndex] = data[i - index];
			}
		}
	};
	cache.prototype.insert = function(index, data) {
		var count = data.length;
		if (this.containsRows(index, 1)) {
			var countBefore = index - this.first;
			var dataBefore = [];
			var dataAfter = [];
			if (index > 0) {
				dataBefore = this.data.slice(0, index);
			}
			if (this.count > index) {
				dataAfter = this.data.slice(index, this.count);
			}

			// this.data = this.data[0..index-1] + data +
			// this.data[index..this.count-1]
			this.data = dataBefore.concat(data.concat(dataAfter));
			this.count += count;
		} else if (this.containsRows(index - 1, 1)) {
			// Append
			this.data = this.data.concat(data);
			this.count += count;
		} else {
			console.error("Unexpected insert for index", index,
					"when cache range is", this.first, this.count);
		}
	};
	cache.prototype.set = function(index, data) {
		window.console.log("set(", index, data.length, ")");
		if (this.first == -1) {
			// Empty cache
			this.first = index;
			this.data = data;
			this.count = this.data.length;
		} else if (index < this.first) {
			if (index + data.length != this.first) {
				console.window.console.log("Ignored unexpected pre data: ["
						+ index + "," + data.length
						+ "] when current range is [" + this.first + ","
						+ this.count + "]");
			} else {
				this.data = data.concat(this.data);
				this.first = index;
				this.count = this.data.length;
			}
		} else if (index > this.first + this.count - 1) {
			if (index != this.first + this.count) {
				console.window.console.log("Ignored unexpected post data: ["
						+ index + "," + data.length
						+ "] when current range is [" + this.first + ","
						+ this.count + "]");
			} else {
				this.data = this.data.concat(data);
				this.count = this.data.length;
			}
		} else {
			// Update
			// current 5-10, new 8-11
			var count = data.length;
			for (var i = 0; i < count; i++) {
				var newIndex = i + index - this.first;
				this.data[newIndex] = data[i];
			}
			var maybeNewCount = index + count;
			if (maybeNewCount > this.count) {
				this.count = maybeNewCount;
			}

		}
	};
	cache.prototype.get = function(index, count) {
		// dataIndex=0: index=first
		var dataIndex = index - this.first;
		window.console.log("get(", index, count, ")");
		return this.data.slice(dataIndex, dataIndex + count);
	};
	cache.prototype.remove = function(index, count) {
		if (index < this.first) {
			count -= (this.first - index);
			index = this.first;
		}

		var dataIndex = index - this.first;
		this.data.splice(dataIndex, count);
	};
	cache.prototype.clear = function() {
		this.data = [];
		this.first = -1;
		this.count = -1;
	};

	module.init = function(grid) {
		var lastSentSelection = [] + "";
		// Custom select listener to ignore extra events and include selected
		// rows
		grid.addEventListener("selected-items-changed", function() {
			var newSelection = grid.selection.selected();
			if (newSelection + "" == lastSentSelection) {
				window.console.log("Ignored selection event with no new info");
				return;
			}
			lastSentSelection = newSelection + "";

			var event = document.createEvent("Event");
			event.initEvent("hSelect", false, true);
			event.selection = newSelection;

			grid.dispatchEvent(event);
		});

		var ds = {
			"totalSize" : 0,
			"callbacks" : {},
			"nextCallbackId" : 0,
			"cache" : new cache(),
			"clearCache" : function(containerSize) {
				ds.cache.clear();
				ds.totalSize = containerSize;
				grid.size = containerSize;
				grid.refreshItems();
			},
			"gridDatasource" : function(params, callback) {
				var reqIndex = params.index;
				var reqCount = params.count;

				if (reqCount == 0) {
					callback([]);
					grid.size = ds.totalSize;
					return;
				}

				window.console.log("grid requests ", reqIndex, reqCount);
				var sortorder = "";
				if (params.sortOrder) {
					params.sortOrder.forEach(function(f) {
						sortorder += f.column + "-" + f.direction;
					});
				}

				if (ds.cache.sortorder == sortorder
						&& ds.cache.containsRows(reqIndex, reqCount)) {
					// In cache
					window.console.log("found in cache", reqIndex, reqCount);
					callback(ds.cache.get(reqIndex, reqCount));
					return;
				} else {
					if (ds.cache.sortorder != sortorder) {
						// Sort order changed - old cache is useless
						ds.cache.clear();
						ds.cache.sortorder = sortorder;
					}

					// Fetch what is not in cache
					window.console.log("not in cache", reqIndex, reqCount);
					missing = ds.cache.getRowsNotInCache(reqIndex, reqCount);

					// FIXME To be reliable we can't use the same callback ids
					// with the server, we need to store the callbacks locally
					// and check when we get data from the server if we fulfill
					// the needs of the request

					var id = "" + ds.nextCallbackId++;
					ds.callbacks[id] = callback;
					callback.reqIndex = reqIndex;
					callback.reqCount = reqCount;

					window.console.log("fetching ", id, missing.index,
							missing.count);
					var event = document.createEvent("Event");
					event.initEvent("hData", false, true);
					event.id = id;
					event.index = missing.index;
					event.count = missing.count;
					event.cacheFirst = ds.cache.first;
					event.cacheCount = ds.cache.count;

					grid.dispatchEvent(event);
				}

			},
			"gridDatasourceCallback" : function(id, totalSize) {
				var callback = ds.callbacks[id];
				callback(ds.cache.get(callback.reqIndex, callback.reqCount));
				grid.size = totalSize;

				window.console.log("Rows ", callback.reqIndex,
						callback.reqCount, "sent to grid: ", ds.cache.get(
								callback.reqIndex, callback.reqCount));
				delete ds.callbacks[id];
			},
			"insertRows" : function(index, rows) {
				ds.cache.insert(index, rows);
				ds.totalSize += rows.length;
				grid.size += rows.length;
				// If (rowIsInGridCache)
				grid.refreshItems();
			},
			"updateRow" : function(index, row) {
				ds.cache.update(index, [ row ]);
				// If (rowIsInGridCache)
				grid.refreshItems();
			},
			"removeRows" : function(index, count) {
				ds.cache.remove(index, count);
				ds.totalSize -= count;
				grid.size -= count;
				// If (rowIsInGridCache)
				grid.refreshItems();
			}
		}

		grid.serverdata = ds;
		grid.items = ds.gridDatasource;
	}

	module.provideRows = function(grid, index, rows, totalSize, id) {
		window.console.log("provideRows", index, rows.length, totalSize, id);
		grid.serverdata.cache.set(index, rows);
		if (id) {
			grid.serverdata.gridDatasourceCallback(id, totalSize);
		}
	}

	module.invalidateCache = function(grid, containerSize) {
		window.console.log("invalidateCache", containerSize);

		grid.serverdata.clearCache(containerSize);
	}

	module.insertRows = function(grid, index, rows) {
		grid.serverdata.insertRows(index, rows);
	}

	module.removeRows = function(grid, index, count) {
		grid.serverdata.removeRows(index, count);
	}
	module.updateRow = function(grid, index, row) {
		grid.serverdata.updateRow(index, row);
	}
})(module);