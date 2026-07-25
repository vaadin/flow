# flow-tests migration list (current IT -> target)

Generated mapping of every current `*IT.java` to its target module and feature
package, per the decision procedure in [README.md](README.md) and the permutation
policy in [MIGRATION.md](MIGRATION.md).

- **type**: `move` = source home moves here; `reuse` = runs here as a permutation
  by reusing another module's test-jar (no source copy); `keep` = stays in its own
  special-infra module.
- Feature packages for the flat `test-root-context` `ui` package are **suggested**
  (keyword-derived) and confirmed per migration PR; sub-packaged and per-module
  mappings are firm.

## Summary (ITs per target)

| Target | ITs |
|---|---|
| test-default | 349 |
| test-embedding | 24 |
| test-vaadin-router | 22 |
| test-spring-security | 20 |
| test-themes | 14 |
| test-contextpath | 13 |
| test-production | 12 |
| test-livereload | 9 |
| test-plain-servlet | 9 |
| test-pwa | 8 |
| test-dev-bundle | 7 |
| test-redeployment | 5 |
| test-custom-frontend-directory | 4 |
| (benchmark, keep) | 3 |
| test-bun | 3 |
| test-eager-bootstrap | 3 |
| test-pnpm | 3 |
| test-commercial-banner(keep) | 1 |
| test-fault-tolerance(keep) | 1 |
| test-multi-war(keep) | 1 |
| test-no-theme | 1 |
| test-performance-regression(keep) | 1 |
| test-plain-spring | 1 |
| test-tailwind | 1 |
| **TOTAL** | **515** |

## Mapping (grouped by target module, then feature)

### → (benchmark, keep)


**`benchmark`**

| IT | from | type |
|---|---|---|
| SpringDevToolsReloadViewIT | vaadin-spring-tests/test-plain-spring-boot-reload-time | keep |
| SpringDevToolsReloadViewIT | vaadin-spring-tests/test-spring-boot-multimodule-reload-time | keep |
| SpringDevToolsReloadViewIT | vaadin-spring-tests/test-spring-boot-reload-time | keep |

### → test-bun


**`frontend`**

| IT | from | type |
|---|---|---|
| BunUsedIT | test-frontend/test-bun | move |
| IdTestIT | test-frontend/test-bun | move |
| WaitForDevServerIT | test-frontend/test-bun | move |

### → test-commercial-banner(keep)


**`infra`**

| IT | from | type |
|---|---|---|
| CommercialBannerIT | test-commercial-banner/integration-test | keep |

### → test-contextpath


**`routing`**

| IT | from | type |
|---|---|---|
| ContextPathIT | test-frontend/vite-context-path | move |
| AbstractContextIT | test-router-custom-context | move |
| EncodedParameterIT | test-router-custom-context | move |
| OfflineWithCustomContextIT | test-router-custom-context | move |
| PlusInParameterIT | test-router-custom-context | move |
| PushIT | test-router-custom-context | move |
| RootContextIT | test-router-custom-context | move |
| RoutedContextIT | test-router-custom-context | move |
| RoutedSubContextIT | test-router-custom-context | move |
| RoutedSubContextSubPathIT | test-router-custom-context | move |
| SubContextIT | test-router-custom-context | move |
| SubContextNoEndingSlashIT | test-router-custom-context | move |
| SubContextSubPathIT | test-router-custom-context | move |

### → test-custom-frontend-directory


**`frontend`**

| IT | from | type |
|---|---|---|
| CssLoadingIT | test-custom-frontend-directory/test-themes-custom-frontend-directory | move |
| ThemeIT | test-custom-frontend-directory/test-themes-custom-frontend-directory | move |
| UtilityClassNameIT | test-legacy-frontend | move |
| CustomFrontendMainIT | test-npm-only-features/test-npm-custom-frontend-directory | move |

### → test-default


**`(already migrated)`**

| IT | from | type |
|---|---|---|
| AbstractDefaultIT | test-default | done |
| RemoveAddVisibilityIT | test-default | done |

**`bootstrap`**

| IT | from | type |
|---|---|---|
| AbstractUpdateDivIT | test-root-context | move |
| BrowserWindowResizeIT | test-root-context | move |
| ClientResourceIT | test-root-context | move |
| CountUIsIT | test-root-context | move |
| DevModeConfigIT | test-root-context | move |
| ExtendedClientDetailsIT | test-root-context | move |
| FaultyLocationIT | test-root-context | move |
| InitialExtendedClientDetailsIT | test-root-context | move |
| InvalidLocationIT | test-root-context | move |
| LogoutIT | test-root-context | move |
| PostponeProceedIT | test-root-context | move |
| PostponeUpdateIT | test-root-context | move |
| RefreshCloseConnectionIT | test-root-context | move |
| ReturnChannelIT | test-root-context | move |
| ScreenOrientationIT | test-root-context | move |
| SessionCloseLogoutIT | test-root-context | move |
| StaticHtmlIT | test-root-context | move |
| TimingInfoReportedIT | test-root-context | move |
| TrackMessageSizeIT | test-root-context | move |

