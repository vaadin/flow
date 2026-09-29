/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
/* eslint-disable @typescript-eslint/member-ordering */
// https://github.com/vaadin/eslint-config-vaadin/issues/33
import { animationFrame, microTask, timeOut } from './async.js';
import { isSafari } from './browser-utils.js';
import { Debouncer, flush } from './debounce.js';
import { ironList } from './iron-list-core.js';

// Iron-list can by default handle sizes up to around 100000.
// When the size is larger than MAX_VIRTUAL_COUNT _vidxOffset is used
const MAX_VIRTUAL_COUNT = 100000;
const OFFSET_ADJUST_MIN_THRESHOLD = 1000;

export class IronListAdapter {
  constructor({ createElements, updateElement, scrollTarget, scrollContainer, elementsContainer, reorderElements }) {
    this.isAttached = true;
    this._vidxOffset = 0;
    this.createElements = createElements;
    this.updateElement = updateElement;
    this.scrollTarget = scrollTarget;
    this.scrollContainer = scrollContainer;
    this.elementsContainer = elementsContainer || scrollContainer;
    this.reorderElements = reorderElements;
    // Iron-list uses this value to determine how many pages of elements to render
    this._maxPages = 1.3;

    // Placeholder height (used for sizing elements that have intrinsic 0 height after update)
    this.__placeholderHeight = 200;
    // A queue of 10 previous element heights
    this.__elementHeightQueue = Array(10);

    this.timeouts = {
      SCROLL_REORDER: 500,
      IGNORE_WHEEL: 500,
      FIX_INVALID_ITEM_POSITIONING: 100,
    };

    this.__resizeObserver = new ResizeObserver(() => this._resizeHandler());

    if (getComputedStyle(this.scrollTarget).overflow === 'visible') {
      this.scrollTarget.style.overflow = 'auto';
    }

    if (getComputedStyle(this.scrollContainer).position === 'static') {
      this.scrollContainer.style.position = 'relative';
    }

    this.__resizeObserver.observe(this.scrollTarget);
    this.scrollTarget.addEventListener('scroll', () => this._scrollHandler());

    const attachObserver = new ResizeObserver(([{ contentRect }]) => {
      const isHidden = contentRect.width === 0 && contentRect.height === 0;
      if (!isHidden && this.__scrollTargetHidden && this.scrollTarget.scrollTop !== this._scrollPosition) {
        // When removing element from DOM, its scroll position is lost and
        // virtualizer doesn't re-render when adding it to the DOM again.
        // Restore scroll position when the scroll target becomes visible,
        // which is the case e.g. when virtualizer is used inside a dialog.
        this.scrollTarget.scrollTop = this._scrollPosition;
      }

      this.__scrollTargetHidden = isHidden;
    });
    attachObserver.observe(this.scrollTarget);

    this._scrollLineHeight = this._getScrollLineHeight();
    this.scrollTarget.addEventListener('wheel', (e) => this.__onWheel(e));

    this.scrollTarget.addEventListener('virtualizer-element-focused', (e) => this.__onElementFocused(e));
    this.elementsContainer.addEventListener('focusin', () => {
      this.scrollTarget.dispatchEvent(
        new CustomEvent('virtualizer-element-focused', { detail: { element: this.__getFocusedElement() } }),
      );
    });

    if (this.reorderElements) {
      // Reordering the physical elements cancels the user's grab of the scroll bar handle on Safari.
      // Need to defer reordering until the user lets go of the scroll bar handle.
      this.scrollTarget.addEventListener('mousedown', () => {
        this.__mouseDown = true;
      });
      this.scrollTarget.addEventListener('mouseup', () => {
        this.__mouseDown = false;
        if (this.__pendingReorder) {
          this.__reorderElements();
        }
      });
    }
  }

  get scrollOffset() {
    return 0;
  }

  get adjustedFirstVisibleIndex() {
    return this.firstVisibleIndex + this._vidxOffset;
  }

  get adjustedLastVisibleIndex() {
    return this.lastVisibleIndex + this._vidxOffset;
  }

