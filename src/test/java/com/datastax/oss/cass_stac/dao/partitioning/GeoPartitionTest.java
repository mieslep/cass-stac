package com.datastax.oss.cass_stac.dao.partitioning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.locationtech.jts.geom.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class GeoPartitionTest {
    private GeoPartition geoPartition;
    private GeometryFactory geometryFactory;

    private Polygon createPolygon(Coordinate[] coordinates) {
        return geometryFactory.createPolygon(coordinates);
    }

    @BeforeEach
    public void setUp() {
        geoPartition = new GeoPartition();
        geometryFactory = new GeometryFactory();
    }

    @Test
    public void test_GetGeoPartitionForPoint() {
        Polygon sfTriangle = createPolygon(new Coordinate[]{
                new Coordinate(-122.4089866999972145, 37.8133189999832380),
                new Coordinate(-122.3544736999993603, 37.7198061999978478),
                new Coordinate(-122.4798767000009008, 37.8151571999998453),
                new Coordinate(-122.4089866999972145, 37.8133189999832380)
        });

        Point p = sfTriangle.getCentroid();
        String partition = geoPartition.getGeoPartitionForPoint(p);
        assertEquals("86283082fffffff", partition);
    }

    @Test
    public void test_GetGeoPartitions_smallPoly_SpanMultiple() {

        Polygon sfSmallRectangle = createPolygon(new Coordinate[]{
                new Coordinate(-122.485216, 37.783903),
                new Coordinate(-122.484265, 37.772570),
                new Coordinate(-122.438217, 37.776847),
                new Coordinate(-122.440582, 37.788091),
                new Coordinate(-122.485216, 37.783903)
        });

        Set<String> sfSmallRectanglePartitions = new HashSet<>(Arrays.asList(
                "86283082fffffff",
                "86283095fffffff",
                "862830877ffffff"
        ));

        List<String> partitions = geoPartition.getGeoPartitions(sfSmallRectangle);
        assertFalse(partitions.isEmpty());
        assertEquals(3, partitions.size());
        assertEquals(sfSmallRectanglePartitions, new HashSet<>(partitions));
    }

    @Test
    public void test_GetGeoPartitions_mediumPoly_SpanMultiple() {
        Polygon sfTriangle = createPolygon(new Coordinate[]{
                new Coordinate(-122.4089866999972145, 37.8133189999832380),
                new Coordinate(-122.3544736999993603, 37.7198061999978478),
                new Coordinate(-122.4798767000009008, 37.8151571999998453),
                new Coordinate(-122.4089866999972145, 37.8133189999832380)
        });

        Set<String> sfTrianglePartitions = new HashSet<>(Arrays.asList(
                "862830827ffffff",
                "862830877ffffff",
                "86283082fffffff",
                "86283080fffffff"
        ));

        List<String> partitions = geoPartition.getGeoPartitions(sfTriangle);
        assertFalse(partitions.isEmpty());
        assertEquals(4, partitions.size());
        assertEquals(sfTrianglePartitions, new HashSet<>(partitions));
    }
}
