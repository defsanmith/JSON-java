# Milestone 3 &mdash; Key‑Transform XML Parser + JSON Streaming API

## 1. Overview

Milestone 3 brings two major capabilities:

| Feature                                             | What it does                                                                                                     | Where it lives                                                             |
| --------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| **On‑the‑fly key transformation while parsing XML** | Turns raw XML into a `JSONObject`, renaming every element/attribute key as it is encountered.                    | `org.json.XML` → `convertXmlToJson(Reader, KeyTransformer)`                |
| **Streaming traversal of any `JSONObject`**         | Iterate each node as a stream (`Stream<JSONNode>`), with options for _full_, _leaves‑only_, or _flat_ traversal. | `JSONNode`, `JSONNodeSpliterator`, and convenience methods in `JSONObject` |

---

## 2. Quick Start

```bash
# Clean, compile, and run the full test‑suite
mvn clean test
```

Run an individual test:

```bash
mvn test -Dtest=org.json.junit.XMLTest#testComplexXmlToJsonConversion
```

---

## 3. API Highlights

### 3.1 XML → JSON with Key Transform

```java
// Identity transform (keeps keys as‑is)
JSONObject json = XML.convertXmlToJson(
        new StringReader("<foo bar=\"1\"><baz>2</baz></foo>"),
        key -> key
);

// Example transform: upper‑case every key
JSONObject upper = XML.convertXmlToJson(reader, String::toUpperCase);
```

- `KeyTransformer` is a `@FunctionalInterface` (single method `String transform(String key)`).

### 3.2 Streaming a `JSONObject`

```java
JSONObject data = ...;

// 1) Full traversal
data.toStream().forEach(node -> System.out.println(node.getPath()));

// 2) Leaves only
data.toLeafStream().forEach(JSONNode::updateValue);

// 3) Flat (top‑level only)
data.toFlatStream()
    .map(JSONNode::getKey)
    .forEach(System.out::println);
```

`JSONNode` gives you:

| Method          | Purpose                                                                       |
| --------------- | ----------------------------------------------------------------------------- |
| `getPath()`     | Absolute JSON path (`"book[1]/title"`).                                       |
| `getKey()`      | Key or array index at this level.                                             |
| `getValue()`    | Raw value (primitive, `JSONObject`, or `JSONArray`).                          |
| Type helpers    | `isLeaf()`, `isObject()`, `isArray()`.                                        |
| Casting helpers | `getStringValue()`, `getIntValue()`, `getDoubleValue()`, `getBooleanValue()`. |
| Mutation        | `updateValue(Object newValue)` writes back into the parent object.            |

---

Each test is run automatically by `mvn test`; they validate:

- End‑to‑end XML ➜ JSON parsing with identity transform.
- Correct stream sizes and paths for simple, nested, and array structures.
- Leaf‑only and flat streaming modes.
- Aggregation examples (e.g., average age, city extraction) in `testComplexXmlToJsonConversion`.

---

## 6. Performance Notes

- **Single‑pass transform** – Keys are rewritten while parsing, eliminating a follow‑up traversal.
- **Cycle‑safe traversal** – `JSONNodeSpliterator` tracks visited objects, preventing infinite recursion in pathological (or intentionally cyclic) JSON graphs.
- **Spliterator Characteristics** – Declared `NONNULL | IMMUTABLE | SIZED` for predictable parallel semantics, though splitting is disabled to keep ordering deterministic.

---

## 7. Extending the Key Transformer

```java
KeyTransformer kebabToCamel = key -> {
    // user-name → userName
    String[] parts = key.split("-");
    for (int i = 1; i < parts.length; i++) {
        parts[i] = Character.toUpperCase(parts[i].charAt(0)) + parts[i].substring(1);
    }
    return String.join("", parts);
};

JSONObject camelKeys = XML.convertXmlToJson(reader, kebabToCamel);
```

Plug in any transform—prefixing, suffixing, locale conversions, encryption—without touching the parser core.

---
