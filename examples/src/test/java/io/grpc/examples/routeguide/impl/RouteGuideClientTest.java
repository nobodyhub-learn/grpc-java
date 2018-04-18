package io.grpc.examples.routeguide.impl;

import com.google.protobuf.util.JsonFormat;
import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.FeatureDatabase;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author yan_h
 * @since 2018-04-18.
 */
public class RouteGuideClientTest {
    @Test
    public void testAsyncClient() throws Exception {
        List<Feature> features = parseFeaturesFromFile();
        try (RouteGuideClient client = new RouteGuideClient("localhost", 8980)) {
            // Looking for a valid feature
            client.getFeatureAsyn(409146138, -746188906);

            // Feature missing.
            client.getFeatureAsyn(0, 0);
            // Looking for features between 40, -75 and 42, -73.
            client.listFeaturesAsync(400000000, -750000000, 420000000, -730000000);
            // Record a few randomly selected points from the features file.
            client.recordRouteAsync(features, 10);
            // Send and receive some notes.
            CountDownLatch finishLatch = client.routeChat();

            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                client.info("routeChat can not finish within 1 minutes");
            }
        }
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

    /**
     * Result error on server side
     *
     * @throws Exception
     */
    @Test
    public void testlockingClient() throws Exception {
        try (RouteGuideClient client = new RouteGuideClient("localhost", 8980)) {
            // Looking for a valid feature
            client.getFeatureBlocking(409146138, -746188906);

            // Feature missing.
            client.getFeatureBlocking(0, 0);
            // Looking for features between 40, -75 and 42, -73.
            client.listFeaturesBlocking(400000000, -750000000, 420000000, -730000000);
        }
    }

    @Test
    public void testFutureClient() throws Exception {
        try (RouteGuideClient client = new RouteGuideClient("localhost", 8980)) {
            // Looking for a valid feature
            client.getFeatureFuture(409146138, -746188906);

            // Feature missing.
            client.getFeatureFuture(0, 0);
        }
    }
}