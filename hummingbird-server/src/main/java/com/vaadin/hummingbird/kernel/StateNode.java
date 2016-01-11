package com.vaadin.hummingbird.kernel;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.hummingbird.kernel.ValueType.ObjectType;
import com.vaadin.hummingbird.kernel.change.NodeChange;

public interface StateNode extends Serializable {

    /**
     * Returns the id of this node
     *
     * @return a positive integer if attached, a negative integer if detached or
     *         0 if the node does not yet have an id
     */
    public int getId();

    /**
     * Attaches the given child object to this node
     *
     * @param object
     *            the child object to attach
     */
    public void attachChild(Object object);

    /**
     * Detaches the given child object from this node
     *
     * @param object
     *            the child object to detach
     */
    public void detachChild(Object object);

    /**
     * Checks if the node is attached to a {@link RootNode}
     *
     * @return true if the node is attached to a root node, false otherwise
     */
    public boolean isAttached();

    /**
     * Gets the root node of the node tree
     *
     * @return the root node or null if the node is not attached to a root
     */
    public RootNode getRoot();

    /**
     * Sets the root node of the node tree
     *
     * @deprecated Should probably not be public API
     * @param root
     *            the root to set
     */
    @Deprecated
    public void setRoot(RootNode root);

    /**
     * Gets the parent of this node.
     *
     * @return the parent of the node or null if the node does not have a parent
     */
    public StateNode getParent();

