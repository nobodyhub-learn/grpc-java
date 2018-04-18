package io.grpc.examples.routeguide.spring;

import com.google.protobuf.util.JsonFormat;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.FeatureDatabase;
import io.grpc.examples.routeguide.impl.RouteGuideService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Logger;

/**
 * gRPC server
 *
 * @author yan_h
 * @since 2018-04-18.
 */
@Component
public class RouteGuideSpringServer {
    private static Logger logger = Logger.getLogger(RouteGuideSpringServer.class.getName());
    @Value("${grpc.port}")
    private int port;

    private Server server;

    @PostConstruct
    public void serverStart() throws IOException, InterruptedException {
        this.server = ServerBuilder.forPort(port)
                .addService(new RouteGuideService(parseFeaturesFromFile()))
                .build();
        this.server.start();
        logger.info("Server started, listening on " + port);
//        blockUntilShutdown(); // if not web application, need to block
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

    @PreDestroy
    public void serverShutdown() {
        if (server != null) {
            server.shutdown();
        }
        logger.severe("*** server shut down");
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
