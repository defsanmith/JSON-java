package org.json;

import java.util.*;
import java.util.function.Consumer;

/**
 * {@code JSONNodeSpliterator} traverses a {@link JSONObject} (optionally
 * recursing into nested objects / arrays) and produces {@link JSONNode}
 * elements. It defends against circular references by keeping a
 * “visited” set.
 *
 * <p>
 * Three traversal modes are supported:
 * </p>
 * <ul>
 * <li><strong>Recursive:</strong> visit nested objects/arrays</li>
 * <li><strong>Flat:</strong> visit only immediate children</li>
 * <li><strong>Leaves‑only:</strong> emit only primitive/leaf nodes</li>
 * </ul>
 */
public class JSONNodeSpliterator implements Spliterator<JSONNode> {

    /** FIFO queue of nodes waiting to be returned by {@link #tryAdvance}. */
    private final Deque<JSONNode> nodeQueue = new ArrayDeque<>();

    /** Whether to descend into child objects/arrays. */
    private final boolean isRecursive;

    /** Whether to emit only leaf nodes. */
    private final boolean leavesOnly;

    /** Tracks objects already visited to prevent infinite loops. */
    private final Set<Object> visited = new HashSet<>();

    /**
     * @param root       root {@link JSONObject} to iterate
     * @param basePath   starting path (usually empty string)
     * @param recursive  {@code true} to recurse into children
     * @param leavesOnly {@code true} to return only primitive leaves
     */
    public JSONNodeSpliterator(JSONObject root,
            String basePath,
            boolean recursive,
            boolean leavesOnly) {
        this.isRecursive = recursive;
        this.leavesOnly = leavesOnly;

        // Pre‑collect all nodes so that structural changes
        // during traversal cannot cause ConcurrentModificationException.
        collectNodes(root, basePath, root);
    }

    /**
     * @param root       root {@link JSONArray} to iterate
     * @param basePath   starting path (usually empty string)
     * @param recursive  {@code true} to recurse into children
     * @param leavesOnly {@code true} to return only primitive leaves
     */
    public JSONNodeSpliterator(JSONArray root,
            String basePath,
            boolean recursive,
            boolean leavesOnly) {
        this.isRecursive = recursive;
        this.leavesOnly = leavesOnly;

        // Pre‑collect all array nodes
        collectArrayNodes(root, basePath, null);
    }

    /*
     * --------------------------------------------------------------------- *
     * Internal collection helpers *
     * ---------------------------------------------------------------------
     */

    /**
     * Collects nodes from a {@link JSONObject}. Recursion stops if the same
     * object instance is encountered again (cycle).
     */
    private void collectNodes(JSONObject currentObj,
            String currentPath,
            JSONObject parentObj) {
        if (visited.contains(currentObj)) {
            return; // avoid circular reference
        }
        visited.add(currentObj);

        // Snapshot of keys to avoid concurrent‑mod exceptions
        String[] keys = currentObj.keySet().toArray(new String[0]);

        for (String key : keys) {
            try {
                Object value = currentObj.get(key);
                String newPath = currentPath.isEmpty()
                        ? key
                        : currentPath + "/" + key;

                JSONNode node = new JSONNode(newPath, key, value, parentObj);

                if (!leavesOnly || node.isLeaf()) {
                    nodeQueue.addLast(node);
                }

                if (isRecursive && value != null) {
                    if (value instanceof JSONObject && !visited.contains(value)) {
                        collectNodes((JSONObject) value, newPath, (JSONObject) value);
                    } else if (value instanceof JSONArray) {
                        collectArrayNodes((JSONArray) value, newPath, parentObj);
                    }
                }

            } catch (Exception ex) {
                System.err.printf("Skipping key '%s': %s%n", key, ex.getMessage());
            }
        }
    }

    /**
     * Collects nodes inside a {@link JSONArray}, recursing into nested structures.
     */
    private void collectArrayNodes(JSONArray array,
            String currentPath,
            JSONObject parentObj) {
        if (visited.contains(array)) {
            return; // avoid circular reference
        }
        visited.add(array);

        // If not leaf-only, add the array itself as a node
        if (array != null && !currentPath.isEmpty() && !leavesOnly) {
            // Extract the parent key from the path
            String key = currentPath.contains("/")
                    ? currentPath.substring(currentPath.lastIndexOf('/') + 1)
                    : currentPath.contains("[")
                            ? currentPath.substring(currentPath.lastIndexOf('['))
                            : currentPath;

            if (key.endsWith("]")) {
                key = key.substring(1, key.length() - 1);
            }

            JSONNode arrayNode = new JSONNode(currentPath, key, array, parentObj);
            if (!leavesOnly) {
                nodeQueue.addLast(arrayNode);
            }
        }

        for (int i = 0; i < array.length(); i++) {
            try {
                Object value = array.get(i);
                String idxPath = currentPath + "[" + i + "]";

                // Add the element itself
                JSONNode elementNode = new JSONNode(idxPath, String.valueOf(i), value, parentObj);
                if (!leavesOnly || elementNode.isLeaf()) {
                    nodeQueue.addLast(elementNode);
                }

                // Recurse into nested structures if recursive mode is on
                if (isRecursive && value != null) {
                    if (value instanceof JSONObject && !visited.contains(value)) {
                        collectNodes((JSONObject) value, idxPath, (JSONObject) value);
                    } else if (value instanceof JSONArray && !visited.contains(value)) {
                        collectArrayNodes((JSONArray) value, idxPath, parentObj);
                    }
                }
            } catch (Exception ex) {
                System.err.printf("Skipping array element at [%d]: %s%n", i, ex.getMessage());
            }
        }
    }

    /*
     * --------------------------------------------------------------------- *
     * Spliterator implementation *
     * ---------------------------------------------------------------------
     */

    @Override
    public boolean tryAdvance(Consumer<? super JSONNode> action) {
        JSONNode next = nodeQueue.pollFirst();
        if (next == null) {
            return false;
        }
        action.accept(next);
        return true;
    }

    /** No support for splitting/parallel traversal. */
    @Override
    public Spliterator<JSONNode> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return nodeQueue.size();
    }

    @Override
    public int characteristics() {
        return NONNULL | IMMUTABLE | SIZED;
    }
}