**`components`**

| IT | from | type |
|---|---|---|
| AbstractBasicElementComponentIT | test-root-context | move |
| BasicComponentIT | test-root-context | move |
| ComplexDialogShortcutIT | test-root-context | move |
| ComponentEventDataIT | test-root-context | move |
| CompositeIT | test-root-context | move |
| CompositeNestedIT | test-root-context | move |
| DialogShortcutIT | test-root-context | move |
| DisabledImageDownloadHandlerIT | test-root-context | move |
| DnDAbsolutePositioningIT | test-root-context | move |
| DnDIT | test-root-context | move |
| DownloadHandlerIT | test-root-context | move |
| EnabledIT | test-root-context | move |
| FindComponentIT | test-root-context | move |
| IFrameIT | test-root-context | move |
| ImageClickIT | test-root-context | move |
| InMemoryChildrenIT | test-root-context | move |
| InertComponentIT | test-root-context | move |
| LoadingIndicatorIT | test-root-context | move |
| ModalDialogIT | test-root-context | move |
| ShortcutsIT | test-root-context | move |
| UploadIT | test-root-context | move |

**`dependencies`**

| IT | from | type |
|---|---|---|
| AbstractContextInlineIT | test-root-context | move |
| AbstractStreamResourceIT | test-root-context | move |
| AnnotatedContextInlineIT | test-root-context | move |
| CallFunctionBeforeRemoveIT | test-root-context | move |
| ContextInlineApiIT | test-root-context | move |
| DependenciesLoadingAnnotationsIT | test-root-context | move |
| DependenciesLoadingPageApiIT | test-root-context | move |
| DirectoryImportIT | test-root-context | move |
| DynamicDependencyIT | test-root-context | move |
| ExecJavaScriptIT | test-root-context | move |
| ExternalJavaScriptIT | test-root-context | move |
| HistoryIT | test-root-context | move |
| JavaScriptReturnValueIT | test-root-context | move |
| JsApiGetByIdIT | test-root-context | move |
| JsFunctionIT | test-root-context | move |
| JsInitializerIT | test-root-context | move |
| PlainScriptViaJavaScriptIT | test-root-context | move |
| ScriptInjectIT | test-root-context | move |
| StreamResourceIT | test-root-context | move |
| WebStorageIT | test-root-context | move |

**`devmode`**

| IT | from | type |
|---|---|---|
| AttachExistingElementByIdIT | test-dev-mode | move |
| BrowserLoggingIT | test-dev-mode | move |
| DependencyFilterIT | test-dev-mode | move |
| DependencyIT | test-dev-mode | move |
| ExceptionStacktraceIT | test-dev-mode | move |
| ExportedJSFunctionIT | test-dev-mode | move |
| InfoIT | test-dev-mode | move |
| OrderedDependencyIT | test-dev-mode | move |
| StreamResourceIT | test-dev-mode | move |
| UrlValidationIT | test-dev-mode | move |
| ViteCommunicationIT | test-dev-mode | move |
| ViteLogoutRedirectIT | test-dev-mode | move |

**`di`**

| IT | from | type |
|---|---|---|
| PartialMatchRefreshIT | vaadin-spring-tests/test-spring-boot | move |
| BeansWithNoOwnerIT | vaadin-spring-tests/test-spring-common | move |
| CoExistingSpringEndpointsIT | vaadin-spring-tests/test-spring-common | move |
| ComponentAddedViaInitListenerIT | vaadin-spring-tests/test-spring-common | move |
| ComponentTestIT | vaadin-spring-tests/test-spring-common | move |
| CustomWebSocketIT | vaadin-spring-tests/test-spring-common | move |
| DoubleNpmAnnotationIT | vaadin-spring-tests/test-spring-common | move |
| EncodedParameterIT | vaadin-spring-tests/test-spring-common | move |
| ErrorParameterIT | vaadin-spring-tests/test-spring-common | move |
| LayoutIT | vaadin-spring-tests/test-spring-common | move |
| NPEHandlerIT | vaadin-spring-tests/test-spring-common | move |
| ParentChildNoOwnerIT | vaadin-spring-tests/test-spring-common | move |
| ParentTemplateIT | vaadin-spring-tests/test-spring-common | move |
| PreserveOnRefreshDestroyBeanIT | vaadin-spring-tests/test-spring-common | move |
| PreserveOnRefreshIT | vaadin-spring-tests/test-spring-common | move |
| ProfiledRouteIT | vaadin-spring-tests/test-spring-common | move |
| PushIT | vaadin-spring-tests/test-spring-common | move |
| ResourcesIT | vaadin-spring-tests/test-spring-common | move |
| RouteBasicIT | vaadin-spring-tests/test-spring-common | move |
| ScopesIT | vaadin-spring-tests/test-spring-common | move |
| SmokeTestIT | vaadin-spring-tests/test-spring-common | move |
| StreamResourceIT | vaadin-spring-tests/test-spring-common | move |
| SwaggerIT | vaadin-spring-tests/test-spring-common | move |
| TemplatePushIT | vaadin-spring-tests/test-spring-common | move |
| TranslationIT | vaadin-spring-tests/test-spring-common | move |
| UploadIT | vaadin-spring-tests/test-spring-common | move |
| VaadinAutowiredDependenciesIT | vaadin-spring-tests/test-spring-common | move |
| ClassScannerIT | vaadin-spring-tests/test-spring-filter-packages | move |
| ClassScannerIT | vaadin-spring-tests/test-spring-filter-packages | move |
| SimpleIT | vaadin-spring-tests/test-spring-white-list | move |

