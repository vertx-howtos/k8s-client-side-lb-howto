package io.vertx.howtos.clientsidelb;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.*;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.kube.KubeResolver;
import io.vertx.serviceresolver.kube.KubeResolverOptions;

public class MicroServiceVerticle extends VerticleBase {

  private static final ServiceAddress SERVICE_ADDRESS = ServiceAddress.of("hello-node");

  private HttpClient client;

  @Override
  public Future<?> start() {

    // tag::config[]
    client = vertx
      .httpClientBuilder()
      .withAddressResolver(KubeResolver.create(new KubeResolverOptions()))
      .build();
    // end::config[]

    StringBuilder sb = new StringBuilder();
    System.getenv().forEach((k, v) -> {
      sb.append(k).append("=").append(v).append("\n");
    });

    return vertx.createHttpServer()
      .requestHandler(req -> {
        RequestOptions connect = new RequestOptions()
          .setMethod(HttpMethod.GET)
          .setServer(SERVICE_ADDRESS)
          .setHost("localhost")
          .setPort(80)
          .setURI("/");
        Future<HttpClientRequest> fut = client.request(connect);
        fut.compose(r -> r.send()
            .compose(resp -> resp.body()
              .map(body -> "Hello from: " + r.connection().remoteAddress() + " with: " + body + "\n" + sb)))
          .onSuccess(res -> {
            req.response()
              .putHeader("content-type", "text/plain")
              .end(res);
          })
          .onFailure(cause -> {
            req.response()
              .setStatusCode(500)
              .putHeader("content-type", "text/plain")
              .end("Error: " + cause.getMessage());
          });

      }).listen(8080);
  }
}
