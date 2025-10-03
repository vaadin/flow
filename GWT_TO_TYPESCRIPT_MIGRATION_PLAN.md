# GWT to TypeScript Migration Plan for Flow Client

## Current Architecture Analysis

### Existing Structure
The flow-client module currently uses a **hybrid architecture**:

**GWT Components (~18,000 lines Java)**:
- State management (StateTree, StateNode)
- DOM binding and synchronization
- Server communication (XHR, WebSocket/Atmosphere push)
- JavaScript execution bridge
- Reactive event system
- Compiled to `client-*.cache.js` by GWT compiler

**TypeScript Components**:
- `Flow.ts`: Routing integration, initialization orchestration
- `FlowBootstrap.js`: Initial configuration setup
- Current build wraps GWT output in ES module: `export function init() { <GWT code> }`

### GWT Dependencies
- **Elemental library**: DOM abstraction (used in 57 files)
- **JSNI**: 136 blocks across 29 files for JavaScript interop
- **JavaScriptObject overlays**: 8 classes for JSON/config data
- **JsInterop**: Modern classes (collections, utilities)
- **Custom GWT linker**: ClientEngineLinker for single-script output

---

## Migration Strategy: Direct Replacement

The migration will **directly replace GWT code with TypeScript** in each phase:
- Migrate one functional area at a time
- Delete GWT code immediately after TypeScript replacement
- Run tests to verify functionality
- Commit and move to next area
- No feature flags, no parallel implementations
- Simple and straightforward approach

---

## Phase-by-Phase Migration Plan

### **Phase 0: Foundation & Tooling**

**Goal**: Set up TypeScript infrastructure and remove GWT build

**Tasks**:
1. **Create TypeScript module structure**
   ```
   flow-client/src/main/frontend/
   ├── collections/    # Collections and utilities
   ├── dom/            # DOM abstraction
   ├── state/          # State management
   ├── binding/        # Data binding
   ├── communication/  # Server communication
   └── core/           # Application core
   ```

2. **Update build system**:
   - Remove gwt-maven-plugin from pom.xml
   - Remove GWT dependencies (gwt-user, gwt-dev, gwt-elemental)
   - Keep only TypeScript compilation
   - Update scripts/client.js to directly export TypeScript output

3. **Set up bundling**:
   - Configure esbuild or rollup to create single bundle
   - Output to same location as GWT for compatibility
   - Preserve existing public API surface

**Deliverable**: TypeScript-only build system ready for migration

---

### **Phase 1: Collections & Utilities**

**Goal**: Replace GWT collection wrappers with native TypeScript

**Why first**: Leaf dependencies, no complex logic, well-defined APIs

**Migration steps**:
1. Read GWT collection implementations (JsArray, JsMap, JsSet, JsWeakMap)
2. Create TypeScript equivalents in `collections/`
3. Read utility classes (Console, BrowserInfo, WidgetUtil, URIResolver, ClientJsonCodec)
4. Implement TypeScript versions in `collections/` or `core/`
5. Delete all GWT collection/utility classes
6. Run tests: `mvn test` and `npm test`
7. Fix any test failures
8. Run integration tests: `mvn verify -pl flow-tests/test-root-context`
9. Commit changes

**Classes to migrate**:
- `JsArray`, `JsMap`, `JsSet`, `JsWeakMap` (~800 lines)
- `Console`, `BrowserInfo`, `WidgetUtil` utilities (~1,000 lines)
- `URIResolver`, `ClientJsonCodec` (~300 lines)

**Success criteria**:
- All existing tests pass
- No GWT collection/utility code remains

---

### **Phase 2: DOM Abstraction Layer**

**Goal**: Replace Elemental DOM with native TypeScript DOM types

**Why second**: Well-isolated layer, clear boundary, Elemental is deprecated

**Migration steps**:
1. Read GWT DomApi and DomApiImpl classes
2. Create TypeScript implementations in `dom/`
3. Use native browser DOM APIs (no Elemental needed)
4. Read ElementUtil and ExecuteJavaScriptElementUtils
5. Implement TypeScript versions
6. Handle Polymer compatibility (PolymerDomApiImpl) - keep if still needed
7. Delete GWT DOM abstraction classes
8. Run tests: `mvn test` and `npm test`
9. Fix any test failures
10. Commit changes

**Classes to migrate**:
- `DomApi`, `DomApiImpl`, `PolymerDomApiImpl` (~200 lines)
- `ElementUtil`, `ExecuteJavaScriptElementUtils` (~400 lines)

**Success criteria**:
- All existing tests pass
- No Elemental dependencies remain (except Polymer support if needed)

---

### **Phase 3: State Management Core**

**Goal**: Rewrite state tree and reactive system in TypeScript

**Why third**: Core dependency for everything else, complex but well-tested

**This is the critical phase** - state management is the heart of Flow client