**`dom`**

| IT | from | type |
|---|---|---|
| AbstractEventDataIT | test-root-context | move |
| AllowInertDomEventIT | test-root-context | move |
| BasicElementIT | test-root-context | move |
| ClassListBindIT | test-root-context | move |
| CustomBrowserTooOldPageIT | test-root-context | move |
| CustomCustomElementIT | test-root-context | move |
| DomEventFilterIT | test-root-context | move |
| ElementInitOrderIT | test-root-context | move |
| ElementInnerHtmlIT | test-root-context | move |
| ElementRemoveItselfIT | test-root-context | move |
| EventListenersIT | test-root-context | move |
| EventTargetIT | test-root-context | move |
| FocusBlurIT | test-root-context | move |
| InvisibleSlotAttributeIT | test-root-context | move |
| KeyboardEventIT | test-root-context | move |
| NamespacedElementsIT | test-root-context | move |
| PageIT | test-root-context | move |
| PageVisibilityIT | test-root-context | move |
| ServiceInitListenersIT | test-root-context | move |
| ShadowRootIT | test-root-context | move |
| TransferProgressListenerIT | test-root-context | move |
| UIElementIT | test-root-context | move |
| VisibilityIT | test-root-context | move |

**`embedding`**

| IT | from | type |
|---|---|---|
| PaperInputIT | test-root-context | move |
| PaperSliderIT | test-root-context | move |

**`errorhandling`**

| IT | from | type |
|---|---|---|
| AbstractErrorIT | test-root-context | move |
| ComponentErrorIT | test-root-context | move |
| DomEventStopPropagationAndPreventDefaultIT | test-root-context | move |
| ErrorHandlingIT | test-root-context | move |
| ErrorPageIT | test-root-context | move |
| ExceptionDuringMapSyncIT | test-root-context | move |
| ExceptionInStreamResourceIT | test-root-context | move |
| HasUrlParameterErrorIT | test-root-context | move |
| InternalErrorIT | test-root-context | move |

**`frontend`**

| IT | from | type |
|---|---|---|
| IdTestIT | test-frontend/test-npm | move |
| NpmUsedIT | test-frontend/test-npm | move |
| PolymerIdTestIT | test-frontend/test-npm | move |
| BasicsIT | test-frontend/vite-basics | move |
| ExternalPackageIT | test-frontend/vite-basics | move |
| FileAccessIT | test-frontend/vite-basics | move |
| PostinstallIT | test-frontend/vite-basics | move |
| ReactComponentsIT | test-frontend/vite-basics | move |
| ThemeIT | test-frontend/vite-basics | move |
| ThemeReloadIT | test-frontend/vite-basics | move |
| ViteDevModeIT | test-frontend/vite-basics | move |
| TemplateIT | test-frontend/vite-test-assets | move |
| ComponentAddedViaInitListenerIT | test-npm-only-features/test-npm-general | move |
| ExternalJSModuleIT | test-npm-only-features/test-npm-general | move |
| MultipleNpmPackageAnnotationsIT | test-npm-only-features/test-npm-no-buildmojo | move |

**`i18n`**

| IT | from | type |
|---|---|---|
| LocaleChangeIT | test-root-context | move |

**`lifecycle`**