    /**
     * Checks if the given node is an ancestor of this node
     *
     * @param node
     *            the ancestor node to check
     * @return true if the given node is an ancestor of this node, false
     *         otherwise
     */
    default public boolean hasAncestor(StateNode node) {
        StateNode n = this;
        while (n != null) {
            if (n == node) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }

    /**
     * Checks if this node should be kept only on server and not be synchronized
     * to the client
     *
     * @return true if the node should only exist on the server, false otherwise
     */
    public boolean isServerOnly();

    /**
     * Checks if the key should be kept only on server and not be synchronized
     * to the client
     *
     * @param key
     *            the key of the content
     * @return true if the data mapped to the key should be kept on the server,
     *         false otherwise
     */
    public boolean isServerOnlyKey(Object key);

    /**
     * Retrieves the content mapped to the given key
     *
     * @param key
     *            the key of the content
     * @return the object mapped to the given key or null if no content has been
     *         mapped
     */
    public Object get(Object key);

    /**
     * Retrieves the content mapped to the given key and ensure it is of the
     * given type
     *
     * @param key
     *            the key of the content
     * @param type
     *            the type of the value
     * @return the object mapped to the given key, cast to the correct type, or
     *         null if no content has been mapped
     */
    @SuppressWarnings("unchecked")
    default public <T> T get(Object key, Class<T> type) {
        Object value = get(key);
        assert value == null || type.isInstance(value);

        return (T) value;
    }

    /**
     * Retrieves the content mapped to the given key or the default value if no
     * content has been mapped
     *
     * @param key
     *            the key of the content
     * @param defaultValue
     *            the default value to use if no content has been mapped
     * @return the object mapped to the given key or {@code defaultValue}
     */
    default public boolean get(String key, boolean defaultValue) {
        if (containsKey(key)) {
            return (boolean) get(key);
        } else {
            return defaultValue;
        }
    }

    /**
     * Retrieves the content mapped to the given key or the default value if no
     * content has been mapped
     *
     * @param key
     *            the key of the content
     * @param defaultValue
     *            the default value to use if no content has been mapped
     * @return the object mapped to the given key or {@code defaultValue}
     */
    default public double get(String key, double defaultValue) {
        if (containsKey(key)) {
            return (double) get(key);
        } else {
            return defaultValue;
        }
    }

    /**
     * Retrieves the content mapped to the given key or the default value if no
     * content has been mapped
     *
     * @param key
     *            the key of the content
     * @param defaultValue
     *            the default value to use if no content has been mapped
     * @return the object mapped to the given key or {@code defaultValue}
     */
    default public int get(String key, int defaultValue) {
        if (containsKey(key)) {
            return (int) get(key);
        } else {
            return defaultValue;
        }
    }

    /**
     * Retrieves the content mapped to the given key or the default value if no
     * content has been mapped
     *
     * @param key
     *            the key of the content
     * @param defaultValue
     *            the default value to use if no content has been mapped
     * @return the object mapped to the given key or {@code defaultValue}
     */
    default public String get(String key, String defaultValue) {
        if (containsKey(key)) {
            return (String) get(key);
        } else {
            return defaultValue;
        }
    }

    /**
     * Maps the given content to the given key
     *
     * @param key
     *            the key of the content
     * @param value
     *            the object to be mapped to the given key, cannot be null
     * @return the object prevoiusly mapped to the given key, or null if no
     *         object was previously mapped to the key
     */
    public Object put(Object key, Object value);

    /**
     * Removes the content mapped using the given key
     *
     * @param key
     *            the key of the content
     * @return the object mapped to the given key, or null if no object was
     *         previously mapped to the key
     */
    public Object remove(Object key);

    /**
     * Checks if the given key has been mapped to some content
     *
     * @param key
     *            the key of the content
     * @return true if the key is mapped to some content, false otherwise
     */
    public boolean containsKey(Object key);

    /**
     * Returns a collection of all mapped keys
     *
     * @return a collection of all mapped keys
     */
    public Set<Object> getKeys();

    /**
     * Returns a collection of all mapped keys which have a String type
     *
     * @return a collection of keys of type String
     */
    public Set<String> getStringKeys();

    /**
     * @deprecated Should not be public API
     *
     * @param listRemoveChange
     */
    @Deprecated
    public void logChange(NodeChange change);

    /**
     * Rolls back the given change
     *
     * @deprecated Should not be public API
     * @param change
     */
    @Deprecated
    public void rollback(NodeChange change);

    /**
     * @deprecated Should not be public API
     * @param object
     */
    @Deprecated
    public void setParent(StateNode parent);

    /**
     * Called when the node is attached to a root. Should be attach() ?
     *
     * @deprecated Should not be public API
     *
     */
    @Deprecated
    public void register();

    /**
     * Called when the node is detached from a root. Should be detach() ?
     *
     * @deprecated Should not be public API
     *
     */
    @Deprecated
    public void unregister();

    /**
     * Creates a new StateNode
     *
     * @return a new StateNode
     */
    public static StateNode create() {
        return create(ValueType.EMPTY_OBJECT);
    }

    public static StateNode create(ObjectType type) {
        return new MapStateNode(type);
    }

    /**
     * Adds the given javascript to the queue of Javascript commands which will
     * be executed when this node has been sent to the client
     *
     * @param javascript
     *            The script snippet to execute. Can contain argument
     *            placeholder $1,$2,...,$N which are replaced by the values in
     *            the {@code arguments} array
     * @param arguments
     *            Optional arguments for the javascript, must be of types
     *            supported by {@link JsonConverter}
     */
    public void enqueueRpc(String javascript, Object... arguments);

    /**
     * Schedules the given runnable for execution when this node has been
     * attached. If the node is already attached, the runnable will be executed
     * immediately
     *
     * @param runnable
     *            the runnable to execute
     */
    public void runAttached(Runnable runnable);

    /**
     * Schedules the given runnable for execution before node updates for this
     * node are sent to the client.
     *
     * @param runnable
     *            the runnable to execute
     */
    public void runBeforeNextClientResponse(Runnable runnable);

    /**
     * Gets (and possibly creates) a List mapped to the given key
     *
     * @deprecated Replace with explicit put(key, List.create())
     * @param key
     *            the key of the content
     * @return the list mapped to the given key, never null
     */
    @Deprecated
    public List<Object> getMultiValued(Object key);

    /**
     * Adds the given node change listener.
     * <p>
     * A node change listener is called during {@link RootNode#commit()} for all
     * changes related to this node
     *
     * @param nodeChangeListener
     *            the listener to add
     */
    public void addChangeListener(NodeChangeListener nodeChangeListener);

    /**
     * Removes the given node change listener.
     * <p>
     * A node change listener is called during {@link RootNode#commit()} for all
     * changes related to this node
     *
     * @param nodeChangeListener
     *            the listener to remove
     */
    public void removeChangeListener(NodeChangeListener nodeChangeListener);

    /**
     * Gets the currently defined computed properties.
     * 
     * @return a map of computed properties
     */
    public Map<String, ComputedProperty> getComputedProperties();

    public ObjectType getType();

}