  get _maxVirtualIndexOffset() {
    return this.size - this._virtualCount;
  }

  __hasPlaceholders() {
    return this.__getVisibleElements().some((el) => el.__virtualizerPlaceholder);
  }

  scrollToIndex(index) {
    if (typeof index !== 'number' || isNaN(index) || this.size === 0 || !this.scrollTarget.offsetHeight) {
      return;
    }
    delete this.__pendingScrollToIndex;

    if (this._physicalCount <= 3 /* iron-list-core.DEFAULT_PHYSICAL_COUNT */) {
      // The condition here is a performance improvement to avoid an unnecessary
      // re-render when the physical item pool is already covered.

      // Finish rendering at the current scroll position before scrolling
      this.flush();
    }

    index = this._clamp(index, 0, this.size - 1);

    const visibleElementCount = this.__getVisibleElements().length;
    let targetVirtualIndex = Math.floor((index / this.size) * this._virtualCount);
    if (this._virtualCount - targetVirtualIndex < visibleElementCount) {
      targetVirtualIndex = this._virtualCount - (this.size - index);
      this._vidxOffset = this._maxVirtualIndexOffset;
    } else if (targetVirtualIndex < visibleElementCount) {
      if (index < OFFSET_ADJUST_MIN_THRESHOLD) {
        targetVirtualIndex = index;
        this._vidxOffset = 0;
      } else {
        targetVirtualIndex = OFFSET_ADJUST_MIN_THRESHOLD;
        this._vidxOffset = index - targetVirtualIndex;
      }
    } else {
      this._vidxOffset = index - targetVirtualIndex;
    }

    this.__skipNextVirtualIndexAdjust = true;
    super.scrollToIndex(targetVirtualIndex);

    if (this.adjustedFirstVisibleIndex !== index && this._scrollTop < this._maxScrollTop && !this.grid) {
      // Workaround an iron-list issue by manually adjusting the scroll position
      this._scrollTop -= this.__getIndexScrollOffset(index) || 0;
    }
    this._scrollHandler();

    if (this.__hasPlaceholders()) {
      // After rendering synchronously, there are still placeholders in the DOM.
      // Try again after the next elements update.
      this.__pendingScrollToIndex = index;
    }
  }

  flush() {
    // The scroll target is hidden.
    if (this.scrollTarget.offsetHeight === 0) {
      return;
    }

    this._resizeHandler();
    flush();
    this._scrollHandler();
    if (this.__fixInvalidItemPositioningDebouncer) {
      this.__fixInvalidItemPositioningDebouncer.flush();
    }
    if (this.__scrollReorderDebouncer) {
      this.__scrollReorderDebouncer.flush();
    }
    if (this.__debouncerWheelAnimationFrame) {
      this.__debouncerWheelAnimationFrame.flush();
    }
  }

  update(startIndex = 0, endIndex = this.size - 1) {
    const updatedElements = [];
    this.__getVisibleElements().forEach((el) => {
      if (el.__virtualIndex >= startIndex && el.__virtualIndex <= endIndex) {
        this.__updateElement(el, el.__virtualIndex, true);
        updatedElements.push(el);
      }
    });

    this.__afterElementsUpdated(updatedElements);
  }

  /**
   * Updates the height for a given set of items.
   *
   * @param {!Array<number>=} itemSet
   */
  _updateMetrics(itemSet) {
    // Make sure we distributed all the physical items
    // so we can measure them.
    flush();

    let newPhysicalSize = 0;
    let oldPhysicalSize = 0;
    const prevAvgCount = this._physicalAverageCount;
    const prevPhysicalAvg = this._physicalAverage;

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    this._iterateItems((pidx, vidx) => {
      oldPhysicalSize += this._physicalSizes[pidx];
      this._physicalSizes[pidx] = Math.ceil(this.__getBorderBoxHeight(this._physicalItems[pidx]));
      newPhysicalSize += this._physicalSizes[pidx];
      this._physicalAverageCount += this._physicalSizes[pidx] ? 1 : 0;
    }, itemSet);

    this._physicalSize = this._physicalSize + newPhysicalSize - oldPhysicalSize;

    // Update the average if it measured something.
    if (this._physicalAverageCount !== prevAvgCount) {
      this._physicalAverage = Math.round(
        (prevPhysicalAvg * prevAvgCount + newPhysicalSize) / this._physicalAverageCount,
      );
    }
  }