**Migration steps**:
1. Read GWT reactive system classes (Reactive, ReactiveValue, Computation, ReactiveEventRouter)
2. Implement TypeScript reactive primitives in `state/reactive/`
3. Read GWT NodeFeature classes (NodeFeature, NodeMap, NodeList, MapProperty)
4. Implement TypeScript versions in `state/nodefeature/`
5. Read StateNode and StateTree classes
6. Implement TypeScript versions in `state/`
7. Read ConstantPool and TreeChangeProcessor
8. Implement TypeScript versions in `state/`
9. Delete all GWT state management classes
10. Run tests: `mvn test` and `npm test`
11. Fix any test failures (expect many - this is complex)
12. Run integration tests: `mvn verify -pl flow-tests/test-root-context`
13. Debug and fix integration test failures
14. Verify performance is acceptable
15. Commit changes

**Classes to migrate**:
- Reactive system: `Reactive`, `ReactiveValue`, `Computation`, `ReactiveEventRouter` (~500 lines)
- NodeFeature classes: `NodeFeature`, `NodeMap`, `NodeList`, `MapProperty` (~600 lines)
- State tree: `StateNode`, `StateTree` (~1,000 lines)
- Processing: `ConstantPool`, `TreeChangeProcessor` (~900 lines)

**Success criteria**:
- All existing tests pass
- Integration tests pass
- Performance within acceptable range
- No GWT state management code remains

---

### **Phase 4: Data Binding**

**Goal**: Replace DOM binding logic with TypeScript

**Depends on**: Phase 2 (DOM) and Phase 3 (State)

**Migration steps**:
1. Read GWT Binder class and BindingStrategy interface
2. Read binding strategy implementations (SimpleElementBindingStrategy, TextBindingStrategy, etc.)
3. Implement TypeScript binding system in `binding/`
4. Read ServerEventHandlerBinder and Debouncer
5. Implement TypeScript versions
6. Read ServerEventObject
7. Implement TypeScript version
8. Delete all GWT binding classes
9. Run tests: `mvn test` and `npm test`
10. Fix any test failures
11. Run integration tests: `mvn verify -pl flow-tests/test-root-context`
12. Debug and fix integration test failures
13. Commit changes

**Classes to migrate**:
- Binding core: `Binder`, `BindingStrategy` (~300 lines)
- Strategies: `SimpleElementBindingStrategy`, `TextBindingStrategy` (~500 lines)
- Event handling: `ServerEventHandlerBinder`, `ServerEventObject`, `Debouncer` (~400 lines)

**Success criteria**:
- All existing tests pass
- Integration tests pass
- No GWT binding code remains

---

### **Phase 5: Server Communication**

**Goal**: Rewrite XHR and push communication in TypeScript

**Depends on**: Phase 3 (State) and Phase 4 (Binding)

**Migration steps**:
1. Read GWT MessageHandler and MessageSender classes
2. Understand UIDL message protocol from GWT code
3. Create TypeScript message protocol types in `communication/protocol/`
4. Implement MessageHandler in TypeScript
5. Implement MessageSender in TypeScript
6. Read XhrConnection and ServerConnector
7. Implement TypeScript versions (use fetch API)
8. Read AtmospherePushConnection
9. Create TypeScript wrapper for Atmosphere (keep library for now)
10. Read RequestResponseTracker, Heartbeat, Poller
11. Implement TypeScript versions
12. Read ServerRpcQueue and related classes
13. Implement TypeScript version
14. Read PollConfigurator, LoadingIndicatorConfigurator
15. Implement TypeScript versions
16. Delete all GWT communication classes
17. Run tests: `mvn test` and `npm test`
18. Fix any test failures
19. Run integration tests: `mvn verify -pl flow-tests/test-root-context`
20. Debug and fix integration test failures
21. Test push communication specifically
22. Commit changes

**Classes to migrate**:
- Message handling: `MessageHandler`, `MessageSender` (~1,500 lines)
- Connection: `XhrConnection`, `ServerConnector` (~500 lines)
- Push: `AtmospherePushConnection`, `PushConnection` (~400 lines)
- Tracking: `RequestResponseTracker`, `Heartbeat`, `Poller` (~600 lines)
- RPC: `ServerRpcQueue` (~300 lines)
- Configuration: `PollConfigurator`, `LoadingIndicatorConfigurator` (~200 lines)

**Success criteria**:
- All existing tests pass
- Integration tests pass
- Push communication works correctly
- No GWT communication code remains

---

### **Phase 6: JavaScript Execution**

**Goal**: Migrate `executeJs()` functionality to TypeScript

**Migration steps**:
1. Read GWT ExecuteJavaScriptProcessor class
2. Understand parameter conversion (StateNode → DOM element)
3. Understand return value handling (DOM element → StateNode)
4. Implement TypeScript version in `core/`
5. Delete GWT ExecuteJavaScriptProcessor
6. Run tests: `mvn test` and `npm test`
7. Fix any test failures
8. Run integration tests: `mvn verify -pl flow-tests/test-root-context`
9. Specifically test executeJs functionality
10. Commit changes

**Classes to migrate**:
- `ExecuteJavaScriptProcessor` (~400 lines)

