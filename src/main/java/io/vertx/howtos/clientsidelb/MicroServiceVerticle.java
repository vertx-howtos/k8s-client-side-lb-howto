package io.vertx.howtos.clientsidelb;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.*;
import io.vertx.core.net.AddressResolver;
import io.vertx.core.net.endpoint.LoadBalancer;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.kube.KubeResolver;
import io.vertx.serviceresolver.kube.KubeResolverOptions;

public class MicroServiceVerticle extends VerticleBase {

  private HttpClient client;

  @Override
  public Future<?> start() {

    // tag::resolver[]
    AddressResolver resolver = KubeResolver.create(new KubeResolverOptions());
    // end::resolver[]

    // tag::load-balancer[]
    LoadBalancer loadBalancer = LoadBalancer.ROUND_ROBIN;
    // end::load-balancer[]

    // tag::client[]
    client = vertx
      .httpClientBuilder()
      .withLoadBalancer(loadBalancer)
      .withAddressResolver(resolver)
      .build();
    // end::client[]

    // tag::server[]
    return vertx.createHttpServer()
      .requestHandler(request -> handleRequest(request))
      .listen(8080);
    // end::server[]
  }

  private void handleRequest(HttpServerRequest request) {

    // tag::request[]
    ServiceAddress serviceAddress = ServiceAddress.of("hello-node");

    Future<HttpClientRequest> fut = client.request(new RequestOptions()
      .setMethod(HttpMethod.GET)
      .setServer(serviceAddress)
      .setURI("/"));
    // end::request[]

    // tag::response[]
    fut.compose(r -> r.send()
        .expecting(HttpResponseExpectation.SC_OK)
        .compose(resp -> resp.body())
        .map(body -> "Response of pod " + r.connection().remoteAddress() + ": " + body + "\n"))
      .onSuccess(res -> {
        request.response()
          .putHeader("content-type", "text/plain")
          .end(res);
      })
      .onFailure(cause -> {
        request.response()
          .setStatusCode(500)
          .putHeader("content-type", "text/plain")
          .end("Error: " + cause.getMessage());
      });
    // end::response[]
  }

}
