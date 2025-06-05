package com.google.solutions;

import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Server {
  private final @NotNull HttpServer server;

  public Server(int listenPort) throws IOException {
    this.server = HttpServer.create(new InetSocketAddress(listenPort), 0);
    this.server.setExecutor(Executors.newFixedThreadPool(10));

    server.createContext("/", exchange -> {
      exchange.sendResponseHeaders(200, 0); // 200 OK, 0 for chunked encoding

      try (exchange; var writer = new OutputStreamWriter(exchange.getResponseBody())) {
        writer.write("Hello world");
      }
    });

  }

  public void start() {
    this.server.start();
  }
}
