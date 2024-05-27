package com.datastax.oss.cass_stac.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.locationtech.jts.geom.*;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GeoTimePartitionTest {
    private GeoTimePartition geoTimePartition;
    private GeometryFactory geometryFactory;
    private Polygon sfTriangle;
    private List<String> sfTriangleGeoPartitions;

    @BeforeEach
    public void setUp() {
        geoTimePartition = new GeoTimePartition(GeoTimePartition.TimeResolution.MONTH);
        geometryFactory = new GeometryFactory();

        sfTriangle = geometryFactory.createPolygon(new Coordinate[]{
                new Coordinate(-122.4089866999972145, 37.8133189999832380),
                new Coordinate(-122.3544736999993603, 37.7198061999978478),
                new Coordinate(-122.4798767000009008, 37.8151571999998453),
                new Coordinate(-122.4089866999972145, 37.8133189999832380)
        });

        sfTriangleGeoPartitions = Arrays.asList(
                "862830827ffffff",
                "862830877ffffff",
                "86283082fffffff",
                "86283080fffffff"
        );
    }

    @Test
    public void test_GetGeoTimePartitionForPoint() {
        Point p = sfTriangle.getCentroid();
        String pointPartition = "86283082fffffff";
        ZonedDateTime dateTime = ZonedDateTime.of(2024, 3, 17, 18, 21, 33, 0, ZoneId.of("UTC"));

        assertAll("GeoTimePartitions",
                () -> assertEquals(pointPartition+"-2024", new GeoTimePartition(GeoTimePartition.TimeResolution.YEAR).getGeoTimePartitionForPoint(p, dateTime), "Year Partition"),
                () -> assertEquals(pointPartition+"-2024-Q1", new GeoTimePartition(GeoTimePartition.TimeResolution.QUARTER).getGeoTimePartitionForPoint(p, dateTime), "Quarter Partition"),
                () -> assertEquals(pointPartition+"-2024-M03", new GeoTimePartition(GeoTimePartition.TimeResolution.MONTH).getGeoTimePartitionForPoint(p, dateTime), "Month Partition"),
                () -> assertEquals(pointPartition+"-2024-F06", new GeoTimePartition(GeoTimePartition.TimeResolution.FORTNIGHT).getGeoTimePartitionForPoint(p, dateTime), "Fortnight Partition"),
                () -> assertEquals(pointPartition+"-2024-W11", new GeoTimePartition(GeoTimePartition.TimeResolution.WEEK).getGeoTimePartitionForPoint(p, dateTime), "Week Partition"),
                () -> assertEquals(pointPartition+"-2024-D077", new GeoTimePartition(GeoTimePartition.TimeResolution.DAY).getGeoTimePartitionForPoint(p, dateTime), "Day Partition")
        );
    }

    @Test
    public void test_GetGeoTimePartitions() {
        ZonedDateTime startDateTime = ZonedDateTime.of(2024, 1, 15, 23, 59, 59, 0, ZoneId.of("UTC"));
        ZonedDateTime endDateTime = ZonedDateTime.of(2024, 2, 15, 0, 0, 0, 0, ZoneId.of("UTC"));

        List<String> timePartitions = Arrays.asList("2024-M01", "2024-M02");
        Set<String> expectedPartitions = sfTriangleGeoPartitions.stream()
                .flatMap(spatialPart -> timePartitions.stream()
                        .map(timePart -> spatialPart + "-" + timePart))
                .collect(Collectors.toSet());

        List<String> actualPartitions = geoTimePartition.getGeoTimePartitions(sfTriangle, startDateTime, endDateTime);
        assertFalse(actualPartitions.isEmpty());
        assertTrue(actualPartitions.containsAll(expectedPartitions));
    }
}
