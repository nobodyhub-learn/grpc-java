package io.grpc.examples.routeguide.impl;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.examples.routeguide.*;
import io.grpc.stub.StreamObserver;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author yan_h
 * @since 2018-04-18.
 */
public class RouteGuideClient implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(io.grpc.examples.routeguide.RouteGuideClient.class.getName());

    private final ManagedChannel channel;
    private final RouteGuideGrpc.RouteGuideStub asyncStub;
    private final RouteGuideGrpc.RouteGuideBlockingStub blockingStub;
    private final RouteGuideGrpc.RouteGuideFutureStub futureStub;

    public RouteGuideClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
    }

    public RouteGuideClient(ManagedChannelBuilder<?> channelBuilder) {
        this.channel = channelBuilder.build();
        this.asyncStub = RouteGuideGrpc.newStub(channel);
        this.blockingStub = RouteGuideGrpc.newBlockingStub(channel);
        this.futureStub = RouteGuideGrpc.newFutureStub(channel);
    }

    /**
     * since the method is unary, non-stream,
     * methods of StreamObserver, onNext/onError/onComplete will not be executed
     * <p>
     * cause error on server side
     *
     * @param lat
     * @param lon
     */
    public void getFeatureAsyn(int lat, int lon) throws InterruptedException {
        info("getFeatureAsyn: lat={0}, lon={1}", lat, lon);
        final long startTime = System.nanoTime();
        CountDownLatch finishLatch = new CountDownLatch(1);

        Point point = Point.newBuilder()
                .setLatitude(lat)
                .setLongitude(lon)
                .build();

        asyncStub.getFeature(point, new StreamObserver<Feature>() {
            @Override
            public void onNext(Feature feature) {
                if (feature != null && !Strings.isNullOrEmpty(feature.getName())) {
                    info("Found feature called: \"{0}\" at ({1}, {2})",
                            feature,
                            feature.getLocation().getLatitude(),
                            feature.getLocation().getLongitude());
                } else {
                    info("Can not found feature at ({0}, {1})", lat, lon);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                info("getFeatureAsyn encounter error: {0}", throwable.getMessage());
                finishLatch.countDown();
                throw new RuntimeException(throwable);
            }

            @Override
            public void onCompleted() {
                info("getFeatureAsyn Finished");
                finishLatch.getCount();
            }
        });

        info("getFeatureAsyn finish within: {0}nanoseconds", System.nanoTime() - startTime);
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            info("recordRoute can not finish within 1 minutes");
        }
    }

    public void getFeatureBlocking(int lat, int lon) {
        info("getFeatureBlocking: lat={0}, lon={1}", lat, lon);
        final long startTime = System.nanoTime();

        Point point = Point.newBuilder()
                .setLatitude(lat)
                .setLongitude(lon)
                .build();
        Feature feature = blockingStub.getFeature(point);
        if (feature != null && !Strings.isNullOrEmpty(feature.getName())) {
            info("Found feature called: \"{0}\" at ({1}, {2})",
                    feature,
                    feature.getLocation().getLatitude(),
                    feature.getLocation().getLongitude());
        } else {
            info("Can not found feature at ({0}, {1})", lat, lon);
        }
        info("getFeatureBlocking finish within: {0} nanoseconds", System.nanoTime() - startTime);
    }

    public void getFeatureFuture(int lat, int lon) throws ExecutionException, InterruptedException {
        info("getFeatureFuture: lat={0}, lon={1}", lat, lon);
        final long startTime = System.nanoTime();

        Point point = Point.newBuilder()
                .setLatitude(lat)
                .setLongitude(lon)
                .build();
        ListenableFuture<Feature> featureFuture = futureStub.getFeature(point);
        info("getFeatureFuture finish within: {0} nanoseconds", System.nanoTime() - startTime);
        Feature feature = featureFuture.get();
        if (feature != null && !Strings.isNullOrEmpty(feature.getName())) {
            info("Found feature called: \"{0}\" at ({1}, {2})",
                    feature,
                    feature.getLocation().getLatitude(),
                    feature.getLocation().getLongitude());
        } else {
            info("Can not found feature at ({0}, {1})", lat, lon);
        }
    }

    public void listFeaturesBlocking(int lowLat, int lowLon, int hiLat, int hiLon) {
        info("*** listFeaturesBlocking: lowLat={0} lowLon={1} hiLat={2} hiLon={3}", lowLat, lowLon, hiLat,
                hiLon);
        Rectangle request =
                Rectangle.newBuilder()
                        .setLo(Point.newBuilder()
                                .setLatitude(lowLat)
                                .setLongitude(lowLon)
                                .build())
                        .setHi(Point.newBuilder()
                                .setLatitude(hiLat)
                                .setLongitude(hiLon)
                                .build())
                        .build();
        Iterator<Feature> featureIter = blockingStub.listFeatures(request);
        int idx = 1;
        while (featureIter.hasNext()) {
            Feature feature = featureIter.next();
            info("listFeaturesBlocking Result #{0}: {1}", idx++, feature);
        }
    }

    public void listFeaturesAsync(int lowLat, int lowLon, int hiLat, int hiLon) throws InterruptedException {
        info("*** listFeaturesAsync: lowLat={0} lowLon={1} hiLat={2} hiLon={3}", lowLat, lowLon, hiLat,
                hiLon);
        CountDownLatch latch = new CountDownLatch(1);
        Rectangle request =
                Rectangle.newBuilder()
                        .setLo(Point.newBuilder()
                                .setLatitude(lowLat)
                                .setLongitude(lowLon)
                                .build())
                        .setHi(Point.newBuilder()
                                .setLatitude(hiLat)
                                .setLongitude(hiLon)
                                .build())
                        .build();
        asyncStub.listFeatures(request, new StreamObserver<Feature>() {
            int idx = 0;

            @Override
            public void onNext(Feature value) {
                info("listFeaturesAsync Result #{0}: {1}", idx++, value);
            }

            @Override
            public void onError(Throwable t) {
                info("listFeaturesAsync ERROR: {0}", t);
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                info("listFeaturesAsync COMPLETE!");
                latch.countDown();
            }
        });
        if(latch.await(1, TimeUnit.MINUTES)) {
            info("recordRoute can not finish within 1 minutes");
        }
    }

    public void recordRouteAsync(List<Feature> features, int numPoints) throws InterruptedException {
        info("*** recordRouteAsync");
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RouteSummary> response = new StreamObserver<RouteSummary>() {
            @Override
            public void onNext(RouteSummary summary) {
                info("Finished trip with {0} points. Passed {1} features. "
                                + "Travelled {2} meters. It took {3} seconds.", summary.getPointCount(),
                        summary.getFeatureCount(), summary.getDistance(), summary.getElapsedTime());
            }

            @Override
            public void onError(Throwable t) {
                info("RecordRoute Failed: {0}", Status.fromThrowable(t));
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                info("Finished RecordRoute");
                finishLatch.countDown();
            }
        };

        StreamObserver<Point> request = asyncStub.recordRoute(response);
        for (int i = 0; i < numPoints; ++i) {
            int idx = new Random().nextInt(features.size());
            Point point = features.get(idx).getLocation();
            info("Visiting point {0}, {1}", point.getLatitude(),
                    point.getLongitude());
            request.onNext(point);
            if (finishLatch.getCount() == 0) {
                return;
            }
        }
        request.onCompleted();
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            info("recordRoute can not finish within 1 minutes");
        }
    }

    public CountDownLatch routeChat() {
        info("*** RouteChat");
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RouteNote> requestObserver =
                asyncStub.routeChat(new StreamObserver<RouteNote>() {
                    @Override
                    public void onNext(RouteNote note) {
                        info("Got message \"{0}\" at {1}, {2}", note.getMessage(), note.getLocation()
                                .getLatitude(), note.getLocation().getLongitude());
                    }

                    @Override
                    public void onError(Throwable t) {
                        info("RouteChat Failed: {0}", Status.fromThrowable(t));
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        info("Finished RouteChat");
                        finishLatch.countDown();
                    }
                });
        RouteNote[] requests =
                {newNote("First message", 0, 0), newNote("Second message", 0, 1),
                        newNote("Third message", 1, 0), newNote("Fourth message", 1, 1)};

        for (RouteNote request : requests) {
            info("Sending message \"{0}\" at {1}, {2}", request.getMessage(), request.getLocation()
                    .getLatitude(), request.getLocation().getLongitude());
            requestObserver.onNext(request);
        }
        requestObserver.onCompleted();
        return finishLatch;
    }

    private RouteNote newNote(String message, int lat, int lon) {
        return RouteNote.newBuilder().setMessage(message)
                .setLocation(Point.newBuilder().setLatitude(lat).setLongitude(lon).build()).build();
    }

    @Override
    public void close() throws Exception {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    protected void info(String msg, Object... params) {
        logger.log(Level.INFO, msg, params);
    }


}
