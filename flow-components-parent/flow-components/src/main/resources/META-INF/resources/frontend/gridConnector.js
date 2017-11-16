window.gridConnector = {
  initLazy: function(grid) {
    const extraPageBuffer = 2;
    const pageCallbacks = {};
    const cache = {};
    let lastRequestedRange = [0, 0];

    const validSelectionModes = ['SINGLE', 'NONE', 'MULTI'];
    let selectedKeys = {};
    let selectionMode = 'SINGLE';

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
      if (selectionMode != 'SINGLE') {
        return;
      }
      if (!newVal) {
        if (oldVal && selectedKeys[oldVal.key]) {
          grid.$connector.doDeselection(oldVal, true);
        }
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
    }
    grid._createPropertyObserver('activeItem', '__activeItemChangedDetails', true);

    grid.dataProvider = function(params, callback) {
      if (params.pageSize != grid.pageSize) {
        throw 'Invalid pageSize';
      }

      const page = params.page;
      if (cache[page]) {
        callback(cache[page]);
      } else {
        pageCallbacks[page] = callback;
      }
      // Determine what to fetch based on scroll position and not only
      // what grid asked for
      let firstNeededPage = Math.min(page, grid._getPageForIndex(grid._virtualStart));
      let lastNeededPage = Math.max(page, grid._getPageForIndex(grid._virtualEnd));

      let first = Math.max(0,  firstNeededPage - extraPageBuffer);
      let last = Math.min(lastNeededPage + extraPageBuffer, Math.max(0, Math.floor(grid.size / grid.pageSize) + 1));

      if (lastRequestedRange[0] != first || lastRequestedRange[1] != last) {
        lastRequestedRange = [first, last];
        let count = 1 + last - first;
        grid.$server.setRequestedRange(first * grid.pageSize, count * grid.pageSize);
      }
    }

    const sorterChangeListener = function(event) {
      grid.$server.sortersChanged(grid._sorters.map(function(sorter) {
        return {
          path: sorter.path,
          direction: sorter.direction
        };
      }));
    }
    grid.addEventListener('sorter-changed', sorterChangeListener);

    const itemsUpdated = function(items) {
      if (!items || !(items instanceof Array)) {
        throw 'Attempted to call itemsUpdated with an invalid value';
      }
      const detailsOpenedItems = [];
      for (let i = 0; i < items.length; ++i) {
        if (items[i].detailsOpened) {
          detailsOpenedItems.push(items[i]);
        }
      }
      grid.detailsOpenedItems = detailsOpenedItems;
    }

    const updateGridCache = function(page) {
      const items = cache[page];
      // Force update unless there's a callback waiting
      if (!pageCallbacks[page]) {
        let rangeStart = page * grid.pageSize;
        let rangeEnd = rangeStart + grid.pageSize;
        if (!items) {
          for (let idx = rangeStart; idx < rangeEnd; idx++) {
            delete grid._cache.items[idx];
          }
        }
        else {
          for (let idx = rangeStart; idx < rangeEnd; idx++) {
            if (grid._cache.items[idx]) {
              grid._cache.items[idx] = items[idx - rangeStart];
            }
          }
          itemsUpdated(items);
        }
        /**
         * Calls the _assignModels function from GridScrollerElement, that triggers
         * the internal revalidation of the items based on the _cache of the DataProviderMixin.
         */
        grid._assignModels();
      }
    }

    grid.$connector.set = function(index, items) {
      if (index % grid.pageSize != 0) {
        throw 'Got new data to index ' + index + ' which is not aligned with the page size of ' + grid.pageSize;
      }

      const firstPage = index / grid.pageSize;
      const updatedPageCount = Math.ceil(items.length / grid.pageSize);

      for (let i = 0; i < updatedPageCount; i++) {
        let page = firstPage + i;
        let slice = items.slice(i * grid.pageSize, (i + 1) * grid.pageSize);
        cache[page] = slice;
        for(let j = 0; j < slice.length; j++) {
          let item = slice[j]
          if (item.selected && !selectedKeys[item.key]) {
            grid.$connector.doSelection(item);
          } else if (selectedKeys[item.key]) {
            grid.$connector.doDeselection(item);
          }
        }
        updateGridCache(page);
      }
    };

    const itemToCacheLocation = function(itemKey) {
      for (let page in cache) {
        for (let index in cache[page]) {
          if (cache[page][index].key === itemKey) {
            return {page: page, index: index};
          }
        }
      }
      return null;
    }

    grid.$connector.updateData = function(items) {
      let pagesToUpdate = [];
      for (let i = 0; i < items.length; i++) {
        let cacheLocation = itemToCacheLocation(items[i].key);
        if (cacheLocation) {
          cache[cacheLocation.page][cacheLocation.index] = items[i];
          if (pagesToUpdate.indexOf(cacheLocation.page) === -1) {
            pagesToUpdate.push(cacheLocation.page);
          }
        }
      }
      for (let page in pagesToUpdate) {
        updateGridCache(page);
      }
    };

    grid.$connector.clear = function(index, length) {
      if (index % grid.pageSize != 0) {
        throw 'Got cleared data for index ' + index + ' which is not aligned with the page size of ' + grid.pageSize;
      }

      let firstPage = index / grid.pageSize;
      let updatedPageCount = Math.ceil(length / grid.pageSize);

      for (let i = 0; i < updatedPageCount; i++) {
        let page = firstPage + i;
        let items = cache[page];
        for (let j = 0; j < items.length; j++) {
          let item = items[j];
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
      let outstandingRequests = Object.getOwnPropertyNames(pageCallbacks);
      for(let i = 0; i < outstandingRequests.length; i++) {
        let page = outstandingRequests[i];
        // Resolve if we have data or if we don't expect to get data
        if (cache[page] || page < lastRequestedRange[0] || page > lastRequestedRange[1]) {
          let callback = pageCallbacks[page];
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
        selectedKeys = {};
      } else {
        throw 'Attempted to set an invalid selection mode';
      }
    }
  }
}