**Success criteria**:
- All existing tests pass
- Integration tests pass
- executeJs() works correctly from server
- No GWT JavaScript execution code remains

---

### **Phase 7: Application Core & Bootstrap**

**Goal**: Migrate application initialization and lifecycle

**Migration steps**:
1. Read GWT Registry and DefaultRegistry classes
2. Implement TypeScript DI container in `core/`
3. Read ApplicationConnection class
4. Understand initialization flow and public API
5. Implement TypeScript ApplicationConnection in `core/`
6. Read ApplicationConfiguration class
7. Implement TypeScript version
8. Read UILifecycle class
9. Implement TypeScript version
10. Read Bootstrapper entry point
11. Merge bootstrapping logic into existing `Flow.ts` or `FlowBootstrap.js`
12. Update FlowClient.d.ts with correct type definitions
13. Delete all GWT core classes (Bootstrapper, Registry, ApplicationConnection, etc.)
14. Delete GWT entry point files (.gwt.xml)
15. Run tests: `mvn test` and `npm test`
16. Fix any test failures
17. Run integration tests: `mvn verify -pl flow-tests/test-root-context`
18. Debug and fix integration test failures
19. Test full application initialization flow
20. Commit changes

**Classes to migrate**:
- DI: `Registry`, `DefaultRegistry` (~200 lines)
- Core: `ApplicationConnection` (~350 lines)
- Config: `ApplicationConfiguration` (~300 lines)
- Lifecycle: `UILifecycle` (~200 lines)
- Bootstrap: `Bootstrapper` → merge into `Flow.ts`/`FlowBootstrap.js` (~200 lines)

**Success criteria**:
- All existing tests pass
- Integration tests pass
- Full application starts and runs correctly
- No GWT core code remains
- No .gwt.xml files remain

---

### **Phase 8: Final Cleanup**

**Goal**: Complete removal of GWT toolchain and infrastructure

**Migration steps**:
1. Verify all GWT Java source files are deleted
2. Verify all .gwt.xml files are deleted
3. Remove gwt-maven-plugin from pom.xml
4. Remove GWT dependencies from pom.xml:
   - gwt-user
   - gwt-dev
   - gwt-elemental
   - asm (GWT dependency)
5. Remove custom ClientEngineLinker Java class
6. Remove build-helper-maven-plugin configuration for GWT tests
7. Remove GWT test source directory (src/test-gwt)
8. Update scripts/client.js to work with TypeScript output only
9. Or remove scripts/client.js and update build to directly use bundled TS
10. Configure esbuild/rollup for production bundling
11. Update output path to match expected location
12. Remove GWT-specific test infrastructure
13. Convert any remaining GWT tests to TypeScript tests
14. Update package.json scripts if needed
15. Update tsconfig.json for optimal build
16. Run full build: `mvn clean install`
17. Run all tests: `mvn test` and `npm test`
18. Run integration tests: `mvn verify`
19. Test in real browser environment
20. Update documentation
21. Commit final changes

**Success criteria**:
- Zero GWT dependencies in pom.xml
- No GWT Java source files
- No GWT test infrastructure
- Full build completes successfully
- All tests pass
- Integration tests pass
- Application works in browser

---

## Migration Metrics

### Code Estimate
| Phase | GWT Lines | TS Lines (est.) | Net Change |
|-------|-----------|-----------------|------------|
| Phase 1: Collections | 2,100 | 1,500 | -600 |
| Phase 2: DOM | 600 | 400 | -200 |
| Phase 3: State | 3,000 | 2,500 | -500 |
| Phase 4: Binding | 1,200 | 1,000 | -200 |
| Phase 5: Communication | 3,500 | 3,000 | -500 |
| Phase 6: JS Execution | 400 | 300 | -100 |
| Phase 7: Core | 1,250 | 1,000 | -250 |
| Phase 8: Cleanup | 6,000 | 0 | -6,000 |
| **Total** | **~18,000** | **~9,700** | **~-8,300** |

### Success Criteria (Every Phase)
- ✅ All unit tests pass (`mvn test`, `npm test`)
- ✅ All integration tests pass (`mvn verify`)
- ✅ No GWT code remains in migrated area
- ✅ Application functions correctly in browser

---

## Key Principles

### Direct Replacement
- Migrate one complete functional area at a time
- Delete GWT code immediately after TypeScript replacement works
- No parallel implementations or feature flags
- Commit working code and move to next area

### Testing First
- Run tests after every phase
- Fix all test failures before committing
- Integration tests must pass
- Manual browser testing before commit

### No Scope Creep
- Each phase migrates code only, no refactoring
- Keep same architecture and patterns
- Improvements come later, after migration is complete
- Focus on getting tests to pass

### Documentation
- Document complex behaviors while reading GWT code
- Add inline comments explaining non-obvious logic
- Update type definitions as you go
- Keep notes on decisions made

---

## Getting Started

Start with Phase 0 to set up the infrastructure, then proceed through phases 1-8 in order. Each phase is a complete, tested, committed piece of work before moving to the next.