| IT | from | type |
|---|---|---|
| AbstractDebounceSynchronizeIT | test-root-context | move |
| AllowInertSynchronizedPropertyIT | test-root-context | move |
| AttachExistingElementIT | test-root-context | move |
| AttachListenerIT | test-root-context | move |
| ClientSideValueChangeIT | test-root-context | move |
| DebounceSynchronizePropertyIT | test-root-context | move |
| DetachedTransferProgressListenerIT | test-root-context | move |
| DnDAttachDetachIT | test-root-context | move |
| DomListenerOnAttachIT | test-root-context | move |
| InvalidateHttpSessionIT | test-root-context | move |
| PreserveOnRefreshIT | test-root-context | move |
| PreserveOnRefreshNestedBeforeEnterIT | test-root-context | move |
| PreserveOnRefreshReAddIT | test-root-context | move |
| PreserveOnRefreshShortcutIT | test-root-context | move |
| ResynchronizationIT | test-root-context | move |
| SerializeShortcutIT | test-root-context | move |
| SerializeUIIT | test-root-context | move |
| ShadowRootShortcutsWithValueChangeModeIT | test-root-context | move |
| ShortcutsWithValueChangeModeIT | test-root-context | move |
| SynchronizedPropertyIT | test-root-context | move |
| TriggerAfterIT | test-root-context | move |
| TriggerDownloadIT | test-root-context | move |
| TriggerFilePasteIT | test-root-context | move |
| TriggerOpenInNewTabIT | test-root-context | move |
| TriggerPasteIT | test-root-context | move |
| TriggerReadFromClipboardIT | test-root-context | move |
| TriggerRequestFullscreenIT | test-root-context | move |
| TriggerSizeIT | test-root-context | move |
| TriggerWriteToClipboardIT | test-root-context | move |
| UIsCollectedWithBeaconAPIIT | test-root-context | move |
| ValueChangeModeIT | test-root-context | move |
| WaitForVaadinIT | test-root-context | move |

**`misc`**

| IT | from | type |
|---|---|---|
| CompressedResourceIT | test-misc | move |
| ExceptionLoggingIT | test-misc | move |
| JavascriptServedAsUtf8IT | test-misc | move |
| MiscelaneousIT | test-misc | move |
| NpmThemedComponentIT | test-misc | move |
| PartialMatchRefreshIT | test-misc | move |
| PreserveOnRefreshCloseUIsIT | test-misc | move |
| ProdModeConfigIT | test-misc | move |
| TranslationIT | test-misc | move |

**`push`**

| IT | from | type |
|---|---|---|
| AbstractClientServerCounterIT | test-root-context | move |
| AbstractPushLargeDataIT | test-root-context | move |
| BasicPollIT | test-root-context | move |
| BasicPushIT | test-root-context | move |
| BasicPushLongPollingIT | test-root-context | move |
| BasicPushWebsocketIT | test-root-context | move |
| BasicPushWebsocketXhrIT | test-root-context | move |
| EnableDisablePushIT | test-root-context | move |
| IdlePushChannelIT | test-root-context | move |
| IdlePushChannelLongPollingIT | test-root-context | move |
| IdlePushChannelWebsocketIT | test-root-context | move |
| LongPollingMultipleThreadsIT | test-root-context | move |
| LongPollingPushIT | test-root-context | move |
| MakeComponentVisibleWithPushIT | test-root-context | move |
| ManualLongPollingPushIT | test-root-context | move |
| PushConfigurationLongPollingIT | test-root-context | move |
| PushConfigurationWebSocketIT | test-root-context | move |
| PushErrorHandlingIT | test-root-context | move |
| PushLargeDataLongPollingIT | test-root-context | move |
| PushLargeDataWebsocketIT | test-root-context | move |
| PushLongPollingUpdateDivIT | test-root-context | move |
| PushLongPollingWithPreserveOnRefreshIT | test-root-context | move |
| PushSettingsIT | test-root-context | move |
| PushToggleComponentVisibilityIT | test-root-context | move |
| PushWSUpdateDivIT | test-root-context | move |
| PushWithPreserveOnRefreshIT | test-root-context | move |
| PushWithRequireJSIT | test-root-context | move |
| ReconnectLongPollingIT | test-root-context | move |
| ReconnectWebsocketIT | test-root-context | move |
| RedirectToPushIT | test-root-context | move |
| SendMultibyteCharactersLongPollingIT | test-root-context | move |
| SendMultibyteCharactersWebSocketIT | test-root-context | move |
| TogglePushIT | test-root-context | move |
| VaadinPushScriptIT | test-root-context | move |

**`react`**

| IT | from | type |
|---|---|---|
| FlowInReactComponentIT | test-react-adapter | move |
| ReactAdapterIT | test-react-adapter | move |

**`routing`**

