package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.kube.KubeResolver;
import io.vertx.serviceresolver.kube.KubeResolverOptions;

import java.io.File;

public class MainVerticle extends AbstractVerticle {

  private static final String KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
  private static final String KUBERNETES_SERVICE_PORT = "KUBERNETES_SERVICE_PORT";
  private static final String KUBERNETES_SERVICE_ACCOUNT_TOKEN = "/var/run/secrets/kubernetes.io/serviceaccount/token";
  private static final String KUBERNETES_SERVICE_ACCOUNT_CA = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
  private HttpClient client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

//    String host = System.getenv(KUBERNETES_SERVICE_HOST);
//    String port = System.getenv(KUBERNETES_SERVICE_PORT);
//    File tokenFile = new File(KUBERNETES_SERVICE_ACCOUNT_TOKEN);
//    File ca = new File(KUBERNETES_SERVICE_ACCOUNT_CA);
//    Buffer token;
//    if (tokenFile.exists()) {
//      token = vertx.fileSystem().readFileBlocking(KUBERNETES_SERVICE_ACCOUNT_TOKEN);
//    } else {
//      token = null;
//    }

    client = vertx
      .httpClientBuilder()
      .withAddressResolver(KubeResolver.create(vertx, new KubeResolverOptions()
//        .setNamespace("default")
//        .setHost(host)
//        .setPort(Integer.parseInt(port))
//        .setHttpClientOptions(new HttpClientOptions().setSsl(true).setTrustAll(true))
//        .setWebSocketClientOptions(new WebSocketClientOptions().setSsl(true).setTrustAll(true))
//        .setBearerToken(token.toString())
        )
      )
      .build();

    ServiceAddress sa = ServiceAddress.create("hello-node");

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
            .map(body -> "Hello from: " + r.connection().remoteAddress() + " with: " + body)))
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
