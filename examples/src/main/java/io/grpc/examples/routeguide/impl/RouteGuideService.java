package io.grpc.examples.routeguide.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.grpc.examples.routeguide.*;
import io.grpc.stub.StreamObserver;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author Ryan
 */
public class RouteGuideService extends RouteGuideGrpc.RouteGuideImplBase {
    private static final Logger logger = Logger.getLogger(RouteGuideService.class.getName());

    private final List<Feature> features;
    private final ConcurrentMap<Point, List<RouteNote>> routeNotes =
            Maps.newConcurrentMap();

    public RouteGuideService(List<Feature> features) {
        this.features = features;
    }

    @Override
    public void getFeature(Point request, StreamObserver<Feature> responseObserver) {
        responseObserver.onNext(checkFeature(request));
        responseObserver.onCompleted();
    }

    private Feature checkFeature(Point location) {
        for (Feature feature : features) {
            if (feature.getLocation().getLongitude() == location.getLongitude()
                    && feature.getLocation().getLatitude() == location.getLatitude()) {
                return feature;
            }
        }
        return Feature.newBuilder()
                .setName("")
                .setLocation(location)
                .build();
    }

    @Override
    public void listFeatures(Rectangle request, StreamObserver<Feature> responseObserver) {
        int left = min(request.getLo().getLongitude(), request.getHi().getLongitude());
        int right = max(request.getLo().getLongitude(), request.getHi().getLongitude());
        int top = max(request.getLo().getLatitude(), request.getHi().getLatitude());
        int bottom = min(request.getLo().getLatitude(), request.getHi().getLatitude());

        for (Feature feature : features) {
            if (isValidFeature(feature)) {
                continue;
            }
            int lat = feature.getLocation().getLatitude();
            int lon = feature.getLocation().getLongitude();
            if (lon >= left && lon <= right && lat >= bottom && lat <= top) {
                responseObserver.onNext(feature);
            }
        }
        responseObserver.onCompleted();
    }

    protected boolean isValidFeature(Feature feature) {
        return feature == null || Strings.isNullOrEmpty(feature.getName());
    }

    @Override
    public StreamObserver<Point> recordRoute(StreamObserver<RouteSummary> responseObserver) {
        return new StreamObserver<Point>() {
            int pointCnt;
            int featureCnt;
            int distance;
            Point previous;
            final long startTime = System.nanoTime();

            @Override
            public void onNext(Point value) {
                pointCnt++;
                if (isValidFeature(checkFeature(value))) {
                    featureCnt++;
                }
                if (previous != null) {
                    distance += calcDistance(previous, value);
                }
                previous = value;
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("recordRoute cancelled");
            }

            @Override
            public void onCompleted() {
                long seconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
                responseObserver.onNext(RouteSummary.newBuilder()
                        .setPointCount(pointCnt)
                        .setFeatureCount(featureCnt)
                        .setElapsedTime((int) seconds)
                        .setDistance(distance)
                        .build());
                responseObserver.onCompleted();
            }
        };
    }

    protected int calcDistance(Point start, Point end) {
        double lat1 = start.getLatitude();
        double lat2 = end.getLatitude();
        double lon1 = start.getLongitude();
        double lon2 = end.getLongitude();

        return (int) Math.sqrt(Math.pow((lat1 - lat2), 2) + Math.pow((lon1 - lon2), 2));
    }

    @Override
    public StreamObserver<RouteNote> routeChat(StreamObserver<RouteNote> responseObserver) {
        return new StreamObserver<RouteNote>() {
            @Override
            public void onNext(RouteNote value) {
                List<RouteNote> notes = getRouteNotes(value.getLocation());
                for (RouteNote note : notes) {
                    responseObserver.onNext(note);
                }
                notes.add(value);
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("routeChat cancelled");
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    protected List<RouteNote> getRouteNotes(Point location) {
        List<RouteNote> notes = Collections.synchronizedList(Lists.newArrayList());
        List<RouteNote> previousNote = routeNotes.putIfAbsent(location, notes);
        return previousNote != null ? previousNote : notes;
    }
}
