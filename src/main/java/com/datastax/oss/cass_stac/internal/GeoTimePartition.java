package com.datastax.oss.cass_stac.internal;

import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.*;
import java.time.*;
import java.time.temporal.IsoFields;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GeoTimePartition extends GeoPartition {
    public enum TimeResolution {
        DAY, WEEK, FORTNIGHT, MONTH, QUARTER, YEAR
    }

    public static final TimeResolution DEFAULT_TIME_RESOLUTION = TimeResolution.MONTH;
    private static final ZoneId BASE_TIME_ZONE = ZoneId.of("UTC");

    private final TimeResolution timeResolution;

    public GeoTimePartition(int geoResolution, TimeResolution timeResolution) {
        super(geoResolution);
        this.timeResolution = timeResolution;
    }

    public GeoTimePartition(int geoResolution) {
        this(geoResolution, DEFAULT_TIME_RESOLUTION);
    }

    public GeoTimePartition(TimeResolution timeResolution) {
        this(DEFAULT_RESOLUTION, timeResolution);
    }

    public GeoTimePartition() {
        this(DEFAULT_RESOLUTION, DEFAULT_TIME_RESOLUTION);
    }

    public String getGeoTimePartitionForPoint(@NotNull Point point, @NotNull ZonedDateTime dateTime) {
        String spatialIndex = getGeoPartitionForPoint(point);
        String temporalIndex = getTimePartition(dateTime);
        return spatialIndex + "-" + temporalIndex;
    }

    public Stream<String> streamGeoTimePartitions(@NotNull Polygon polygon, @NotNull ZonedDateTime minDateTime, @NotNull ZonedDateTime maxDateTime) {
        Stream<String> spatialIndexes = streamGeoPartitions(polygon);
        List<String> timePartitions = getDateRange(minDateTime, maxDateTime)
                .map(this::getTimePartition)
                .distinct()
                .toList();

        return spatialIndexes.flatMap(spatialIndex -> timePartitions.stream()
                .map(timePartition -> spatialIndex + "-" + timePartition));
    }

    public List<String> getGeoTimePartitions(@NotNull Polygon polygon, @NotNull ZonedDateTime minDateTime, @NotNull ZonedDateTime maxDateTime) {
        return streamGeoTimePartitions(polygon, minDateTime, maxDateTime)
                .collect(Collectors.toList());
    }

    private Stream<LocalDate> getDateRange(ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
        LocalDate baseStartDate = startDateTime.withZoneSameInstant(BASE_TIME_ZONE).toLocalDate();
        LocalDate baseEndDate = endDateTime.withZoneSameInstant(BASE_TIME_ZONE).toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(baseStartDate, baseEndDate);
        return IntStream.rangeClosed(0, (int) daysBetween)
                .mapToObj(baseStartDate::plusDays);
    }

    private String getTimePartition(LocalDate date) {
        int year = date.getYear();

        return switch (timeResolution) {
            case DAY -> {
                int dayOfYear = date.getDayOfYear();
                yield String.format("%d-D%03d", year, dayOfYear);
            }
            case WEEK -> {
                int weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                yield String.format("%d-W%s", year, weekOfYear);
            }
            case FORTNIGHT -> {
                int fortnight = (date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) - 1) / 2 + 1;
                yield String.format("%d-F%02d", year, fortnight);
            }
            case MONTH -> {
                int month = date.getMonthValue();
                yield String.format("%d-M%02d", year, month);
            }
            case QUARTER -> {
                int quarter = (date.getMonthValue() - 1) / 3 + 1;
                yield String.format("%d-Q%d", year, quarter);
            }
            case YEAR -> String.format("%d", year);
        };
    }

    private String getTimePartition(ZonedDateTime dateTime) {
        ZonedDateTime baseDateTime = dateTime.withZoneSameInstant(BASE_TIME_ZONE);
        return getTimePartition(baseDateTime.toLocalDate());
    }
}
