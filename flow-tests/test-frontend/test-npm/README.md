`test-pnpm` is a module that tests `npm`. At the same time it 
checks that the application can be deployed with a custom context in the container, 
custom servlet path and custom route.

The module has different maven build files per each mode combination.

### To run the module use the following:

#### DEVELOPMENT mode 
 Run `mvn jetty:run` and visit http://localhost:8888/context-path/servlet-path/route-path

#### PRODUCTION mode 
 Run `mvn jetty:run -f pom-production.xml` and visit http://localhost:8888/context-path/servlet-path/route-path


### To run Integration Tests for each mode run:

```
mvn verify
mvn verify -f pom-production.xml
```



