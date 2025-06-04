package org.json.junit;

/*
Public Domain.
*/

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.json.XMLParserConfiguration;
import org.junit.Test;

/**
 * Tests for asynchronous XML to JSON conversion methods in JSON-Java XML.java
 */
public class XMLAsyncTest {

    /**
     * Test basic async conversion with callback - simple XML
     */
    @Test
    public void testBasicAsyncCallbackWithSimpleXML() throws InterruptedException {
        String xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<person>\n" +
                "   <name>John Doe</name>\n" +
                "   <age>30</age>\n" +
                "</person>";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<JSONObject> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        Consumer<JSONObject> successCallback = jsonObj -> {
            result.set(jsonObj);
            latch.countDown();
        };

        Consumer<Exception> errorCallback = ex -> {
            error.set(ex);
            latch.countDown();
        };

        XML.toJSONObjectAsync(new StringReader(xmlStr), successCallback, errorCallback);

        assertTrue("Callback should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Success callback should be called", result.get());
        assertEquals("John Doe", result.get().getJSONObject("person").getString("name"));
        assertEquals(30, result.get().getJSONObject("person").getInt("age"));
    }

    /**
     * Test async conversion with callback using String input
     */
    @Test
    public void testAsyncCallbackWithStringInput() throws InterruptedException {
        String xmlStr = "<book><title>Test Book</title><author>Test Author</author></book>";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<JSONObject> result = new AtomicReference<>();

        XML.toJSONObjectAsync(xmlStr,
                jsonObj -> {
                    result.set(jsonObj);
                    latch.countDown();
                },
                ex -> {
                    fail("Should not call error callback: " + ex.getMessage());
                    latch.countDown();
                });

        assertTrue("Callback should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Result should not be null", result.get());
        assertEquals("Test Book", result.get().getJSONObject("book").getString("title"));
        assertEquals("Test Author", result.get().getJSONObject("book").getString("author"));
    }

    /**
     * Test async conversion with callback and XMLParserConfiguration
     */
    @Test
    public void testAsyncCallbackWithConfiguration() throws InterruptedException {
        String xmlStr = "<data><num>123</num><bool>true</bool></data>";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<JSONObject> result = new AtomicReference<>();

        XMLParserConfiguration config = XMLParserConfiguration.KEEP_STRINGS;

        XML.toJSONObjectAsync(new StringReader(xmlStr), config,
                jsonObj -> {
                    result.set(jsonObj);
                    latch.countDown();
                },
                ex -> {
                    fail("Should not call error callback: " + ex.getMessage());
                    latch.countDown();
                });

        assertTrue("Callback should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Result should not be null", result.get());
        // With KEEP_STRINGS, numbers and booleans should remain as strings
        assertEquals("123", result.get().getJSONObject("data").getString("num"));
        assertEquals("true", result.get().getJSONObject("data").getString("bool"));
    }

    /**
     * Test async conversion with callback and configuration using String input
     */
    @Test
    public void testAsyncCallbackWithStringAndConfiguration() throws InterruptedException {
        String xmlStr = "<data><num>456</num><bool>false</bool></data>";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<JSONObject> result = new AtomicReference<>();

        XMLParserConfiguration config = XMLParserConfiguration.KEEP_STRINGS;

        XML.toJSONObjectAsync(xmlStr, config,
                jsonObj -> {
                    result.set(jsonObj);
                    latch.countDown();
                },
                ex -> {
                    fail("Should not call error callback: " + ex.getMessage());
                    latch.countDown();
                });

        assertTrue("Callback should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Result should not be null", result.get());
        assertEquals("456", result.get().getJSONObject("data").getString("num"));
        assertEquals("false", result.get().getJSONObject("data").getString("bool"));
    }

    /**
     * Test CompletableFuture version with Reader
     */
    @Test
    public void testCompletableFutureWithReader() throws Exception {
        String xmlStr = "<product><id>123</id><name>Widget</name><price>19.99</price></product>";

        CompletableFuture<JSONObject> future = XML.toJSONObjectAsync(new StringReader(xmlStr));

        JSONObject result = future.get(5, TimeUnit.SECONDS);
        assertNotNull("Result should not be null", result);
        assertEquals(123, result.getJSONObject("product").getInt("id"));
        assertEquals("Widget", result.getJSONObject("product").getString("name"));
        assertEquals(19.99, result.getJSONObject("product").getDouble("price"), 0.001);
    }

    /**
     * Test CompletableFuture version with Reader and configuration
     */
    @Test
    public void testCompletableFutureWithReaderAndConfig() throws Exception {
        String xmlStr = "<product><id>789</id><active>true</active></product>";

        CompletableFuture<JSONObject> future = XML.toJSONObjectAsync(
                new StringReader(xmlStr),
                XMLParserConfiguration.KEEP_STRINGS);

        JSONObject result = future.get(5, TimeUnit.SECONDS);
        assertNotNull("Result should not be null", result);
        assertEquals("789", result.getJSONObject("product").getString("id"));
        assertEquals("true", result.getJSONObject("product").getString("active"));
    }