| IT | from | type |
|---|---|---|
| AddQueryParamIT | test-react-router | move |
| BackNavIT | test-react-router | move |
| BasePathIT | test-react-router | move |
| ForwardTargetIT | test-react-router | move |
| NavigationIT | test-react-router | move |
| StateIT | test-react-router | move |
| BackButtonServerRoundTripIT | test-root-context | move |
| BaseHrefIT | test-root-context | move |
| BrokenRouterLinkIT | test-root-context | move |
| DnDAttachToDropLocationIT | test-root-context | move |
| DynamicallyRegisteredRouteIT | test-root-context | move |
| ForwardToIT | test-root-context | move |
| FragmentLinkIT | test-root-context | move |
| GeolocationIT | test-root-context | move |
| InfiniteRerouteLoopIT | test-root-context | move |
| NavigationEventsIT | test-root-context | move |
| NavigationTriggerIT | test-root-context | move |
| PopStateHandlerIT | test-root-context | move |
| PreserveOnRefreshForwardingIT | test-root-context | move |
| PreserveOnRefreshNavigationIT | test-root-context | move |
| PushRouteNotFoundIT | test-root-context | move |
| PushRouteWildcardParameterIT | test-root-context | move |
| RedirectToSameViewIT | test-root-context | move |
| RefreshCurrentPreserveOnRefreshRouteIT | test-root-context | move |
| RefreshCurrentRouteIT | test-root-context | move |
| RefreshCurrentRouteRedirectIT | test-root-context | move |
| RemoveRoutersLayoutContentIT | test-root-context | move |
| RequestParametersHistoryIT | test-root-context | move |
| RequestParametersIT | test-root-context | move |
| RerouteIT | test-root-context | move |
| RouteAndQueryParametersIT | test-root-context | move |
| RouteHierarchyIT | test-root-context | move |
| RouteNotFoundDevModeIT | test-root-context | move |
| RouteNotFoundIT | test-root-context | move |
| RouterIT | test-root-context | move |
| RouterLinkIT | test-root-context | move |
| RouterParallelIT | test-root-context | move |
| RouterSessionExpirationIT | test-root-context | move |
| RouterStateAIT | test-root-context | move |
| SetParameterForwardToIT | test-root-context | move |
| SetParameterRerouteToIT | test-root-context | move |
| ViewTitleIT | test-root-context | move |

**`scroll`**

| IT | from | type |
|---|---|---|
| AbstractScrollIT | test-root-context | move |
| BodyScrollIT | test-root-context | move |
| CustomScrollCallbacksIT | test-root-context | move |
| MultipleAnchorsIT | test-root-context | move |
| PushStateScrollIT | test-root-context | move |
| ScrollIT | test-root-context | move |
| ScrollableViewIT | test-root-context | move |
| ServerRequestScrollIT | test-root-context | move |

**`signals`**

| IT | from | type |
|---|---|---|
| BindEnabledIT | test-root-context | move |
| BindValueIT | test-root-context | move |
| BindVisibleIT | test-root-context | move |
| BindWidthHeightIT | test-root-context | move |
| ElementPropertySignalBindingIT | test-root-context | move |
| SharedValueSignalIT | test-root-context | move |
| TriggerSetSignalIT | test-root-context | move |

**`templates.lit`**

| IT | from | type |
|---|---|---|
| AnchorInsideTemplateIT | test-root-context | move |
| InjectingTemplateIT | test-root-context | move |
| InnerTemplateVisibilityIT | test-root-context | move |
| LitTemplateAttributeIT | test-root-context | move |
| ReattachIT | test-root-context | move |
| SetInitialTextLitIT | test-root-context | move |
| SimpleLitTemplateNoShadowRootIT | test-root-context | move |
| SimpleLitTemplateShadowRootIT | test-root-context | move |

**`templates.polymer`**

