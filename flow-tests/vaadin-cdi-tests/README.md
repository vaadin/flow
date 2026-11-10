# Vaadin CDI tests

Arquillian tests for `vaadin-cdi`. Each test deploys a WAR, assembled by
ShrinkWrap from this module's effective POM, to a real application server, so
one of the container profiles has to be selected — without a profile the tests
are skipped rather than failed:

```sh
mvn verify -pl flow-tests/vaadin-cdi-tests -Ptomee
```

The available profiles are `tomee`, `wildfly`, `payara`, `liberty` and
`tomcat-weld`. Validation runs `tomee` for every change and the rest when the
CDI sources themselves change; see the `cdi-tests` job in
`.github/workflows/validation.yml`.

Run these on **JDK 21**, the version validation uses. On JDK 24 and later
`java.security.Policy.setPolicy` throws `UnsupportedOperationException` (the
Security Manager is gone, JEP 486), and TomEE calls it while installing its
JACC policy provider, so the container never starts and every test fails with
`Could not install JACC Policy Provider` rather than anything about CDI.

The tests drive a real Chrome through TestBench, so a `~/.vaadin/proKey` is
required and browser windows open while they run.

The tests carrying the `SlowTests` category are excluded by default. They
still need a container, so enable them alongside one:

```sh
mvn verify -pl flow-tests/vaadin-cdi-tests -Ptomee,slow-tests
```

### Dependencies for tests

If there is a need to add or remove a dependency from the test module
the library should be also be added/removed from the `base(String warName)` 
method in `ArchiveProvider`.

If the tomee execution doesn't return any clear indication on a failure 
except 404 for a module then running with the wildfly profile will often
yield better exception information.
