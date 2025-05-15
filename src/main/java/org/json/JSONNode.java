package org.json;

/**
 * Represents a single node in a JSON structure, including its absolute path.
 * Helpful for traversal or manipulation when streaming JSON content.
 */
public class JSONNode {

    /**
     * Absolute path from the root (e.g., "person/address/street" or
     * "books[0]/title").
     */
    private final String absolutePath;

    /** Key (or array‑index string) identifying this node inside its parent. */
    private final String nodeKey;

    /**
     * Raw value stored at this node (may be primitive, JSONObject, or JSONArray).
     */
    private final Object nodeValue;

    /**
     * Parent object that contains this node, or {@code null} if this node is the
     * root.
     */
    private final JSONObject parentObject;

    /**
     * Constructs a {@code JSONNode}.
     *
     * @param absolutePath fully qualified path from the JSON root to this node
     * @param nodeKey      key or index of this node within its parent
     * @param nodeValue    value stored at this node
     * @param parentObject parent {@link JSONObject}; {@code null} if this node is
     *                     the root
     */
    public JSONNode(String absolutePath,
            String nodeKey,
            Object nodeValue,
            JSONObject parentObject) {
        this.absolutePath = absolutePath;
        this.nodeKey = nodeKey;
        this.nodeValue = nodeValue;
        this.parentObject = parentObject;
    }

    /** @return the absolute path of this node */
    public String getPath() {
        return absolutePath;
    }

    /** @return the key or index used to access this node from its parent */
    public String getKey() {
        return nodeKey;
    }

    /** @return the raw value stored at this node */
    public Object getValue() {
        return nodeValue;
    }

    /**
     * @return parent {@link JSONObject}, or {@code null} if this node is the root
     */
    public JSONObject getParent() {
        return parentObject;
    }

    /**
     * Replaces this node’s value in its parent object.
     *
     * @param newValue the new value to set
     * @return {@code true} if updated, {@code false} if this node has no parent
     */
    public boolean updateValue(Object newValue) {
        if (parentObject != null) {
            parentObject.put(nodeKey, newValue);
            return true;
        }
        return false;
    }

    /** @return {@code true} if this node is a leaf (neither object nor array) */
    public boolean isLeaf() {
        return !(nodeValue instanceof JSONObject || nodeValue instanceof JSONArray);
    }

    /** @return {@code true} if the value is a {@link JSONObject} */
    public boolean isObject() {
        return nodeValue instanceof JSONObject;
    }

    /** @return {@code true} if the value is a {@link JSONArray} */
    public boolean isArray() {
        return nodeValue instanceof JSONArray;
    }

    /** @return string value, or {@code null} if the value is not a string */
    public String getStringValue() {
        return nodeValue instanceof String ? (String) nodeValue : null;
    }

    /**
     * @return int value
     * @throws JSONException if the value is not numeric
     */
    public int getIntValue() {
        if (nodeValue instanceof Number) {
            return ((Number) nodeValue).intValue();
        }
        throw new JSONException("Expected a number but found: " + nodeValue);
    }

    /**
     * @return double value
     * @throws JSONException if the value is not numeric
     */
    public double getDoubleValue() {
        if (nodeValue instanceof Number) {
            return ((Number) nodeValue).doubleValue();
        }
        throw new JSONException("Expected a number but found: " + nodeValue);
    }

    /**
     * @return boolean value
     * @throws JSONException if the value is not a boolean
     */
    public boolean getBooleanValue() {
        if (nodeValue instanceof Boolean) {
            return (Boolean) nodeValue;
        }
        throw new JSONException("Expected a boolean but found: " + nodeValue);
    }

    @Override
    public String toString() {
        return "JSONNode{" +
                "path='" + absolutePath + '\'' +
                ", key='" + nodeKey + '\'' +
                ", value=" + nodeValue +
                '}';
    }
}