| IT | from | type |
|---|---|---|
| AfterServerChangesIT | test-root-context | move |
| AttachExistingDomElementByIdIT | test-root-context | move |
| BasicTypeInListIT | test-root-context | move |
| BeanInListingIT | test-root-context | move |
| ChangeInjectedComponentTextIT | test-root-context | move |
| ChildOrderIT | test-root-context | move |
| ClearListIT | test-root-context | move |
| ClearNodeChildrenIT | test-root-context | move |
| ConvertToBeanIT | test-root-context | move |
| DomRepeatIT | test-root-context | move |
| EmptyListsIT | test-root-context | move |
| EventHandlerIT | test-root-context | move |
| ExceptionsDuringPropertyUpdatesIT | test-root-context | move |
| HiddenTemplateIT | test-root-context | move |
| InjectScriptTagIT | test-root-context | move |
| InjectedElementInsideMixinBehaviorIT | test-root-context | move |
| InjectsJsTemplateIT | test-root-context | move |
| InvisibleDefaultPropertyValueIT | test-root-context | move |
| JsGrandParentIT | test-root-context | move |
| LazyWidgetIT | test-root-context | move |
| ListBindingIT | test-root-context | move |
| ListInsideListBindingIT | test-root-context | move |
| ModelListIT | test-root-context | move |
| MutationSeveralSyncedPropsIT | test-root-context | move |
| OneWayPolymerBindingIT | test-root-context | move |
| PolymerDefaultPropertyValueIT | test-root-context | move |
| PolymerModelPropertiesIT | test-root-context | move |
| PolymerPropertiesIT | test-root-context | move |
| PolymerPropertyChangeEventIT | test-root-context | move |
| PolymerPropertyMutationInObserverIT | test-root-context | move |
| PolymerTemplateWithoutShadowRootIT | test-root-context | move |
| PropertiesUpdatedBeforeChangeEventsIT | test-root-context | move |
| RestoreViewWithAttachedByIdIT | test-root-context | move |
| SetInitialTextIT | test-root-context | move |
| SubPropertyModelIT | test-root-context | move |
| TemplateAttributeIT | test-root-context | move |
| TemplateHasInjectedSubTemplateIT | test-root-context | move |
| TemplateInTemplateIT | test-root-context | move |
| TemplateInTemplateWithIdIT | test-root-context | move |
| TemplateMappingDetectorIT | test-root-context | move |
| TemplateScalabilityIT | test-root-context | move |
| TemplateWithConnectedCallbacksIT | test-root-context | move |
| TemplatesVisibilityIT | test-root-context | move |
| ToggleNullListIT | test-root-context | move |
| TwoWayListBindingIT | test-root-context | move |
| TwoWayPolymerBindingIT | test-root-context | move |
| UpdatableModelPropertiesIT | test-root-context | move |
| UpgradeElementIT | test-root-context | move |

**`theming`**

| IT | from | type |
|---|---|---|
| DirectionChangeIT | test-root-context | move |
| ElementStyleIT | test-root-context | move |
| StyleBindIT | test-root-context | move |
| StylePriorityIT | test-root-context | move |
| StyleRemovalIT | test-root-context | move |

### → test-dev-bundle


**`frontend`**

| IT | from | type |
|---|---|---|
| ChangeFrontendContentIT | test-express-build/test-dev-bundle-frontend-add-on | move |
| DevBundleCssImportIT | test-express-build/test-dev-bundle-frontend-add-on | move |
| DevBundleJsModuleIT | test-express-build/test-dev-bundle-frontend-add-on | move |
| TodoIT | test-express-build/test-dev-bundle-frontend-add-on | move |
| ViteImportedCSSIT | test-express-build/test-dev-bundle-frontend-add-on | move |
| AddOnIT | test-express-build/test-dev-bundle-java-add-on | move |
| NoAppBundleIT | test-express-build/test-dev-bundle-no-plugin | move |

### → test-eager-bootstrap


**`bootstrap`**

| IT | from | type |
|---|---|---|
| BasicViewsIT | test-eager-bootstrap | move |
| LowLevelFetchIT | test-eager-bootstrap | move |
| ParameterIT | test-eager-bootstrap | move |

### → test-embedding


**`embedding`**

| IT | from | type |
|---|---|---|
| DefaultValueInitializationIT | test-embedding/embedding-test-assets | move |
| EmbeddedWebComponentIT | test-embedding/embedding-test-assets | move |
| FactoryExporterIT | test-embedding/embedding-test-assets | move |
| FireEventIT | test-embedding/embedding-test-assets | move |
| NpmOnlyIndexIT | test-embedding/embedding-test-assets | move |
| PreserveOnRefreshIT | test-embedding/embedding-test-assets | move |
| PushAnnotationIT | test-embedding/embedding-test-assets | move |
| StreamResourceIT | test-embedding/embedding-test-assets | move |
| UpdatePropertyIT | test-embedding/embedding-test-assets | move |
| WebComponentIT | test-embedding/embedding-test-assets | move |
| ApplicationThemeComponentIT | test-embedding/test-embedding-application-theme | move |
| ApplicationThemeComponentIT | test-embedding/test-embedding-reusable-theme | move |
| ExporterIT | test-embedding/test-embedding-style-containment | move |
| MainIT | test-embedding/test-embedding-style-containment | move |
| ThemedVariantComponentIT | test-embedding/test-embedding-theme-variant | move |
| ApplicationThemeComponentIT | test-express-build/test-embedding-express-build | move |
| MyViewIT | test-express-build/test-embedding-express-build | move |
| BasicComponentIT | test-frontend/vite-embedded | move |
| PushComponentIT | test-frontend/vite-embedded | move |
| BasicComponentIT | test-frontend/vite-embedded-no-theme | move |
| BasicComponentIT | test-frontend/vite-embedded-webcomponent-resync | move |
| BasicComponentIT | test-frontend/vite-embedded-webcomponent-resync-longpolling | move |
| BasicComponentIT | test-frontend/vite-embedded-webcomponent-resync-ws | move |
| BasicComponentIT | test-frontend/vite-embedded-webcomponent-resync-wsxhr | move |