  __getBorderBoxHeight(el) {
    const style = getComputedStyle(el);

    const itemHeight = parseFloat(style.height) || 0;

    if (style.boxSizing === 'border-box') {
      return itemHeight;
    }

    const paddingBottom = parseFloat(style.paddingBottom) || 0;
    const paddingTop = parseFloat(style.paddingTop) || 0;
    const borderBottomWidth = parseFloat(style.borderBottomWidth) || 0;
    const borderTopWidth = parseFloat(style.borderTopWidth) || 0;

    return itemHeight + paddingBottom + paddingTop + borderBottomWidth + borderTopWidth;
  }

  __updateElement(el, index, forceSameIndexUpdates) {
    // Clean up temporary placeholder sizing
    if (el.__virtualizerPlaceholder) {
      el.style.paddingTop = '';
      el.style.opacity = '';
      el.__virtualizerPlaceholder = false;
    }

    if (!this.__preventElementUpdates && (el.__lastUpdatedIndex !== index || forceSameIndexUpdates)) {
      this.updateElement(el, index);
      el.__lastUpdatedIndex = index;
    }
  }

  /**
   * Called synchronously right after elements have been updated.
   * This is a good place to do any post-update work.
   *
   * @param {!Array<!HTMLElement>} updatedElements
   */
  __afterElementsUpdated(updatedElements) {
    updatedElements.forEach((el) => {
      const elementHeight = el.offsetHeight;
      if (elementHeight === 0) {
        // If the elements have 0 height after update (for example due to lazy rendering),
        // it results in iron-list requesting to create an unlimited count of elements.
        // Assign a temporary placeholder sizing to elements that would otherwise end up having
        // no height.
        el.style.paddingTop = `${this.__placeholderHeight}px`;
        el.style.opacity = '0';
        el.__virtualizerPlaceholder = true;

        // Manually schedule the resize handler to make sure the placeholder padding is
        // cleared in case the resize observer never triggers.
        this.__placeholderClearDebouncer = Debouncer.debounce(this.__placeholderClearDebouncer, animationFrame, () =>
          this._resizeHandler(),
        );
      } else {
        // Add element height to the queue
        this.__elementHeightQueue.push(elementHeight);
        this.__elementHeightQueue.shift();

        // Calculate new placeholder height based on the average of the defined values in the
        // element height queue
        const filteredHeights = this.__elementHeightQueue.filter((h) => h !== undefined);
        this.__placeholderHeight = Math.round(filteredHeights.reduce((a, b) => a + b, 0) / filteredHeights.length);
      }
    });

    if (this.__pendingScrollToIndex !== undefined && !this.__hasPlaceholders()) {
      this.scrollToIndex(this.__pendingScrollToIndex);
    }
  }

  __getIndexScrollOffset(index) {
    const element = this.__getVisibleElements().find((el) => el.__virtualIndex === index);
    return element ? this.scrollTarget.getBoundingClientRect().top - element.getBoundingClientRect().top : undefined;
  }

  get size() {
    return this.__size;
  }

