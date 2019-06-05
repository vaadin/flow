


`test-mixed` is a module that test maven builds in all modes. At the same time it 
checks that the application can be deployed with a custom context in the container, 
custom servlet path and custom route.

The module has different maven build files per each mode combination.

### To run the module use the following:

#### NPM + DEVELOPMENT mode 
 Run `mvn jetty:run` and visit http://localhost:8888/context-path/servlet-path/route-path

#### BOWER + DEVELOPMENT mode 
 Run `mvn jetty:run -f pom-bower-devmode.xml` and visit http://localhost:8888/context-path/servlet-path/route-path

#### NPM + PRODUCTION mode 
 Run `mvn jetty:run -f pom-npm-production.xml` and visit http://localhost:8888/context-path/servlet-path/route-path

#### BOWER + PRODUCTION mode 
 Run `mvn jetty:run -f pom-bower-production.xml` and visit http://localhost:8888/context-path/servlet-path/route-path


### To run Integration Tests for each mode run:

```
mvn verify
mvn verify -f pom-bower-devmode.xml
mvn verify -f pom-npm-production.xml
mvn verify -f pom-bower-production.xml
```



