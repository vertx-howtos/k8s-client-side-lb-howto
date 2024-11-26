package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.*;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.kube.KubeResolver;
import io.vertx.serviceresolver.kube.KubeResolverOptions;

public class MainVerticle extends AbstractVerticle {

  private HttpClient client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    client = vertx
      .httpClientBuilder()
      .withAddressResolver(KubeResolver.create(new KubeResolverOptions())
      )
      .build();

    ServiceAddress sa = ServiceAddress.of("hello-node");

    StringBuilder sb = new StringBuilder();
    System.getenv().forEach((k, v) -> {
      sb.append(k).append("=").append(v).append("\n");
    });

    vertx.createHttpServer().requestHandler(req -> {
        RequestOptions connect = new RequestOptions()
          .setMethod(HttpMethod.GET)
          .setServer(sa)
          .setHost("localhost")
          .setPort(80)
          .setURI("/");
        Future<HttpClientRequest> fut = client.request(connect);
        fut.compose(r -> r.send()
          .compose(resp -> resp.body()
            .map(body -> "Hello from: " + r.connection().remoteAddress() + " with: " + body + "\n" + sb)))
          .onComplete(ar -> {
          if (ar.succeeded()) {
            req.response()
              .putHeader("content-type", "text/plain")
              .end(ar.result());
          } else {
            req.response()
              .putHeader("content-type", "text/plain")
              .end("Error: " + ar.cause().getMessage());
          }
        });

      }).listen(8080)
      .onComplete(http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port 8080");
        } else {
          startPromise.fail(http.cause());
        }
      });
  }
}