  set size(size) {
    if (size === this.size) {
      return;
    }
    // Cancel active debouncers
    if (this.__fixInvalidItemPositioningDebouncer) {
      this.__fixInvalidItemPositioningDebouncer.cancel();
    }
    if (this._debouncers && this._debouncers._increasePoolIfNeeded) {
      // Avoid creating unnecessary elements on the following flush()
      this._debouncers._increasePoolIfNeeded.cancel();
    }

    // Prevent element update while the scroll position is being restored
    this.__preventElementUpdates = true;

    // Record the scroll position before changing the size
    let fvi; // First visible index
    let fviOffsetBefore; // Scroll offset of the first visible index
    if (size > 0) {
      fvi = this.adjustedFirstVisibleIndex;
      fviOffsetBefore = this.__getIndexScrollOffset(fvi);
    }

    // Change the size
    this.__size = size;

    this._itemsChanged({
      path: 'items',
    });
    flush();

    // Try to restore the scroll position if the new size is larger than 0
    if (size > 0) {
      fvi = Math.min(fvi, size - 1);
      // Note, calling scrollToIndex also updates the virtual index offset,
      // causing the virtualizer to add more items when size is increased,
      // and remove exceeding items when size is decreased.
      this.scrollToIndex(fvi);

      const fviOffsetAfter = this.__getIndexScrollOffset(fvi);
      if (fviOffsetBefore !== undefined && fviOffsetAfter !== undefined) {
        this._scrollTop += fviOffsetBefore - fviOffsetAfter;
      }
    }

    this.__preventElementUpdates = false;

    // When reducing size while invisible, iron-list does not update items, so
    // their hidden state is not updated and their __lastUpdatedIndex is not
    // reset. In that case force an update here.
    if (!this._isVisible) {
      this._assignModels();
    }

    if (!this.elementsContainer.children.length) {
      requestAnimationFrame(() => this._resizeHandler());
    }

    // Schedule and flush a resize handler
    this._resizeHandler();
    flush();
    // Schedule an update to ensure item positions are correct after subsequent size changes
    // Fix for https://github.com/vaadin/flow-components/issues/6269
    this._debounce('_update', this._update, microTask);
  }

  /** @private */
  get _scrollTop() {
    return this.scrollTarget.scrollTop;
  }

  /** @private */
  set _scrollTop(top) {
    this.scrollTarget.scrollTop = top;
  }

  /** @private */
  get items() {
    return {
      length: Math.min(this.size, MAX_VIRTUAL_COUNT),
    };
  }

  /** @private */
  get offsetHeight() {
    return this.scrollTarget.offsetHeight;
  }

  /** @private */
  get $() {
    return {
      items: this.scrollContainer,
    };
  }

  /** @private */
  updateViewportBoundaries() {
    const styles = window.getComputedStyle(this.scrollTarget);
    this._scrollerPaddingTop = this.scrollTarget === this ? 0 : parseInt(styles['padding-top'], 10);
    this._isRTL = Boolean(styles.direction === 'rtl');
    this._viewportWidth = this.elementsContainer.offsetWidth;
    this._viewportHeight = this.scrollTarget.offsetHeight;
    this._scrollPageHeight = this._viewportHeight - this._scrollLineHeight;
    if (this.grid) {
      this._updateGridMetrics();
    }
  }

  /** @private */
  setAttribute() {}

  /** @private */
  _createPool(size) {
    const physicalItems = this.createElements(size);
    const fragment = document.createDocumentFragment();
    physicalItems.forEach((el) => {
      el.style.position = 'absolute';
      fragment.appendChild(el);
      this.__resizeObserver.observe(el);
    });
    this.elementsContainer.appendChild(fragment);
    return physicalItems;
  }

  /** @private */
  _assignModels(itemSet) {
    const updatedElements = [];
    this._iterateItems((pidx, vidx) => {
      const el = this._physicalItems[pidx];
      el.hidden = vidx >= this.size;
      if (!el.hidden) {
        el.__virtualIndex = vidx + (this._vidxOffset || 0);
        this.__updateElement(el, el.__virtualIndex);
        updatedElements.push(el);
      } else {
        delete el.__lastUpdatedIndex;
      }
    }, itemSet);

    this.__afterElementsUpdated(updatedElements);
  }

  /** @private */
  _isClientFull() {
    // Workaround an issue in iron-list that can cause it to freeze on fast scroll
    setTimeout(() => {
      this.__clientFull = true;
    });
    return this.__clientFull || super._isClientFull();
  }

  /** @private */
  translate3d(_x, y, _z, el) {
    el.style.transform = `translateY(${y})`;
  }

