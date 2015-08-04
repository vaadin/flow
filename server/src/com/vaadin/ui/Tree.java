/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.ui;

import java.awt.dnd.DragSource;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.jsoup.nodes.Element;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.ContainerHierarchicalWrapper;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ItemClickEvent.ItemClickNotifier;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.MultiSelectMode;
import com.vaadin.shared.ui.tree.TreeConstants;
import com.vaadin.ui.declarative.DesignAttributeHandler;
import com.vaadin.ui.declarative.DesignContext;
import com.vaadin.ui.declarative.DesignException;
import com.vaadin.util.ReflectTools;

/**
 * Tree component. A Tree can be used to select an item (or multiple items) from
 * a hierarchical set of items.
 * 
 * @author Vaadin Ltd.
 * @since 3.0
 */
@SuppressWarnings({ "serial", "deprecation" })
public class Tree extends AbstractSelect implements Container.Hierarchical, ItemClickNotifier {

    /* Private members */

    private static final String NULL_ALT_EXCEPTION_MESSAGE = "Parameter 'altText' needs to be non null";

    /**
     * Item icons alt texts.
     */
    private final HashMap<Object, String> itemIconAlts = new HashMap<Object, String>();

    /**
     * Set of expanded nodes.
     */
    private HashSet<Object> expanded = new HashSet<Object>();

    /**
     * Is the tree selectable on the client side.
     */
    private boolean selectable = true;

    /**
     * Flag to indicate sub-tree loading
     */
    private boolean partialUpdate = false;

    /**
     * Holds a itemId which was recently expanded
     */
    private Object expandedItemId;

    /**
     * a flag which indicates initial paint. After this flag set true partial
     * updates are allowed.
     */
    private boolean initialPaint = true;

    /**
     * Item tooltip generator
     */
    private ItemDescriptionGenerator itemDescriptionGenerator;

    /**
     * Supported drag modes for Tree.
     */
    public enum TreeDragMode {
        /**
         * When drag mode is NONE, dragging from Tree is not supported. Browsers
         * may still support selecting text/icons from Tree which can initiate
         * HTML 5 style drag and drop operation.
         */
        NONE,
        /**
         * When drag mode is NODE, users can initiate drag from Tree nodes that
         * represent {@link Item}s in from the backed {@link Container}.
         */
        NODE
        // , SUBTREE
    }

    private TreeDragMode dragMode = TreeDragMode.NONE;

    private MultiSelectMode multiSelectMode = MultiSelectMode.DEFAULT;

    /* Tree constructors */

    /**
     * Creates a new empty tree.
     */
    public Tree() {
        this(null);
    }

    /**
     * Creates a new empty tree with caption.
     * 
     * @param caption
     */
    public Tree(String caption) {
        this(caption, new HierarchicalContainer());
    }

    /**
     * Creates a new tree with caption and connect it to a Container.
     * 
     * @param caption
     * @param dataSource
     */
    public Tree(String caption, Container dataSource) {
        super(caption, dataSource);
    }

    @Override
    public void setItemIcon(Object itemId, Resource icon) {
        setItemIcon(itemId, icon, "");
    }

    /**
     * Sets the icon for an item.
     * 
     * @param itemId
     *            the id of the item to be assigned an icon.
     * @param icon
     *            the icon to use or null.
     * 
     * @param altText
     *            the alternative text for the icon
     */
    public void setItemIcon(Object itemId, Resource icon, String altText) {
        if (itemId != null) {
            super.setItemIcon(itemId, icon);

            if (icon == null) {
                itemIconAlts.remove(itemId);
            } else if (altText == null) {
                throw new IllegalArgumentException(NULL_ALT_EXCEPTION_MESSAGE);
            } else {
                itemIconAlts.put(itemId, altText);
            }
            markAsDirty();
        }
    }

    /**
     * Set the alternate text for an item.
     * 
     * Used when the item has an icon.
     * 
     * @param itemId
     *            the id of the item to be assigned an icon.
     * @param altText
     *            the alternative text for the icon
     */
    public void setItemIconAlternateText(Object itemId, String altText) {
        if (itemId != null) {
            if (altText == null) {
                throw new IllegalArgumentException(NULL_ALT_EXCEPTION_MESSAGE);
            } else {
                itemIconAlts.put(itemId, altText);
            }
        }
    }

