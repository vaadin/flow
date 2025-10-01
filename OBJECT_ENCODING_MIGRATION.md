# Object-Based Type Encoding Migration (@v Format)

## Overview

This document outlines the migration from the current array-based type encoding system to a universal object-based `@v` format for Vaadin Flow's client-server JSON communication.

## Current System (Array-Based)

The current system uses JSON arrays with numeric type IDs:

- **Components**: `[0, nodeId]` (NODE_TYPE = 0)
- **Arrays**: `[1, actualArrayData]` (ARRAY_TYPE = 1) 
- **Return Channels**: `[2, nodeId, channelId]` (RETURN_CHANNEL_TYPE = 2)
- **Beans**: Complex wrapping with BEAN_TYPE = 5

### Problems with Current System

1. **Collection Issue**: Arrays need special wrapping `[1, actualData]` which causes client-side processing problems
2. **Complex Logic**: Recursive type detection and wrapping/unwrapping
3. **Performance**: Multiple parsing passes required
4. **Extensibility**: Adding new types requires core logic changes
5. **Test Failures**: `testListSerialization` fails due to array wrapping complexity

## Proposed System (@v Object-Based)

Replace array-based encoding with universal `@v` object format:

- **Components**: `{"@v": "node", "id": nodeId}`
- **Return Channels**: `{"@v": "return", "nodeId": x, "channelId": y}`
- **Arrays**: Standard JSON arrays (no special wrapping)
- **Beans**: Standard Jackson serialization with custom serializers for embedded components

### Benefits

1. **Universal Format**: Single `@v` system instead of multiple type ID constants
2. **Performance**: Uses native `JSON.parse` with reviver function for optimal client-side processing
3. **Extensibility**: Easy to add new types without changing core logic
4. **Simplicity**: No complex recursive array wrapping/unwrapping
5. **Cleaner Architecture**: Eliminates the problematic collection wrapping
6. **Fix for Collections**: Arrays/lists work as standard JSON without special handling

## Implementation Plan

### Phase 1: Server-Side Encoding (JacksonCodec)

**Goal**: Replace array-based encoding with `@v` object encoding

**Changes**:
1. Replace `wrapComplexValue()` array creation with `@v` object creation
2. Change component encoding from `[0, nodeId]` to `{"@v": "node", "id": nodeId}`
3. Change return channel encoding from `[2, nodeId, channelId]` to `{"@v": "return", "nodeId": x, "channelId": y}`
4. Remove array wrapping for collections - use standard Jackson serialization
5. Add custom Jackson serializers to handle embedded `@v` references in beans

**Files to Modify**:
- `flow-server/src/main/java/com/vaadin/flow/internal/JacksonCodec.java`

### Phase 2: Client-Side Decoding (ClientJsonCodec)

**Goal**: Replace array-based decoding with `@v` object detection using JSON.parse reviver

**Changes**:
1. Replace array-based `switch(typeId)` logic with `@v` object detection
2. Implement native `JSON.parse` with reviver function for optimal performance
3. Handle recursive `@v` resolution in nested objects
4. Remove `jsonArrayAsJsArray()` special handling
5. Simplify `decodeWithTypeInfo()` logic

**Files to Modify**:
- `flow-client/src/main/java/com/vaadin/client/flow/util/ClientJsonCodec.java`

### Phase 3: Test Updates

**Goal**: Update all tests to expect `@v` format instead of array format

**Changes**:
1. Update JacksonCodecTest to expect `@v` format
2. Update ClientJsonCodecTest to expect `@v` format  
3. Update ExecuteJavaScriptProcessorTest references
4. Update integration tests (ExecJavaScriptIT)
5. This will fix the `testListSerialization` issue

**Files to Modify**:
- `flow-server/src/test/java/com/vaadin/flow/internal/JacksonCodecTest.java`
- `flow-client/src/test/java/com/vaadin/client/flow/util/ClientJsonCodecTest.java`
- `flow-client/src/test/java/com/vaadin/client/flow/ExecuteJavaScriptProcessorTest.java`
- `flow-tests/test-root-context/src/test/java/com/vaadin/flow/uitest/ui/ExecJavaScriptIT.java`

### Phase 4: Cleanup

**Goal**: Remove legacy array-based constants and methods

**Changes**:
1. Remove `NODE_TYPE`, `ARRAY_TYPE`, `RETURN_CHANNEL_TYPE` constants
2. Remove unused `wrapComplexValue()` array methods
3. Clean up any remaining array-based logic

## Example Transformations

### Component Reference
```javascript
// Before (Array-based)
[0, 123]

// After (@v Object-based)  
{"@v": "node", "id": 123}
```

### Return Channel
```javascript
// Before (Array-based)
[2, 456, 789]

// After (@v Object-based)
{"@v": "return", "nodeId": 456, "channelId": 789}
```

### Collection with Components
```javascript
// Before (Array-based) - PROBLEMATIC
[1, [{"name": "item1"}, [0, 123], {"name": "item2"}]]

// After (@v Object-based) - CLEAN
[{"name": "item1"}, {"@v": "node", "id": 123}, {"name": "item2"}]
```

### Bean with Component
```javascript
// Before (Complex wrapping)
[5, {"title": "My Bean", "button": [0, 123]}]

// After (Direct with @v)
{"title": "My Bean", "button": {"@v": "node", "id": 123}}
```

## Expected Outcomes

1. **Fix Collection Issue**: `testListSerialization` and similar tests will pass
2. **Improved Performance**: Native JSON.parse with reviver is faster than recursive parsing
3. **Cleaner Code**: Simpler encoding/decoding logic
4. **Better Extensibility**: Easy to add new `@v` types like `{"@v": "template", ...}`
5. **Reduced Complexity**: No more array wrapping/unwrapping edge cases

## Migration Notes

- This is a breaking change for the client-server protocol
- All clients must be updated simultaneously with the server
- The change is backward incompatible but provides a cleaner foundation
- Integration tests will need to be updated to expect the new format

## Reference

Based on prototype implementation in commit `97d7ed27b32711c0802164e48408262d8c47ed92`.