  /** @private */
  toggleScrollListener() {}

  /** @private */
  __getFocusedElement(visibleElements = this.__getVisibleElements()) {
    return visibleElements.find(
      (element) =>
        element.contains(this.elementsContainer.getRootNode().activeElement) ||
        element.contains(this.scrollTarget.getRootNode().activeElement),
    );
  }

  /** @private */
  __nextFocusableSiblingMissing(focusedElement, visibleElements) {
    return (
      // Check if focused element is the last visible DOM element
      visibleElements.indexOf(focusedElement) === visibleElements.length - 1 &&
      // ...while there are more items available
      this.size > focusedElement.__virtualIndex + 1
    );
  }

  /** @private */
  __previousFocusableSiblingMissing(focusedElement, visibleElements) {
    return (
      // Check if focused element is the first visible DOM element
      visibleElements.indexOf(focusedElement) === 0 &&
      // ...while there are preceding items available
      focusedElement.__virtualIndex > 0
    );
  }

  /** @private */
  __onElementFocused(e) {
    if (!this.reorderElements) {
      return;
    }

    const focusedElement = e.detail.element;
    if (!focusedElement) {
      return;
    }

    // User has tabbed to or within a virtualizer element.
    // Check if a next or previous focusable sibling is missing while it should be there (so the user can continue tabbing).
    // The focusable sibling might be missing due to the elements not yet being in the correct DOM order.
    // First try flushing (which also flushes any active __scrollReorderDebouncer).
    const visibleElements = this.__getVisibleElements();
    if (
      this.__previousFocusableSiblingMissing(focusedElement, visibleElements) ||
      this.__nextFocusableSiblingMissing(focusedElement, visibleElements)
    ) {
      this.flush();
    }

    // If the focusable sibling is still missing (because the focused element is at the edge of the viewport and
    // the virtual scrolling logic hasn't had the need to recycle elements), scroll the virtualizer just enough to
    // have the focusable sibling inside the visible viewport to force the virtualizer to recycle.
    const reorderedVisibleElements = this.__getVisibleElements();
    if (this.__nextFocusableSiblingMissing(focusedElement, reorderedVisibleElements)) {
      this._scrollTop +=
        Math.ceil(focusedElement.getBoundingClientRect().bottom) -
        Math.floor(this.scrollTarget.getBoundingClientRect().bottom - 1);
      this.flush();
    } else if (this.__previousFocusableSiblingMissing(focusedElement, reorderedVisibleElements)) {
      this._scrollTop -=
        Math.ceil(this.scrollTarget.getBoundingClientRect().top + 1) -
        Math.floor(focusedElement.getBoundingClientRect().top);
      this.flush();
    }
  }

  _scrollHandler() {
    // The scroll target is hidden.
    if (this.scrollTarget.offsetHeight === 0) {
      return;
    }

    this._adjustVirtualIndexOffset(this._scrollTop - (this.__previousScrollTop || 0));
    const delta = this.scrollTarget.scrollTop - this._scrollPosition;

    super._scrollHandler();

    if (this._physicalCount !== 0) {
      const isScrollingDown = delta >= 0;
      const reusables = this._getReusables(!isScrollingDown);

      if (reusables.indexes.length) {
        // After running super._scrollHandler, fix internal properties to workaround an iron-list issue.
        // See https://github.com/vaadin/web-components/issues/1691
        this._physicalTop = reusables.physicalTop;

        if (isScrollingDown) {
          this._virtualStart -= reusables.indexes.length;
          this._physicalStart -= reusables.indexes.length;
        } else {
          this._virtualStart += reusables.indexes.length;
          this._physicalStart += reusables.indexes.length;
        }
        this._resizeHandler();
      }
    }

    if (delta) {
      // There was a change in scroll top. Schedule a check for invalid item positioning.
      this.__fixInvalidItemPositioningDebouncer = Debouncer.debounce(
        this.__fixInvalidItemPositioningDebouncer,
        timeOut.after(this.timeouts.FIX_INVALID_ITEM_POSITIONING),
        () => this.__fixInvalidItemPositioning(),
      );
    }

    if (this.reorderElements) {
      this.__scrollReorderDebouncer = Debouncer.debounce(
        this.__scrollReorderDebouncer,
        timeOut.after(this.timeouts.SCROLL_REORDER),
        () => this.__reorderElements(),
      );
    }

    this.__previousScrollTop = this._scrollTop;

    // If the first visible index is not 0 when scrolled to the top,
    // scroll to index 0 to fix the issue.
    if (this._scrollTop === 0 && this.firstVisibleIndex !== 0 && Math.abs(delta) > 0) {
      this.scrollToIndex(0);
    }
  }