    /**
     * Return the alternate text of an icon in a tree item.
     * 
     * @param itemId
     *            Object with the ID of the item
     * @return String with the alternate text of the icon, or null when no icon
     *         was set
     */
    public String getItemIconAlternateText(Object itemId) {
        String storedAlt = itemIconAlts.get(itemId);
        return storedAlt == null ? "" : storedAlt;
    }

    /* Expanding and collapsing */

    /**
     * Check is an item is expanded
     * 
     * @param itemId
     *            the item id.
     * @return true iff the item is expanded.
     */
    public boolean isExpanded(Object itemId) {
        return expanded.contains(itemId);
    }

    /**
     * Expands an item.
     * 
     * @param itemId
     *            the item id.
     * @return True iff the expand operation succeeded
     */
    public boolean expandItem(Object itemId) {
        boolean success = expandItem(itemId, true);
        markAsDirty();
        return success;
    }

    /**
     * Expands an item.
     * 
     * @param itemId
     *            the item id.
     * @param sendChildTree
     *            flag to indicate if client needs subtree or not (may be
     *            cached)
     * @return True if the expand operation succeeded
     */
    private boolean expandItem(Object itemId, boolean sendChildTree) {

        // Succeeds if the node is already expanded
        if (isExpanded(itemId)) {
            return true;
        }

        // Nodes that can not have children are not expandable
        if (!areChildrenAllowed(itemId)) {
            return false;
        }

        // Expands
        expanded.add(itemId);

        expandedItemId = itemId;
        if (initialPaint) {
            markAsDirty();
        } else if (sendChildTree) {
            requestPartialRepaint();
        }
        fireExpandEvent(itemId);

        return true;
    }

    @Override
    public void markAsDirty() {
        super.markAsDirty();
        partialUpdate = false;
    }

    private void requestPartialRepaint() {
        super.markAsDirty();
        partialUpdate = true;
    }

    /**
     * Expands the items recursively
     * 
     * Expands all the children recursively starting from an item. Operation
     * succeeds only if all expandable items are expanded.
     * 
     * @param startItemId
     * @return True iff the expand operation succeeded
     */
    public boolean expandItemsRecursively(Object startItemId) {

        boolean result = true;

        // Initial stack
        final Stack<Object> todo = new Stack<Object>();
        todo.add(startItemId);

        // Expands recursively
        while (!todo.isEmpty()) {
            final Object id = todo.pop();
            if (areChildrenAllowed(id) && !expandItem(id, false)) {
                result = false;
            }
            if (hasChildren(id)) {
                todo.addAll(getChildren(id));
            }
        }
        markAsDirty();
        return result;
    }

    /**
     * Collapses an item.
     * 
     * @param itemId
     *            the item id.
     * @return True iff the collapse operation succeeded
     */
    public boolean collapseItem(Object itemId) {

        // Succeeds if the node is already collapsed
        if (!isExpanded(itemId)) {
            return true;
        }

        // Collapse
        expanded.remove(itemId);
        markAsDirty();
        fireCollapseEvent(itemId);

        return true;
    }

    /**
     * Collapses the items recursively.
     * 
     * Collapse all the children recursively starting from an item. Operation
     * succeeds only if all expandable items are collapsed.
     * 
     * @param startItemId
     * @return True iff the collapse operation succeeded
     */
    public boolean collapseItemsRecursively(Object startItemId) {

        boolean result = true;

        // Initial stack
        final Stack<Object> todo = new Stack<Object>();
        todo.add(startItemId);

        // Collapse recursively
        while (!todo.isEmpty()) {
            final Object id = todo.pop();
            if (areChildrenAllowed(id) && !collapseItem(id)) {
                result = false;
            }
            if (hasChildren(id)) {
                todo.addAll(getChildren(id));
            }
        }

        return result;
    }

