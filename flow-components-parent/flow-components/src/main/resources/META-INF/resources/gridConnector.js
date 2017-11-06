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

        grid.$connector = {};

        grid.$connector.doSelection = function(item, userOriginated) {
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
                item.selected = true;
                grid.$server.select(item.key);
            }
            grid.fire('select', {item: item, userOriginated: userOriginated});
        };

        grid.$connector.doDeselection = function(item, userOriginated) {
            if (selectionMode === 'SINGLE' || selectionMode === 'MULTI') {
                grid.deselectItem(item);
                delete selectedKeys[item.key];
                if (userOriginated) {
                    delete item.selected;
                    grid.$server.deselect(item.key);
                }
                grid.fire('deselect', {item: item, userOriginated: userOriginated});
            }
        };

        grid.__activeItemChanged = function(newVal, oldVal) {
            if (!newVal || selectionMode != 'SINGLE') {
                return;
            }
            if (!selectedKeys[newVal.key]) {
                grid.$connector.doSelection(newVal, true);
            } else {
                grid.$connector.doDeselection(newVal, true);
            }
        };
        grid._createPropertyObserver('activeItem', '__activeItemChanged', true);

        grid.__activeItemChangedDetails = function(newVal, oldVal) {
            grid.detailsOpenedItems = [newVal];
            grid.$server.setDetailsVisible(newVal ? newVal.key : null);
        }
        grid._createPropertyObserver('activeItem', '__activeItemChangedDetails', true);

        grid.dataProvider = function(params, callback) {
            if (params.pageSize != grid.pageSize) { 
                throw 'Invalid pageSize'; 
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
                grid.$server.setRequestedRange(first * grid.pageSize, count * grid.pageSize);
            }
        }

        var itemsUpdated = function(items) {
            if (!items || !(items instanceof Array)) {
                throw 'Attempted to call itemsUpdated with an invalid value';
            }
            var detailsOpenedItems = [];
            for (var i = 0; i < items.length; ++i) {
                if (items[i].detailsOpened) {
                    detailsOpenedItems.push(items[i]);
                }
            }
            grid.detailsOpenedItems = detailsOpenedItems;
        }

        var updateGridCache = function(page) {
            var items = cache[page];
            // Force update unless there's a callback waiting
            if (!pageCallbacks[page]) {
                if (!items) {
                    delete grid._cache[page];
                    // Fake page to pass to _updateItems
                    items = new Array(grid.pageSize);
                }
                else if (grid._cache[page]){
                    grid._cache[page] = items;
                }
                grid._updateItems(page, items);
                itemsUpdated(items);
            }
        }

        grid.$connector.set = function(index, items) {
            if (index % grid.pageSize != 0) {
                throw 'Got new data to index ' + index + ' which is not aligned with the page size of ' + grid.pageSize;
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
                        grid.$connector.doSelection(item);
                    } else if (selectedKeys[item.key]) {
                        grid.$connector.doDeselection(item);
                    }
                }
                updateGridCache(page);
            }
        };

        var itemToCacheLocation = function(itemKey) {
            for (var page in cache) {
                for (var index in cache[page]) {
                    if (cache[page][index].key === itemKey) {
                        return {page: page, index: index};
                    }
                }
            }
            return null;
        }

        grid.$connector.updateData = function(items) {
            var pagesToUpdate = [];
            for (var i = 0; i < items.length; i++) {
                var cacheLocation = itemToCacheLocation(items[i].key);
                if (cacheLocation) {
                    cache[cacheLocation.page][cacheLocation.index] = items[i];
                    if (pagesToUpdate.indexOf(cacheLocation.page) === -1) {
                        pagesToUpdate.push(cacheLocation.page);
                    }
                }
            }
            for (var page in pagesToUpdate) {
                updateGridCache(page);
            }
        };

        grid.$connector.clear = function(index, length) {
            if (index % grid.pageSize != 0) {
                throw 'Got cleared data for index ' + index + ' which is not aligned with the page size of ' + grid.pageSize;
            }

            var firstPage = index / grid.pageSize;
            var updatedPageCount = Math.ceil(length / grid.pageSize);

            for (var i = 0; i < updatedPageCount; i++) {
                var page = firstPage + i;
                var items = cache[page];
                for (var j = 0; j < items.length; j++) {
                    var item = items[j];
                    if (selectedKeys[item.key]) {
                        grid.$connector.doDeselection(item);
                    }
                }
                delete cache[page];
                updateGridCache(page);
            }
        };

        grid.$connector.updateSize = function(newSize) {
            grid.size = newSize;
        };

        grid.$connector.confirm = function(id) {
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

        grid.$connector.setSelectionMode = function(mode) {
            if ((typeof mode === 'string' || mode instanceof String)
                && validSelectionModes.indexOf(mode) >= 0) {
                selectionMode = mode;
            } else {
                throw 'Attempted to set an invalid selection mode';
            }
        }
    }
}