  /**
   * Work around an iron-list issue with invalid item positioning.
   * See https://github.com/vaadin/flow-components/issues/4306
   * @private
   */
  __fixInvalidItemPositioning() {
    if (!this.scrollTarget.isConnected) {
      return;
    }

    // Check if the first physical item element is below the top of the viewport
    const physicalTopBelowTop = this._physicalTop > this._scrollTop;
    // Check if the last physical item element is above the bottom of the viewport
    const physicalBottomAboveBottom = this._physicalBottom < this._scrollBottom;

    // Check if the first index is visible
    const firstIndexVisible = this.adjustedFirstVisibleIndex === 0;
    // Check if the last index is visible
    const lastIndexVisible = this.adjustedLastVisibleIndex === this.size - 1;

    if ((physicalTopBelowTop && !firstIndexVisible) || (physicalBottomAboveBottom && !lastIndexVisible)) {
      // Invalid state! Try to recover.

      const isScrollingDown = physicalBottomAboveBottom;
      // Set the "_ratio" property temporarily to 0 to make iron-list's _getReusables
      // place all the free physical items on one side of the viewport.
      const originalRatio = this._ratio;
      this._ratio = 0;
      // Fake a scroll change to make _scrollHandler place the physical items
      // on the desired side.
      this._scrollPosition = this._scrollTop + (isScrollingDown ? -1 : 1);
      this._scrollHandler();
      // Restore the original "_ratio" value.
      this._ratio = originalRatio;
    }
  }

  /** @private */
  __onWheel(e) {
    if (e.ctrlKey || this._hasScrolledAncestor(e.target, e.deltaX, e.deltaY)) {
      return;
    }

    let deltaY = e.deltaY;
    if (e.deltaMode === WheelEvent.DOM_DELTA_LINE) {
      // Scrolling by "lines of text" instead of pixels
      deltaY *= this._scrollLineHeight;
    } else if (e.deltaMode === WheelEvent.DOM_DELTA_PAGE) {
      // Scrolling by "pages" instead of pixels
      deltaY *= this._scrollPageHeight;
    }

    if (!this._deltaYAcc) {
      this._deltaYAcc = 0;
    }

    if (this._wheelAnimationFrame) {
      // Accumulate wheel delta while a frame is being processed
      this._deltaYAcc += deltaY;
      e.preventDefault();
      return;
    }

    deltaY += this._deltaYAcc;
    this._deltaYAcc = 0;

    this._wheelAnimationFrame = true;
    this.__debouncerWheelAnimationFrame = Debouncer.debounce(
      this.__debouncerWheelAnimationFrame,
      animationFrame,
      () => {
        this._wheelAnimationFrame = false;
      },
    );

    const momentum = Math.abs(e.deltaX) + Math.abs(deltaY);

    if (this._canScroll(this.scrollTarget, e.deltaX, deltaY)) {
      e.preventDefault();
      this.scrollTarget.scrollTop += deltaY;
      this.scrollTarget.scrollLeft += e.deltaX;

      this._hasResidualMomentum = true;

      this._ignoreNewWheel = true;
      this._debouncerIgnoreNewWheel = Debouncer.debounce(
        this._debouncerIgnoreNewWheel,
        timeOut.after(this.timeouts.IGNORE_WHEEL),
        () => {
          this._ignoreNewWheel = false;
        },
      );
    } else if ((this._hasResidualMomentum && momentum <= this._previousMomentum) || this._ignoreNewWheel) {
      e.preventDefault();
    } else if (momentum > this._previousMomentum) {
      this._hasResidualMomentum = false;
    }
    this._previousMomentum = momentum;
  }