### → test-fault-tolerance(keep)


**`fault-tolerance`**

| IT | from | type |
|---|---|---|
| NetworkInterruptionIT | test-root-context | keep |

### → test-livereload


**`devmode`**

| IT | from | type |
|---|---|---|
| AbstractLiveReloadIT | test-live-reload | move |
| FrontendLiveReloadIT | test-live-reload | move |
| JavaLiveReloadIT | test-live-reload | move |
| PreserveOnRefreshLiveReloadIT | test-live-reload | move |
| ScrollPositionLiveReloadIT | test-live-reload | move |
| StylesheetLiveReloadIT | test-live-reload | move |
| ThemeLiveReloadIT | test-live-reload | move |
| ThemeLiveReloadWithShadowRootIT | test-live-reload | move |
| FrontendLiveReloadIT | test-live-reload-multimodule/ui | move |

### → test-multi-war(keep)


**`infra`**

| IT | from | type |
|---|---|---|
| TwoAppsIT | test-multi-war/deployment | keep |

### → test-no-theme


**`theming`**

| IT | from | type |
|---|---|---|
| NoThemeComponentIT | test-no-theme | move |

### → test-performance-regression(keep)


**`infra`**

| IT | from | type |
|---|---|---|
| StartupPerformanceIT | test-npm-only-features/test-npm-performance-regression | keep |

### → test-plain-servlet


**`devmode`**

| IT | from | type |
|---|---|---|
| NoResponseIT | test-client-queue | move |
| ResyncLoopIT | test-client-queue | move |
| SlowResponseIT | test-client-queue | move |

**`routing`**

| IT | from | type |
|---|---|---|
| CustomRouteIT | test-custom-route-registry | move |

**`servlet`**

| IT | from | type |
|---|---|---|
| LogoutWithNotificationIT | test-root-context | move |
| SyncErrorCustomMessagesIT | test-root-context | move |
| SyncErrorIT | test-root-context | move |
| SyncErrorSilentReloadIT | test-root-context | move |
| NavigationTargetIT | test-servlet | move |

### → test-plain-spring


**`di`**

| IT | from | type |
|---|---|---|
| AppViewIT | vaadin-spring-tests/test-mvc-without-endpoints | move |

### → test-pnpm


**`frontend`**

| IT | from | type |
|---|---|---|
| IdTestIT | test-frontend/test-pnpm | move |
| PnpmUsedIT | test-frontend/test-pnpm | move |
| PolymerIdTestIT | test-frontend/test-pnpm | move |

### → test-production


**`frontend`**

| IT | from | type |
|---|---|---|
| ParentThemeInFrontendIT | test-express-build/test-parent-theme-in-frontend-prod | move |
| ParentThemeIT | test-express-build/test-parent-theme-prod | move |
| NoAppBundleIT | test-express-build/test-prod-bundle-no-plugin | move |
| ThemeComponentsCssIT | test-express-build/test-theme-legacy-components-css-prod | move |
| CompressionIT | test-frontend/vite-production | move |
| ExternalPackageIT | test-frontend/vite-production | move |
| ProductionBasicsIT | test-frontend/vite-production | move |
| ProductionBasicsIT | test-frontend/vite-production-custom-frontend | move |
| ByteCodeScanningIT | test-npm-only-features/test-npm-bytecode-scanning | move |
| FullCPScanningIT | test-npm-only-features/test-npm-bytecode-scanning | move |
| LazyIT | test-npm-only-features/test-npm-bytecode-scanning | move |

**`routing`**

| IT | from | type |
|---|---|---|
| RouteNotFoundProdModeIT | test-root-context | move |

### → test-pwa


**`pwa`**

| IT | from | type |
|---|---|---|
| MainIT | test-frontend/vite-pwa | move |
| MainIT | test-frontend/vite-pwa-custom-offline-path | move |
| MainIT | test-frontend/vite-pwa-custom-sw | move |
| MainIT | test-frontend/vite-pwa-disabled-offline | move |
| MainIT | test-frontend/vite-pwa-production | move |
| PwaTestIT | test-pwa | move |
| MainIT | test-pwa-disabled-offline | move |
| WebPushIT | test-webpush | move |

### → test-redeployment


**`devmode`**

| IT | from | type |
|---|---|---|
| AbstractReloadIT | test-redeployment | move |
| DevModeClassCacheIT | test-redeployment | move |
| SessionValueIT | test-redeployment | move |
| ThemeSwitchLiveReloadIT | test-redeployment | move |
| DevModeNoClassCacheIT | test-redeployment-no-cache | move |

### → test-spring-security


**`security`**