    /**
     * Test CompletableFuture version with String
     */
    @Test
    public void testCompletableFutureWithString() throws Exception {
        String xmlStr = "<order><orderid>100</orderid><total>299.99</total></order>";

        CompletableFuture<JSONObject> future = XML.toJSONObjectAsync(xmlStr);

        JSONObject result = future.get(5, TimeUnit.SECONDS);
        assertNotNull("Result should not be null", result);
        assertEquals(100, result.getJSONObject("order").getInt("orderid"));
        assertEquals(299.99, result.getJSONObject("order").getDouble("total"), 0.001);
    }

    /**
     * Test CompletableFuture version with String and configuration
     */
    @Test
    public void testCompletableFutureWithStringAndConfig() throws Exception {
        String xmlStr = "<order><orderid>200</orderid><completed>false</completed></order>";

        CompletableFuture<JSONObject> future = XML.toJSONObjectAsync(xmlStr, XMLParserConfiguration.KEEP_STRINGS);

        JSONObject result = future.get(5, TimeUnit.SECONDS);
        assertNotNull("Result should not be null", result);
        assertEquals("200", result.getJSONObject("order").getString("orderid"));
        assertEquals("false", result.getJSONObject("order").getString("completed"));
    }

    /**
     * Test CompletableFuture error handling with invalid XML
     */
    @Test
    public void testCompletableFutureErrorHandling() {
        String invalidXml = "<invalid><tag>content</invalid>";

        CompletableFuture<JSONObject> future = XML.toJSONObjectAsync(invalidXml);

        try {
            future.get(5, TimeUnit.SECONDS);
            fail("Should throw an exception for invalid XML");
        } catch (Exception e) {
            // Expected - should contain a JSONException in the cause chain
            Throwable cause = e.getCause();
            assertTrue("Should have JSONException in cause chain",
                    cause instanceof JSONException ||
                            (cause != null && cause.getCause() instanceof JSONException));
        }
    }

    /**
     * Test async processing with complex XML structure
     */
    @Test
    public void testAsyncWithComplexXML() throws InterruptedException {
        String xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<library>\n" +
                "   <book id=\"1\">\n" +
                "       <title>Java Programming</title>\n" +
                "       <author>Jane Smith</author>\n" +
                "       <price>49.99</price>\n" +
                "   </book>\n" +
                "   <book id=\"2\">\n" +
                "       <title>Python Guide</title>\n" +
                "       <author>Bob Johnson</author>\n" +
                "       <price>39.99</price>\n" +
                "   </book>\n" +
                "</library>";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<JSONObject> result = new AtomicReference<>();

        XML.toJSONObjectAsync(xmlStr,
                jsonObj -> {
                    result.set(jsonObj);
                    latch.countDown();
                },
                ex -> {
                    fail("Should not call error callback: " + ex.getMessage());
                    latch.countDown();
                });

        assertTrue("Callback should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Result should not be null", result.get());

        JSONObject library = result.get().getJSONObject("library");
        assertNotNull("Library should not be null", library);
        assertTrue("Library should contain book data", library.length() > 0);
    }

    /**
     * Test async processing with XML containing CDATA
     */
    @Test
    public void testAsyncWithCDATA() throws Exception {
        String xmlStr = "<message><content><![CDATA[Hello <world> & everyone!]]></content></message>";

        CompletableFuture<JSONObject> future = XML.toJSONObjectAsync(xmlStr);

        JSONObject result = future.get(5, TimeUnit.SECONDS);
        assertNotNull("Result should not be null", result);
        assertEquals("Hello <world> & everyone!",
                result.getString("message"));
    }

    /**
     * Test async processing with XML containing comments
     */
    @Test
    public void testAsyncWithComments() throws Exception {
        String xmlStr = "<?xml version=\"1.0\"?>\n" +
                "<!-- This is a comment -->\n" +
                "<data>\n" +
                "   <!-- Another comment -->\n" +
                "   <value>test</value>\n" +
                "</data>";

        CompletableFuture<JSONObject> future = XML.toJSONObjectAsync(xmlStr);

        JSONObject result = future.get(5, TimeUnit.SECONDS);
        assertNotNull("Result should not be null", result);
        assertEquals("test", result.getJSONObject("data").getString("value"));
    }

    /**
     * Test async processing with empty XML elements
     */
    @Test
    public void testAsyncWithEmptyElements() throws Exception {
        String xmlStr = "<container><empty/><notempty>value</notempty></container>";

        CompletableFuture<JSONObject> future = XML.toJSONObjectAsync(xmlStr);

        JSONObject result = future.get(5, TimeUnit.SECONDS);
        assertNotNull("Result should not be null", result);

        JSONObject container = result.getJSONObject("container");
        assertEquals("", container.getString("empty"));
        assertEquals("value", container.getString("notempty"));
    }

