# Plan: Enable Dynamic Style Removal via Registration

## Overview
Modify `Page.addStyleSheet()` methods to return `Registration` objects that allow removing the styles later.

## Implementation Plan

### 1. Server-Side Changes (flow-server)

#### Page.java
- Change return type of both `addStyleSheet` methods from `void` to `Registration`
- Generate unique ID for each added stylesheet
- Track stylesheet dependencies with their IDs
- Return Registration that triggers stylesheet removal

```java
public Registration addStyleSheet(String url) {
    return addStyleSheet(url, LoadMode.EAGER);
}

public Registration addStyleSheet(String url, LoadMode loadMode) {
    Dependency dependency = new Dependency(Type.STYLESHEET, url, loadMode);
    String dependencyId = UUID.randomUUID().toString();
    
    // Add dependency with tracking ID
    addStyleSheetDependency(dependency, dependencyId);
    
    // Return Registration for removal
    return () -> removeStyleSheet(dependencyId);
}

private void removeStyleSheet(String dependencyId) {
    // Send removal command to client
    ui.getInternals().addStyleSheetRemoval(dependencyId);
}
```

#### UIInternals.java
- Add collection to track stylesheet removals pending send to client
- Add method `addStyleSheetRemoval(String dependencyId)`
- Include removal commands in client communication

#### DependencyList.java
- Add support for tracking dependencies with IDs
- Store mapping of dependencyId -> Dependency

### 2. Client-Side Changes (flow-client)

#### Dependency Loading
- Tag created `<link>` and `<style>` elements with `data-dependency-id` attribute when adding stylesheets
- Add handler for stylesheet removal commands from server

#### FlowClient.js
```javascript
// Handle stylesheet removal command from server
function handleStylesheetRemoval(dependencyId) {
    // Remove <link> elements
    const link = document.querySelector(`link[data-dependency-id="${dependencyId}"]`);
    if (link) {
        link.remove();
    }
    
    // Remove <style> elements (for inline styles)
    const style = document.querySelector(`style[data-dependency-id="${dependencyId}"]`);
    if (style) {
        style.remove();
    }
}
```

### 3. Communication Protocol

Add new message type for stylesheet removal:
```json
{
    "type": "removeStylesheet",
    "dependencyId": "uuid-here"
}
```

## Implementation Steps

1. **Modify Page.java**
   - Change method signatures to return Registration
   - Implement dependency tracking with IDs
   - Add removeStyleSheet private method

2. **Update UIInternals**
   - Add stylesheet removal tracking
   - Modify message sending to include removals

3. **Update DependencyList**
   - Add ID tracking for dependencies
   - Support dependency lookup by ID

4. **Modify client-side dependency loader**
   - Add data-dependency-id attribute when creating DOM elements
   - Implement removal handler

5. **Update communication protocol**
   - Add removeStylesheet message handling
   - Process removal commands from server

## Testing Requirements

1. **Unit Tests**
   - Test Registration.remove() triggers removal
   - Test multiple stylesheets can be added and removed independently
   - Test removal commands are sent to client

2. **Integration Tests**
   - Verify stylesheet DOM elements are removed from browser
   - Test adding and removing same URL multiple times
   - Verify styles are actually no longer applied after removal

## Example Usage

```java
Registration styleReg = UI.getCurrent().getPage().addStyleSheet("styles/temporary.css");

// Later, when styles are no longer needed:
styleReg.remove();
```

## Notes
- No reference counting - each `addStyleSheet` call is independent
- JavaScript methods (`addJavaScript`, `addJsModule`) remain unchanged as JavaScript cannot be unloaded
- Adding return type to previously void methods is backward compatible enough