package scala.com.vaadin.flow.demo

class WarmupFlow extends Simulation {

  val httpProtocol = http
    .baseURL("http://localhost:8888")
    .warmUp("http://localhost:8888/")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:46.0) Gecko/20100101 Firefox/46.0")

  var scn = scenario("Warm up Jetty").exec(
    http("Open index page")
      .get("/"))

  setUp(scn.inject(atOnceUsers(50))).protocols(httpProtocol)
}