    /**
     * Test async processing with XML namespaces
     */
    @Test
    public void testAsyncWithNamespaces() throws Exception {
        String xmlStr = "<?xml version=\"1.0\"?>\n" +
                "<root xmlns:test=\"http://example.com/test\">\n" +
                "   <test:element>value</test:element>\n" +
                "</root>";

        CompletableFuture<JSONObject> future = XML.toJSONObjectAsync(xmlStr);

        JSONObject result = future.get(5, TimeUnit.SECONDS);
        assertNotNull("Result should not be null", result);

        JSONObject root = result.getJSONObject("root");
        assertNotNull("Root should not be null", root);
        assertTrue("Should contain namespace data", root.length() > 0);
    }

    /**
     * Test that async methods complete successfully with null or empty content
     */
    @Test
    public void testAsyncWithEmptyXML() throws Exception {
        String xmlStr = "";

        CompletableFuture<JSONObject> future = XML.toJSONObjectAsync(xmlStr);

        JSONObject result = future.get(5, TimeUnit.SECONDS);
        assertNotNull("Result should not be null", result);
        assertEquals("Empty XML should result in empty JSONObject", 0, result.length());
    }

    /**
     * Test multiple concurrent async operations
     */
    @Test
    public void testConcurrentAsyncOperations() throws InterruptedException {
        int numOperations = 5;
        CountDownLatch latch = new CountDownLatch(numOperations);
        AtomicReference<Exception> error = new AtomicReference<>();

        for (int i = 0; i < numOperations; i++) {
            final int index = i;
            String xmlStr = "<item><id>" + index + "</id><name>Item " + index + "</name></item>";

            XML.toJSONObjectAsync(xmlStr,
                    jsonObj -> {
                        try {
                            assertEquals(index, jsonObj.getJSONObject("item").getInt("id"));
                            assertEquals("Item " + index, jsonObj.getJSONObject("item").getString("name"));
                        } catch (Exception e) {
                            error.set(e);
                        }
                        latch.countDown();
                    },
                    ex -> {
                        error.set(ex);
                        latch.countDown();
                    });
        }

        assertTrue("All operations should complete within 10 seconds",
                latch.await(10, TimeUnit.SECONDS));

        if (error.get() != null) {
            fail("No errors should occur: " + error.get().getMessage());
        }
    }

    /**
     * Test that CompletableFuture can be composed with other async operations
     */
    @Test
    public void testCompletableFutureComposition() throws Exception {
        String xmlStr = "<calculation><a>10</a><b>20</b></calculation>";

        CompletableFuture<Integer> sumFuture = XML.toJSONObjectAsync(xmlStr)
                .thenApply(jsonObj -> {
                    JSONObject calc = jsonObj.getJSONObject("calculation");
                    return calc.getInt("a") + calc.getInt("b");
                });

        Integer result = sumFuture.get(5, TimeUnit.SECONDS);
        assertEquals("Sum should be 30", Integer.valueOf(30), result);
    }

    /**
     * Test async processing with large XML content
     */
    @Test
    public void testAsyncWithLargeXML() throws Exception {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<data>");

        // Create a moderately large XML structure
        for (int i = 0; i < 100; i++) {
            xmlBuilder.append("<item><id>").append(i).append("</id>");
            xmlBuilder.append("<value>Value ").append(i).append("</value></item>");
        }
        xmlBuilder.append("</data>");

        CompletableFuture<JSONObject> future = XML.toJSONObjectAsync(xmlBuilder.toString());

        JSONObject result = future.get(10, TimeUnit.SECONDS);
        assertNotNull("Result should not be null", result);
        assertTrue("Should contain data", result.has("data"));
    }

    /**
     * Test that async operations are truly asynchronous (don't block the calling
     * thread)
     */
    @Test
    public void testAsyncNonBlocking() throws InterruptedException {
        String xmlStr = "<test><data>value</data></test>";
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        long startTime = System.currentTimeMillis();

        XML.toJSONObjectAsync(xmlStr,
                jsonObj -> {
                    callbackExecuted.set(true);
                    latch.countDown();
                },
                ex -> {
                    fail("Should not call error callback");
                    latch.countDown();
                });

        long endTime = System.currentTimeMillis();

        // The method call itself should return quickly (within 100ms)
        assertTrue("Async method should return quickly", (endTime - startTime) < 100);

        // But the callback should eventually be executed
        assertTrue("Callback should be executed within 5 seconds",
                latch.await(5, TimeUnit.SECONDS));
        assertTrue("Callback should have been executed", callbackExecuted.get());
    }
}
