window.gridConnector = {
    initLazy: function(grid) {
        var extraPageBuffer = 2;
        var pageCallbacks = {};
        var cache = {};
        var lastRequestedRange = [0, 0];

        var validSelectionModes = ['SINGLE', 'NONE', 'MULTI'];
        var selectedKeys = {};
        var selectionMode = 'SINGLE';

        grid.size = 0; // To avoid NaN here and there before we get proper data

        var doSelection = function(item, userOriginated) {
            if (selectionMode === 'NONE') {
                return;
            }
            if (selectionMode === 'SINGLE') {
                grid.selectedItems = [];
                selectedKeys = {};
            }
            grid.selectItem(item);
            selectedKeys[item.key] = item;
            if (userOriginated) {
                grid.$server.select(item.key);
            }
        };

        var doDeselection = function(item, userOriginated) {
            if (selectionMode === 'SINGLE' || selectionMode === 'MULTI') {
                grid.deselectItem(item);
                delete selectedKeys[item.key];
                if (userOriginated) {
                    grid.$server.deselect(item.key);
                }
            }
        };

        // $connector postfix to reduce change of name collision
        grid._activeItemChanged$connector = function(newVal, oldVal) {
            if (!newVal) {
                return;
            }
            if (!selectedKeys[newVal.key]) {
                doSelection(newVal, true);
            } else {
                doDeselection(newVal, true);
            }
        };
        grid._createPropertyObserver('activeItem', '_activeItemChanged$connector', true);

        grid.dataProvider = function(params, callback) {
            if (params.pageSize != grid.pageSize) { 
                throw "Invalid pageSize"; 
            }

            var page = params.page;
            if (cache[page]) {
                callback(cache[page]);
            } else {
                pageCallbacks[page] = callback;
            }
            // Determine what to fetch based on scroll position and not only
            // what grid asked for
            var firstNeededPage = Math.min(page, grid._getPageForIndex(grid._virtualStart));
            var lastNeededPage = Math.max(page, grid._getPageForIndex(grid._virtualEnd));

            var first = Math.max(0,  firstNeededPage - extraPageBuffer);
            var last = Math.min(lastNeededPage + extraPageBuffer, Math.max(0, Math.floor(grid.size / grid.pageSize) + 1));

            if (lastRequestedRange[0] != first || lastRequestedRange[1] != last) {
                lastRequestedRange = [first, last];
                var count = 1 + last - first;
                // setTimeout to avoid race condition in ServerRpcQueue
                setTimeout(() => grid.$server.setRequestedRange(first * grid.pageSize, count * grid.pageSize), 0);
            }
        }

        var updateGridCache = function(page) {
            var items = cache[page];
            // Force update unless there's a callback waiting
            if (!pageCallbacks[page]) {
                if (items) {
                    // Replace existing cache page
                    grid._cache[page] = items;
                } else {
                    delete grid._cache[page];
                    // Fake page to pass to _updateItems
                    items = new Array(grid.pageSize);
                }

                grid._updateItems(page, items);
            }
        }

        grid.connectorSet = function(index, items) {
            if (index % grid.pageSize != 0) {
                throw "Got new data to index " + index + " which is not aligned with the page size of " + grid.pageSize;
            }

            var firstPage = index / grid.pageSize;
            var updatedPageCount = Math.ceil(items.length / grid.pageSize);

            for (var i = 0; i < updatedPageCount; i++) {
                var page = firstPage + i;
                var slice = items.slice(i * grid.pageSize, (i + 1) * grid.pageSize);
                cache[page] = slice;
                for(var j = 0; j < slice.length; j++) {
                    var item = slice[j]
                    if (item.selected && !selectedKeys[item.key]) {
                        doSelection(item);
                    } else if (selectedKeys[item.key]) {
                        doDeselection(item);
                    }
                }
                updateGridCache(page);
            }
        };

        grid.connectorClear = function(index, length) {
            if (index % grid.pageSize != 0) {
                throw "Got cleared data for index " + index + " which is not aligned with the page size of " + grid.pageSize;
            }

            var firstPage = index / grid.pageSize;
            var updatedPageCount = Math.ceil(length / grid.pageSize);

            for (var i = 0; i < updatedPageCount; i++) {
                var page = firstPage + i;
                var items = cache[page];
                for (var j = 0; j < items.length; j++) {
                    var item = items[j];
                    if (selectedKeys[item.key]) {
                        doDeselection(item);
                    }
                }
                delete cache[page];
                updateGridCache(page);
            }
        };

        grid.connectorUpdateSize = function(newSize) {
            grid.size = newSize;
        };

        grid.connectorConfirm = function(id) {
            // We're done applying changes from this batch, resolve outstanding
            // callbacks
            var outstandingRequests = Object.getOwnPropertyNames(pageCallbacks);
            for(var i = 0; i < outstandingRequests.length; i++) {
                var page = outstandingRequests[i];
                // Resolve if we have data or if we don't expect to get data
                if (cache[page] || page < lastRequestedRange[0] || page > lastRequestedRange[1]) {
                    var callback = pageCallbacks[page];
                    delete pageCallbacks[page];
                    callback(cache[page] || new Array(grid.pageSize));
                }
            }

            // Let server know we're done
            grid.$server.confirmUpdate(id);
        }

        grid.setSelectionMode = function(mode) {
            if ((typeof mode === 'string' || mode instanceof String)
                && validSelectionModes.indexOf(mode) >= 0) {
                selectionMode = mode;
            } else {
                throw 'Attempted to set an invalid selection mode';
            }
        }
    }
}