| IT | from | type |
|---|---|---|
| AbstractIT | vaadin-spring-tests/test-spring-security-flow | move |
| AppViewIT | vaadin-spring-tests/test-spring-security-flow | move |
| TranslationIT | vaadin-spring-tests/test-spring-security-flow | move |
| UIAccessContextIT | vaadin-spring-tests/test-spring-security-flow | move |
| AppViewIT | vaadin-spring-tests/test-spring-security-flow-contextpath | move |
| AbstractIT | vaadin-spring-tests/test-spring-security-flow-methodsecurity | move |
| AppViewIT | vaadin-spring-tests/test-spring-security-flow-methodsecurity | move |
| AppViewIT | vaadin-spring-tests/test-spring-security-flow-reverseproxy | move |
| AbstractIT | vaadin-spring-tests/test-spring-security-flow-routepathaccesschecker | move |
| AppViewIT | vaadin-spring-tests/test-spring-security-flow-routepathaccesschecker | move |
| UIAccessContextIT | vaadin-spring-tests/test-spring-security-flow-routepathaccesschecker | move |
| AbstractIT | vaadin-spring-tests/test-spring-security-flow-standalone-routepathaccesschecker | move |
| AppViewIT | vaadin-spring-tests/test-spring-security-flow-standalone-routepathaccesschecker | move |
| UIAccessContextIT | vaadin-spring-tests/test-spring-security-flow-standalone-routepathaccesschecker | move |
| AppViewIT | vaadin-spring-tests/test-spring-security-flow-themes-contextpath | move |
| AppViewIT | vaadin-spring-tests/test-spring-security-flow-themes-urlmapping | move |
| AppViewIT | vaadin-spring-tests/test-spring-security-flow-urlmapping | move |
| AppViewIT | vaadin-spring-tests/test-spring-security-flow-websocket | move |
| CustomWebIconsIT | vaadin-spring-tests/test-spring-security-webicons | move |
| CustomWebIconsIT | vaadin-spring-tests/test-spring-security-webicons-urlmapping | move |

### → test-tailwind


**`theming`**

| IT | from | type |
|---|---|---|
| TailwindCssIT | test-tailwindcss | move |

### → test-themes


**`theming`**

| IT | from | type |
|---|---|---|
| ParentThemeIT | test-application-theme/test-reusable-as-parent-vite | move |
| ComponentThemeLiveReloadIT | test-application-theme/test-theme-component-live-reload | move |
| ThemeLiveReloadIT | test-application-theme/test-theme-live-reload | move |
| ReusableThemeIT | test-application-theme/test-theme-reusable-vite | move |
| TSIT | test-application-theme/test-theme-reusable-vite | move |
| ParentThemeIT | test-express-build/test-parent-theme-express-build | move |
| ParentThemeInFrontendIT | test-express-build/test-parent-theme-in-frontend | move |
| ReusingThemeIT | test-express-build/test-reusing-theme-express-build | move |
| DevBundleThemeIT | test-express-build/test-theme-dev-bundle | move |
| UtilityClassNameIT | test-theme-no-polymer | move |
| ColorSchemeIT | test-themes | move |
| CssLoadingIT | test-themes | move |
| StylesheetCacheBustingIT | test-themes | move |
| ThemeIT | test-themes | move |

### → test-vaadin-router


**`routing`**

| IT | from | type |
|---|---|---|
| AppThemeTestIT | test-ccdm | move |
| IndexHtmlRequestHandlerIT | test-ccdm | move |
| NavigateToServerSideViewWithoutTitleIT | test-ccdm | move |
| ServerSideForwardIT | test-ccdm | move |
| ServerSideNavigationExceptionHandlingIT | test-ccdm | move |
| ServerSidePostponeIT | test-ccdm | move |
| ConnectionIndicatorIT | test-ccdm-flow-navigation | move |
| LoadingIndicatorNavigationIT | test-ccdm-flow-navigation | move |
| NavigateBetweenViewsIT | test-ccdm-flow-navigation | move |
| ServiceWorkerIT | test-ccdm-flow-navigation | move |
| ServiceWorkerOnNestedMappingIT | test-ccdm-flow-navigation | move |
| AddQueryParamIT | test-vaadin-router | reuse |
| BackNavIT | test-vaadin-router | reuse |
| ForwardTargetIT | test-vaadin-router | reuse |
| HistoryIT | test-vaadin-router | reuse |
| NavigationEventsIT | test-vaadin-router | reuse |
| NavigationIT | test-vaadin-router | reuse |
| NavigationTriggerIT | test-vaadin-router | reuse |
| PopStateHandlerIT | test-vaadin-router | reuse |
| PostponeProceedIT | test-vaadin-router | reuse |
| PostponeUpdateIT | test-vaadin-router | reuse |
| RouterLinkIT | test-vaadin-router | reuse |