  /**
   * Determines if the element has an ancestor that handles the scroll delta prior to this
   *
   * @private
   */
  _hasScrolledAncestor(el, deltaX, deltaY) {
    if (el === this.scrollTarget || el === this.scrollTarget.getRootNode().host) {
      return false;
    } else if (
      this._canScroll(el, deltaX, deltaY) &&
      ['auto', 'scroll'].indexOf(getComputedStyle(el).overflow) !== -1
    ) {
      return true;
    } else if (el !== this && el.parentElement) {
      return this._hasScrolledAncestor(el.parentElement, deltaX, deltaY);
    }
  }

  _canScroll(el, deltaX, deltaY) {
    return (
      (deltaY > 0 && el.scrollTop < el.scrollHeight - el.offsetHeight) ||
      (deltaY < 0 && el.scrollTop > 0) ||
      (deltaX > 0 && el.scrollLeft < el.scrollWidth - el.offsetWidth) ||
      (deltaX < 0 && el.scrollLeft > 0)
    );
  }

  /**
   * Increases the pool size.
   * @override
   */
  _increasePoolIfNeeded(count) {
    if (this._physicalCount > 2 && count) {
      // The iron-list logic has already created some physical items and
      // has decided to create more. Since each item creation round is
      // expensive, let's try to create the remaining items in one go.

      // Calculate the total item count that would be needed to fill the viewport
      // plus the buffer assuming rest of the items to be of the average size
      // of the items already created.
      const totalItemCount = Math.ceil(this._optPhysicalSize / this._physicalAverage);
      const missingItemCount = totalItemCount - this._physicalCount;
      // Create the remaining items in one go. Use a maximum of 100 items
      // as a safety measure.
      super._increasePoolIfNeeded(Math.max(count, Math.min(100, missingItemCount)));
    } else {
      super._increasePoolIfNeeded(count);
    }
  }

  /**
   * An optimal physical size such that we will have enough physical items
   * to fill up the viewport and recycle when the user scrolls.
   *
   * This default value assumes that we will at least have the equivalent
   * to a viewport of physical items above and below the user's viewport.
   * @override
   */
  get _optPhysicalSize() {
    const optPhysicalSize = super._optPhysicalSize;
    // No need to adjust
    if (optPhysicalSize <= 0 || this.__hasPlaceholders()) {
      return optPhysicalSize;
    }
    // Item height buffer accounts for the cases where some items are much larger than the average.
    // This can lead to some items not being rendered and leaving empty space in the viewport.
    // https://github.com/vaadin/flow-components/issues/6651
    return optPhysicalSize + this.__getItemHeightBuffer();
  }

  /**
   * Extra item height buffer used when calculating optimal physical size.
   *
   * The iron list core uses the optimal physical size when determining whether to increase the item pool.
   * For the cases where some items are much larger than the average, the iron list core might not increase item pool.
   * This can lead to the large item not being rendered.
   *
   * @returns {Number} - Extra item height buffer
   * @private
   */
  __getItemHeightBuffer() {
    // No need for a buffer with no items
    if (this._physicalCount === 0) {
      return 0;
    }
    // The regular buffer zone height for either top or bottom
    const bufferZoneHeight = Math.ceil((this._viewportHeight * (this._maxPages - 1)) / 2);
    // The maximum height of the currently rendered items
    const maxItemHeight = Math.max(...this._physicalSizes);
    // Only add buffer if the item is larger that the other items
    if (maxItemHeight > Math.min(...this._physicalSizes)) {
      // Add a buffer height since the large item can still be in the viewport and out of the original buffer
      return Math.max(0, maxItemHeight - bufferZoneHeight);
    }
    return 0;
  }

