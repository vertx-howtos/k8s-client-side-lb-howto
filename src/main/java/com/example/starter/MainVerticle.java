package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.util.Base64;
import java.util.Map;

public class MainVerticle extends AbstractVerticle {

  private static final String KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
  private static final String KUBERNETES_SERVICE_PORT = "KUBERNETES_SERVICE_PORT";
  private static final String KUBERNETES_SERVICE_ACCOUNT_TOKEN = "/var/run/secrets/kubernetes.io/serviceaccount/token";
  private static final String KUBERNETES_SERVICE_ACCOUNT_CA = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
  private HttpClient client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    String host = System.getenv(KUBERNETES_SERVICE_HOST);
    String port = System.getenv(KUBERNETES_SERVICE_PORT);
    File tokenFile = new File(KUBERNETES_SERVICE_ACCOUNT_TOKEN);
    File ca = new File(KUBERNETES_SERVICE_ACCOUNT_CA);
    Buffer token;
    JsonObject jwt;
    if (tokenFile.exists()) {
      token = vertx.fileSystem().readFileBlocking(KUBERNETES_SERVICE_ACCOUNT_TOKEN);
//      byte[] decoded = Base64.getDecoder().decode(token.toString());
//      jwt = new JsonObject(Buffer.buffer(decoded));
      jwt = null;
    } else {
      token = null;
      jwt = null;
    }

    client = vertx.createHttpClient(new HttpClientOptions()
      .setSsl(true)
      .setTrustAll(true));
    vertx.createHttpServer().requestHandler(req -> {

        Future<HttpClientRequest> fut = client.request(HttpMethod.GET, Integer.parseInt(port), host, "/api/v1/namespaces/" + "default" + "/endpoints");

        fut.compose(r -> {
          if (token != null) {
            r.putHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
          }
          return r.send().compose(HttpClientResponse::body);
        }).onComplete(ar -> {
          StringBuilder sb = new StringBuilder();
          Map<String, String> env = System.getenv();
          env.forEach((key, value) -> sb.append(key).append(":").append(value).append("\r\n"));

          req.response()
            .putHeader("content-type", "text/plain")
            .end("Hello from Vert.x!" +
              "\r\n" + token +
              "\r\n" + jwt +
              "\r\n" + ar.result().toJsonObject().encodePrettily() +
              "\r\n" + sb);
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