    /**
     * Returns the current selectable state. Selectable determines if the a node
     * can be selected on the client side. Selectable does not affect
     * {@link #setValue(Object)} or {@link #select(Object)}.
     * 
     * <p>
     * The tree is selectable by default.
     * </p>
     * 
     * @return the current selectable state.
     */
    public boolean isSelectable() {
        return selectable;
    }

    /**
     * Sets the selectable state. Selectable determines if the a node can be
     * selected on the client side. Selectable does not affect
     * {@link #setValue(Object)} or {@link #select(Object)}.
     * 
     * <p>
     * The tree is selectable by default.
     * </p>
     * 
     * @param selectable
     *            The new selectable state.
     */
    public void setSelectable(boolean selectable) {
        if (this.selectable != selectable) {
            this.selectable = selectable;
            markAsDirty();
        }
    }

    /**
     * Sets the behavior of the multiselect mode
     * 
     * @param mode
     *            The mode to set
     */
    public void setMultiselectMode(MultiSelectMode mode) {
        if (multiSelectMode != mode && mode != null) {
            multiSelectMode = mode;
            markAsDirty();
        }
    }

    /**
     * Returns the mode the multiselect is in. The mode controls how
     * multiselection can be done.
     * 
     * @return The mode
     */
    public MultiSelectMode getMultiselectMode() {
        return multiSelectMode;
    }

    /* Component API */

    /**
     * Handles the selection
     * 
     * @param variables
     *            The variables sent to the server from the client
     */
    private void handleSelectedItems(Map<String, Object> variables) {
        final String[] ka = (String[]) variables.get("selected");

        // Converts the key-array to id-set
        final LinkedList<Object> s = new LinkedList<Object>();
        for (int i = 0; i < ka.length; i++) {
            final Object id = itemIdMapper.get(ka[i]);
            if (!isNullSelectionAllowed() && (id == null || id == getNullSelectionItemId())) {
                // skip empty selection if nullselection is not allowed
                markAsDirty();
            } else if (id != null && containsId(id)) {
                s.add(id);
            }
        }

        if (!isNullSelectionAllowed() && s.size() < 1) {
            // empty selection not allowed, keep old value
            markAsDirty();
            return;
        }

        setValue(s, true);
    }

    /* Container.Hierarchical API */

    /**
     * Tests if the Item with given ID can have any children.
     * 
     * @see com.vaadin.data.Container.Hierarchical#areChildrenAllowed(Object)
     */
    @Override
    public boolean areChildrenAllowed(Object itemId) {
        return ((Container.Hierarchical) items).areChildrenAllowed(itemId);
    }

    /**
     * Gets the IDs of all Items that are children of the specified Item.
     * 
     * @see com.vaadin.data.Container.Hierarchical#getChildren(Object)
     */
    @Override
    public Collection<?> getChildren(Object itemId) {
        return ((Container.Hierarchical) items).getChildren(itemId);
    }

    /**
     * Gets the ID of the parent Item of the specified Item.
     * 
     * @see com.vaadin.data.Container.Hierarchical#getParent(Object)
     */
    @Override
    public Object getParent(Object itemId) {
        return ((Container.Hierarchical) items).getParent(itemId);
    }

    /**
     * Tests if the Item specified with <code>itemId</code> has child Items.
     * 
     * @see com.vaadin.data.Container.Hierarchical#hasChildren(Object)
     */
    @Override
    public boolean hasChildren(Object itemId) {
        return ((Container.Hierarchical) items).hasChildren(itemId);
    }

    /**
     * Tests if the Item specified with <code>itemId</code> is a root Item.
     * 
     * @see com.vaadin.data.Container.Hierarchical#isRoot(Object)
     */
    @Override
    public boolean isRoot(Object itemId) {
        return ((Container.Hierarchical) items).isRoot(itemId);
    }

    /**
     * Gets the IDs of all Items in the container that don't have a parent.
     * 
     * @see com.vaadin.data.Container.Hierarchical#rootItemIds()
     */
    @Override
    public Collection<?> rootItemIds() {
        return ((Container.Hierarchical) items).rootItemIds();
    }

