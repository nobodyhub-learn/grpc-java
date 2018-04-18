package io.grpc.examples.routeguide.impl;

import com.google.protobuf.util.JsonFormat;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.FeatureDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ryan
 */
public class RouteGuideServer {
    private static final Logger logger = Logger.getLogger(RouteGuideServer.class.getName());

    private final int port;
    private final Server server;

    public RouteGuideServer(int port) throws IOException {
        this(ServerBuilder.forPort(port), port, parseFeaturesFromFile());
    }

    public RouteGuideServer(ServerBuilder<?> serverBuilder, int port, List<Feature> features) {
        this.port = port;
        this.server = serverBuilder.addService(new RouteGuideService(features)).build();
    }

    protected static List<Feature> parseFeaturesFromFile() throws IOException {
        URL featureUrl = io.grpc.examples.routeguide.RouteGuideServer.class.getResource("route_guide_db.json");
        try (InputStream input = featureUrl.openStream()) {
            try (Reader reader = new InputStreamReader(input, Charset.forName("UTF-8"))) {
                FeatureDatabase.Builder database = FeatureDatabase.newBuilder();
                JsonFormat.parser().merge(reader, database);
                return database.getFeatureList();
            }
        }
    }

    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.severe("*** shutting down gRPC server since JVM is shutting down");
                RouteGuideServer.this.stop();
                logger.severe("*** server shut down");
            }
        });
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws Exception {
        RouteGuideServer server = new RouteGuideServer(8980);
        server.start();
        server.blockUntilShutdown();
    }
}