  /**
   * @returns {Number|undefined} - The browser's default font-size in pixels
   * @private
   */
  _getScrollLineHeight() {
    const el = document.createElement('div');
    el.style.fontSize = 'initial';
    el.style.display = 'none';
    document.body.appendChild(el);
    const fontSize = window.getComputedStyle(el).fontSize;
    document.body.removeChild(el);
    return fontSize ? window.parseInt(fontSize) : undefined;
  }

  __getVisibleElements() {
    return Array.from(this.elementsContainer.children).filter((element) => !element.hidden);
  }

  /** @private */
  __reorderElements() {
    if (this.__mouseDown) {
      this.__pendingReorder = true;
      return;
    }
    this.__pendingReorder = false;

    const adjustedVirtualStart = this._virtualStart + (this._vidxOffset || 0);

    // Which row to use as a target?
    const visibleElements = this.__getVisibleElements();
    const targetElement = this.__getFocusedElement(visibleElements) || visibleElements[0];
    if (!targetElement) {
      // All elements are hidden, don't reorder
      return;
    }

    // Where the target row should be?
    const targetPhysicalIndex = targetElement.__virtualIndex - adjustedVirtualStart;

    // Reodrer the DOM elements to keep the target row at the target physical index
    const delta = visibleElements.indexOf(targetElement) - targetPhysicalIndex;
    if (delta > 0) {
      for (let i = 0; i < delta; i++) {
        this.elementsContainer.appendChild(visibleElements[i]);
      }
    } else if (delta < 0) {
      for (let i = visibleElements.length + delta; i < visibleElements.length; i++) {
        this.elementsContainer.insertBefore(visibleElements[i], visibleElements[0]);
      }
    }

    // Due to a rendering bug, reordering the rows can make parts of the scroll target disappear
    // on Safari when using sticky positioning in case the scroll target is inside a flexbox.
    // This issue manifests with grid (the header can disappear if grid is used inside a flexbox)
    if (isSafari) {
      const { transform } = this.scrollTarget.style;
      this.scrollTarget.style.transform = 'translateZ(0)';
      setTimeout(() => {
        this.scrollTarget.style.transform = transform;
      });
    }
  }

  /** @private */
  _adjustVirtualIndexOffset(delta) {
    const maxOffset = this._maxVirtualIndexOffset;

    if (this._virtualCount >= this.size) {
      this._vidxOffset = 0;
    } else if (this.__skipNextVirtualIndexAdjust) {
      this.__skipNextVirtualIndexAdjust = false;
    } else if (Math.abs(delta) > 10000) {
      // Process a large scroll position change
      const scale = this._scrollTop / (this.scrollTarget.scrollHeight - this.scrollTarget.clientHeight);
      this._vidxOffset = Math.round(scale * maxOffset);
    } else {
      // Make sure user can always swipe/wheel scroll to the start and end
      const oldOffset = this._vidxOffset;
      const threshold = OFFSET_ADJUST_MIN_THRESHOLD;
      const maxShift = 100;

      // Near start
      if (this._scrollTop === 0) {
        this._vidxOffset = 0;
        if (oldOffset !== this._vidxOffset) {
          super.scrollToIndex(0);
        }
      } else if (this.firstVisibleIndex < threshold && this._vidxOffset > 0) {
        this._vidxOffset -= Math.min(this._vidxOffset, maxShift);
        super.scrollToIndex(this.firstVisibleIndex + (oldOffset - this._vidxOffset));
      }

      // Near end
      if (this._scrollTop >= this._maxScrollTop && this._maxScrollTop > 0) {
        this._vidxOffset = maxOffset;
        if (oldOffset !== this._vidxOffset) {
          super.scrollToIndex(this._virtualCount - 1);
        }
      } else if (this.firstVisibleIndex > this._virtualCount - threshold && this._vidxOffset < maxOffset) {
        this._vidxOffset += Math.min(maxOffset - this._vidxOffset, maxShift);
        super.scrollToIndex(this.firstVisibleIndex - (this._vidxOffset - oldOffset));
      }
    }
  }
}

Object.setPrototypeOf(IronListAdapter.prototype, ironList);