    /**
     * Sets the given Item's capability to have children.
     * 
     * @see com.vaadin.data.Container.Hierarchical#setChildrenAllowed(Object,
     *      boolean)
     */
    @Override
    public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) {
        final boolean success = ((Container.Hierarchical) items).setChildrenAllowed(itemId, areChildrenAllowed);
        if (success) {
            markAsDirty();
        }
        return success;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.data.Container.Hierarchical#setParent(java.lang.Object ,
     * java.lang.Object)
     */
    @Override
    public boolean setParent(Object itemId, Object newParentId) {
        final boolean success = ((Container.Hierarchical) items).setParent(itemId, newParentId);
        if (success) {
            markAsDirty();
        }
        return success;
    }

    /* Overriding select behavior */

    /**
     * Sets the Container that serves as the data source of the viewer.
     * 
     * @see com.vaadin.data.Container.Viewer#setContainerDataSource(Container)
     */
    @Override
    public void setContainerDataSource(Container newDataSource) {
        if (newDataSource == null) {
            newDataSource = new HierarchicalContainer();
        }

        // Assure that the data source is ordered by making unordered
        // containers ordered by wrapping them
        if (Container.Hierarchical.class.isAssignableFrom(newDataSource.getClass())) {
            super.setContainerDataSource(newDataSource);
        } else {
            super.setContainerDataSource(new ContainerHierarchicalWrapper(newDataSource));
        }

        /*
         * Ensure previous expanded items are cleaned up if they don't exist in
         * the new container
         */
        if (expanded != null) {
            /*
             * We need to check that the expanded-field is not null since
             * setContainerDataSource() is called from the parent constructor
             * (AbstractSelect()) and at that time the expanded field is not yet
             * initialized.
             */
            cleanupExpandedItems();
        }

    }

    @Override
    public void containerItemSetChange(com.vaadin.data.Container.ItemSetChangeEvent event) {
        super.containerItemSetChange(event);
        if (getContainerDataSource() instanceof Filterable) {
            boolean hasFilters = !((Filterable) getContainerDataSource()).getContainerFilters().isEmpty();
            if (!hasFilters) {
                /*
                 * If Container is not filtered then the itemsetchange is caused
                 * by either adding or removing items to the container. To
                 * prevent a memory leak we should cleanup the expanded list
                 * from items which was removed.
                 * 
                 * However, there will still be a leak if the container is
                 * filtered to show only a subset of the items in the tree and
                 * later unfiltered items are removed from the container. In
                 * that case references to the unfiltered item ids will remain
                 * in the expanded list until the Tree instance is removed and
                 * the list is destroyed, or the container data source is
                 * replaced/updated. To force the removal of the removed items
                 * the application developer needs to a) remove the container
                 * filters temporarly or b) re-apply the container datasource
                 * using setContainerDataSource(getContainerDataSource())
                 */
                cleanupExpandedItems();
            }
        }

    }

    /* Expand event and listener */

    /**
     * Event to fired when a node is expanded. ExapandEvent is fired when a node
     * is to be expanded. it can me used to dynamically fill the sub-nodes of
     * the node.
     * 
     * @author Vaadin Ltd.
     * @since 3.0
     */
    public static class ExpandEvent extends Component.Event {

        private final Object expandedItemId;

        /**
         * New instance of options change event
         * 
         * @param source
         *            the Source of the event.
         * @param expandedItemId
         */
        public ExpandEvent(Component source, Object expandedItemId) {
            super(source);
            this.expandedItemId = expandedItemId;
        }

        /**
         * Node where the event occurred.
         * 
         * @return the Source of the event.
         */
        public Object getItemId() {
            return expandedItemId;
        }
    }

    /**
     * Expand event listener.
     * 
     * @author Vaadin Ltd.
     * @since 3.0
     */
    public interface ExpandListener extends Serializable {

        public static final Method EXPAND_METHOD = ReflectTools.findMethod(ExpandListener.class, "nodeExpand", ExpandEvent.class);

        /**
         * A node has been expanded.
         * 
         * @param event
         *            the Expand event.
         */
        public void nodeExpand(ExpandEvent event);
    }

    /**
     * Adds the expand listener.
     * 
     * @param listener
     *            the Listener to be added.
     */
    public void addExpandListener(ExpandListener listener) {
        addListener(ExpandEvent.class, listener, ExpandListener.EXPAND_METHOD);
    }

    /**
     * @deprecated As of 7.0, replaced by
     *             {@link #addExpandListener(ExpandListener)}
     **/
    @Deprecated
    public void addListener(ExpandListener listener) {
        addExpandListener(listener);
    }

    /**
     * Removes the expand listener.
     * 
     * @param listener
     *            the Listener to be removed.
     */
    public void removeExpandListener(ExpandListener listener) {
        removeListener(ExpandEvent.class, listener, ExpandListener.EXPAND_METHOD);
    }

    /**
     * @deprecated As of 7.0, replaced by
     *             {@link #removeExpandListener(ExpandListener)}
     **/
    @Deprecated
    public void removeListener(ExpandListener listener) {
        removeExpandListener(listener);
    }

    /**
     * Emits the expand event.
     * 
     * @param itemId
     *            the item id.
     */
    protected void fireExpandEvent(Object itemId) {
        fireEvent(new ExpandEvent(this, itemId));
    }

    /* Collapse event */

    /**
     * Collapse event
     * 
     * @author Vaadin Ltd.
     * @since 3.0
     */
    public static class CollapseEvent extends Component.Event {

        private final Object collapsedItemId;

        /**
         * New instance of options change event.
         * 
         * @param source
         *            the Source of the event.
         * @param collapsedItemId
         */
        public CollapseEvent(Component source, Object collapsedItemId) {
            super(source);
            this.collapsedItemId = collapsedItemId;
        }

        /**
         * Gets tge Collapsed Item id.
         * 
         * @return the collapsed item id.
         */
        public Object getItemId() {
            return collapsedItemId;
        }
    }

    /**
     * Collapse event listener.
     * 
     * @author Vaadin Ltd.
     * @since 3.0
     */
    public interface CollapseListener extends Serializable {

        public static final Method COLLAPSE_METHOD = ReflectTools.findMethod(CollapseListener.class, "nodeCollapse", CollapseEvent.class);

        /**
         * A node has been collapsed.
         * 
         * @param event
         *            the Collapse event.
         */
        public void nodeCollapse(CollapseEvent event);
    }

    /**
     * Adds the collapse listener.
     * 
     * @param listener
     *            the Listener to be added.
     */
    public void addCollapseListener(CollapseListener listener) {
        addListener(CollapseEvent.class, listener, CollapseListener.COLLAPSE_METHOD);
    }

    /**
     * @deprecated As of 7.0, replaced by
     *             {@link #addCollapseListener(CollapseListener)}
     **/
    @Deprecated
    public void addListener(CollapseListener listener) {
        addCollapseListener(listener);
    }

    /**
     * Removes the collapse listener.
     * 
     * @param listener
     *            the Listener to be removed.
     */
    public void removeCollapseListener(CollapseListener listener) {
        removeListener(CollapseEvent.class, listener, CollapseListener.COLLAPSE_METHOD);
    }

    /**
     * @deprecated As of 7.0, replaced by
     *             {@link #removeCollapseListener(CollapseListener)}
     **/
    @Deprecated
    public void removeListener(CollapseListener listener) {
        removeCollapseListener(listener);
    }

    /**
     * Emits collapse event.
     * 
     * @param itemId
     *            the item id.
     */
    protected void fireCollapseEvent(Object itemId) {
        fireEvent(new CollapseEvent(this, itemId));
    }

    /**
     * Gets the visible item ids.
     * 
     * @see com.vaadin.ui.Select#getVisibleItemIds()
     */
    @Override
    public Collection<?> getVisibleItemIds() {

        final LinkedList<Object> visible = new LinkedList<Object>();

        // Iterates trough hierarchical tree using a stack of iterators
        final Stack<Iterator<?>> iteratorStack = new Stack<Iterator<?>>();
        final Collection<?> ids = rootItemIds();
        if (ids != null) {
            iteratorStack.push(ids.iterator());
        }
        while (!iteratorStack.isEmpty()) {

            // Gets the iterator for current tree level
            final Iterator<?> i = iteratorStack.peek();

            // If the level is finished, back to previous tree level
            if (!i.hasNext()) {

                // Removes used iterator from the stack
                iteratorStack.pop();
            }

            // Adds the item on current level
            else {
                final Object itemId = i.next();

                visible.add(itemId);

                // Adds children if expanded, or close the tag
                if (isExpanded(itemId) && hasChildren(itemId)) {
                    iteratorStack.push(getChildren(itemId).iterator());
                }
            }
        }

        return visible;
    }

    /**
     * Tree does not support <code>setNullSelectionItemId</code>.
     * 
     * @see com.vaadin.ui.AbstractSelect#setNullSelectionItemId(java.lang.Object)
     */
    @Override
    public void setNullSelectionItemId(Object nullSelectionItemId) throws UnsupportedOperationException {
        if (nullSelectionItemId != null) {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Adding new items is not supported.
     * 
     * @throws UnsupportedOperationException
     *             if set to true.
     * @see com.vaadin.ui.Select#setNewItemsAllowed(boolean)
     */
    @Override
    public void setNewItemsAllowed(boolean allowNewOptions) throws UnsupportedOperationException {
        if (allowNewOptions) {
            throw new UnsupportedOperationException();
        }
    }

    private ItemStyleGenerator itemStyleGenerator;

    @Override
    public void addItemClickListener(ItemClickListener listener) {
        addListener(TreeConstants.ITEM_CLICK_EVENT_ID, ItemClickEvent.class, listener, ItemClickEvent.ITEM_CLICK_METHOD);
    }

    /**
     * @deprecated As of 7.0, replaced by
     *             {@link #addItemClickListener(ItemClickListener)}
     **/
    @Override
    @Deprecated
    public void addListener(ItemClickListener listener) {
        addItemClickListener(listener);
    }

    @Override
    public void removeItemClickListener(ItemClickListener listener) {
        removeListener(TreeConstants.ITEM_CLICK_EVENT_ID, ItemClickEvent.class, listener);
    }

    /**
     * @deprecated As of 7.0, replaced by
     *             {@link #removeItemClickListener(ItemClickListener)}
     **/
    @Override
    @Deprecated
    public void removeListener(ItemClickListener listener) {
        removeItemClickListener(listener);
    }

    /**
     * Sets the {@link ItemStyleGenerator} to be used with this tree.
     * 
     * @param itemStyleGenerator
     *            item style generator or null to remove generator
     */
    public void setItemStyleGenerator(ItemStyleGenerator itemStyleGenerator) {
        if (this.itemStyleGenerator != itemStyleGenerator) {
            this.itemStyleGenerator = itemStyleGenerator;
            markAsDirty();
        }
    }

    /**
     * @return the current {@link ItemStyleGenerator} for this tree. Null if
     *         {@link ItemStyleGenerator} is not set.
     */
    public ItemStyleGenerator getItemStyleGenerator() {
        return itemStyleGenerator;
    }

    /**
     * ItemStyleGenerator can be used to add custom styles to tree items. The
     * CSS class name that will be added to the cell content is
     * <tt>v-tree-node-[style name]</tt>.
     */
    public interface ItemStyleGenerator extends Serializable {

        /**
         * Called by Tree when an item is painted.
         * 
         * @param source
         *            the source Tree
         * @param itemId
         *            The itemId of the item to be painted
         * @return The style name to add to this item. (the CSS class name will
         *         be v-tree-node-[style name]
         */
        public abstract String getStyle(Tree source, Object itemId);
    }

    // Overriden so javadoc comes from Container.Hierarchical
    @Override
    public boolean removeItem(Object itemId) throws UnsupportedOperationException {
        return super.removeItem(itemId);
    }

    /**
     * Helper API for {@link TreeDropCriterion}
     * 
     * @param itemId
     * @return
     */
    private String key(Object itemId) {
        return itemIdMapper.key(itemId);
    }

    /**
     * Sets the drag mode that controls how Tree behaves as a {@link DragSource}
     * .
     * 
     * @param dragMode
     */
    public void setDragMode(TreeDragMode dragMode) {
        this.dragMode = dragMode;
        markAsDirty();
    }

    /**
     * @return the drag mode that controls how Tree behaves as a
     *         {@link DragSource}.
     * 
     * @see TreeDragMode
     */
    public TreeDragMode getDragMode() {
        return dragMode;
    }

    /**
     * Concrete implementation of {@link DataBoundTransferable} for data
     * transferred from a tree.
     * 
     * @see {@link DataBoundTransferable}.
     * 
     * @since 6.3
     */
    protected class TreeTransferable extends DataBoundTransferable {

        public TreeTransferable(Component sourceComponent, Map<String, Object> rawVariables) {
            super(sourceComponent, rawVariables);
        }

        @Override
        public Object getItemId() {
            return getData("itemId");
        }

        @Override
        public Object getPropertyId() {
            return getItemCaptionPropertyId();
        }
    }

    /**
     * Set the item description generator which generates tooltips for the tree
     * items
     * 
     * @param generator
     *            The generator to use or null to disable
     */
    public void setItemDescriptionGenerator(ItemDescriptionGenerator generator) {
        if (generator != itemDescriptionGenerator) {
            itemDescriptionGenerator = generator;
            markAsDirty();
        }
    }

    /**
     * Get the item description generator which generates tooltips for tree
     * items
     */
    public ItemDescriptionGenerator getItemDescriptionGenerator() {
        return itemDescriptionGenerator;
    }

    private void cleanupExpandedItems() {
        Set<Object> removedItemIds = new HashSet<Object>();
        for (Object expandedItemId : expanded) {
            if (getItem(expandedItemId) == null) {
                removedItemIds.add(expandedItemId);
                if (this.expandedItemId == expandedItemId) {
                    this.expandedItemId = null;
                }
            }
        }
        expanded.removeAll(removedItemIds);
    }

    /**
     * Reads an Item from a design and inserts it into the data source.
     * Recursively handles any children of the item as well.
     * 
     * @since 7.5.0
     * @param node
     *            an element representing the item (tree node).
     * @param selected
     *            A set accumulating selected items. If the item that is read is
     *            marked as selected, its item id should be added to this set.
     * @param context
     *            the DesignContext instance used in parsing
     * @return the item id of the new item
     * 
     * @throws DesignException
     *             if the tag name of the {@code node} element is not
     *             {@code node}.
     */
    @Override
    protected String readItem(Element node, Set<String> selected, DesignContext context) {

        if (!"node".equals(node.tagName())) {
            throw new DesignException("Unrecognized child element in " + getClass().getSimpleName() + ": " + node.tagName());
        }

        String itemId = node.attr("text");
        addItem(itemId);
        if (node.hasAttr("icon")) {
            Resource icon = DesignAttributeHandler.readAttribute("icon", node.attributes(), Resource.class);
            setItemIcon(itemId, icon);
        }
        if (node.hasAttr("selected")) {
            selected.add(itemId);
        }

        for (Element child : node.children()) {
            String childItemId = readItem(child, selected, context);
            setParent(childItemId, itemId);
        }
        return itemId;
    }

    /**
     * Recursively writes the root items and their children to a design.
     * 
     * @since 7.5.0
     * @param design
     *            the element into which to insert the items
     * @param context
     *            the DesignContext instance used in writing
     */
    @Override
    protected void writeItems(Element design, DesignContext context) {
        for (Object itemId : rootItemIds()) {
            writeItem(design, itemId, context);
        }
    }

    /**
     * Recursively writes a data source Item and its children to a design.
     * 
     * @since 7.5.0
     * @param design
     *            the element into which to insert the item
     * @param itemId
     *            the id of the item to write
     * @param context
     *            the DesignContext instance used in writing
     * @return
     */
    @Override
    protected Element writeItem(Element design, Object itemId, DesignContext context) {
        Element element = design.appendElement("node");

        element.attr("text", itemId.toString());

        Resource icon = getItemIcon(itemId);
        if (icon != null) {
            DesignAttributeHandler.writeAttribute("icon", element.attributes(), icon, null, Resource.class);
        }

        if (isSelected(itemId)) {
            element.attr("selected", "");
        }

        Collection<?> children = getChildren(itemId);
        if (children != null) {
            // Yeah... see #5864
            for (Object childItemId : children) {
                writeItem(element, childItemId, context);
            }
        }

        return element;
    }
}
