package org.json.junit;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONNode;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Tests for JSONArray streaming capabilities.
 */
public class JSONArrayStreamTest {

    @Test
    public void testSimpleJsonArrayStreaming() {
        JSONArray arr = new JSONArray();
        arr.put("John");
        arr.put(30);
        arr.put(true);

        List<JSONNode> nodes = arr.toStream().collect(Collectors.toList());

        assertEquals(3, nodes.size());

        Map<String, Object> values = new HashMap<>();
        for (JSONNode node : nodes) {
            values.put(node.getKey(), node.getValue());
        }

        assertEquals("John", values.get("0"));
        assertEquals(30, values.get("1"));
        assertEquals(true, values.get("2"));

        // Check paths are correctly formatted
        List<String> paths = nodes.stream().map(JSONNode::getPath).collect(Collectors.toList());
        assertTrue(paths.contains("[0]"));
        assertTrue(paths.contains("[1]"));
        assertTrue(paths.contains("[2]"));
    }

    @Test
    public void testNestedJsonArrayStreaming() {
        JSONArray innerArray = new JSONArray();
        innerArray.put("inner1");
        innerArray.put("inner2");

        JSONObject obj = new JSONObject();
        obj.put("name", "John");
        obj.put("age", 30);

        JSONArray arr = new JSONArray();
        arr.put(innerArray);
        arr.put(obj);
        arr.put("simple");

        List<JSONNode> nodes = arr.toStream().collect(Collectors.toList());

        // Should have 8 nodes:
        // - [0] (the inner array)
        // - [0][0] (inner1)
        // - [0][1] (inner2)
        // - [1] (the object)
        // - [1]/name (John)
        // - [1]/age (30)
        // - [2] (simple string)
        assertEquals(8, nodes.size());

        // Check paths
        List<String> paths = nodes.stream().map(JSONNode::getPath).collect(Collectors.toList());
        assertTrue(paths.contains("[0]"));
        assertTrue(paths.contains("[0][0]"));
        assertTrue(paths.contains("[0][1]"));
        assertTrue(paths.contains("[1]"));
        assertTrue(paths.contains("[1]/name"));
        assertTrue(paths.contains("[1]/age"));
        assertTrue(paths.contains("[2]"));
    }

    @Test
    public void testLeafNodesStreaming() {
        JSONArray innerArray = new JSONArray();
        innerArray.put("inner1");
        innerArray.put("inner2");

        JSONObject obj = new JSONObject();
        obj.put("name", "John");
        obj.put("age", 30);

        JSONArray arr = new JSONArray();
        arr.put(innerArray);
        arr.put(obj);
        arr.put("simple");

        List<JSONNode> nodes = arr.toLeafStream().collect(Collectors.toList());

        // Should have 5 leaf nodes:
        // - [0][0] (inner1)
        // - [0][1] (inner2)
        // - [1]/name (John)
        // - [1]/age (30)
        // - [2] (simple)
        assertEquals(5, nodes.size());

        // Verify only leaf nodes are included
        for (JSONNode node : nodes) {
            assertTrue(node.isLeaf());
        }

        // Check paths
        List<String> paths = nodes.stream().map(JSONNode::getPath).collect(Collectors.toList());
        assertTrue(paths.contains("[0][0]"));
        assertTrue(paths.contains("[0][1]"));
        assertTrue(paths.contains("[1]/name"));
        assertTrue(paths.contains("[1]/age"));
        assertTrue(paths.contains("[2]"));

        // Verify non-leaf nodes are excluded
        assertFalse(paths.contains("[0]"));
        assertFalse(paths.contains("[1]"));
    }

    @Test
    public void testFlatStreamOnlyTopLevel() {
        JSONArray innerArray = new JSONArray();
        innerArray.put("inner1");
        innerArray.put("inner2");

        JSONObject obj = new JSONObject();
        obj.put("name", "John");
        obj.put("age", 30);

        JSONArray arr = new JSONArray();
        arr.put(innerArray);
        arr.put(obj);
        arr.put("simple");

        List<JSONNode> nodes = arr.toFlatStream().collect(Collectors.toList());

        // Should have 3 top-level nodes only
        assertEquals(3, nodes.size());

        // Check paths
        List<String> paths = nodes.stream().map(JSONNode::getPath).collect(Collectors.toList());
        assertTrue(paths.contains("[0]"));
        assertTrue(paths.contains("[1]"));
        assertTrue(paths.contains("[2]"));

        // Verify nested nodes are not included
        assertFalse(paths.contains("[0][0]"));
        assertFalse(paths.contains("[0][1]"));
        assertFalse(paths.contains("[1]/name"));
        assertFalse(paths.contains("[1]/age"));
    }

    @Test
    public void testComplexNestedStructure() {
        // Create a more complex structure with arrays of arrays
        JSONArray level3 = new JSONArray();
        level3.put("deep1");
        level3.put("deep2");

        JSONArray level2 = new JSONArray();
        level2.put(level3);
        level2.put(42);

        JSONArray level1 = new JSONArray();
        level1.put(level2);
        level1.put(true);

        // Check leaf nodes
        List<JSONNode> leafNodes = level1.toLeafStream().collect(Collectors.toList());
        assertEquals(4, leafNodes.size());

        List<String> leafPaths = leafNodes.stream().map(JSONNode::getPath).collect(Collectors.toList());
        assertTrue(leafPaths.contains("[0][0][0]"));
        assertTrue(leafPaths.contains("[0][0][1]"));
        assertTrue(leafPaths.contains("[0][1]"));
        assertTrue(leafPaths.contains("[1]"));

        // Check recursive stream (includes non-leaf nodes)
        List<JSONNode> allNodes = level1.toStream().collect(Collectors.toList());
        assertEquals(8, allNodes.size());

        List<String> allPaths = allNodes.stream().map(JSONNode::getPath).collect(Collectors.toList());
        assertTrue(allPaths.contains("[0]"));
        assertTrue(allPaths.contains("[0][0]"));
        assertTrue(allPaths.contains("[0][0][0]"));
        assertTrue(allPaths.contains("[0][0][1]"));
        assertTrue(allPaths.contains("[0][1]"));
        assertTrue(allPaths.contains("[1]"));

        // Check flat stream (top level only)
        List<JSONNode> flatNodes = level1.toFlatStream().collect(Collectors.toList());
        assertEquals(2, flatNodes.size());

        List<String> flatPaths = flatNodes.stream().map(JSONNode::getPath).collect(Collectors.toList());
        assertTrue(flatPaths.contains("[0]"));
        assertTrue(flatPaths.contains("[1]"));
    }

    @Test
    public void testEmptyArray() {
        JSONArray emptyArray = new JSONArray();

        List<JSONNode> nodes = emptyArray.toStream().collect(Collectors.toList());
        assertEquals(0, nodes.size());

        nodes = emptyArray.toLeafStream().collect(Collectors.toList());
        assertEquals(0, nodes.size());

        nodes = emptyArray.toFlatStream().collect(Collectors.toList());
        assertEquals(0, nodes.size());
    }
}