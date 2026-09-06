/*
 * Copyright 2026 Daniel-Gheorghe Popiniuc
 */
package io.github.dgp_eu.software_releases;

import static java.lang.Math.*;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.dgp_eu.tools.core.LogExposureClass;
import io.github.dgp_eu.tools.core.time.TimingClass;

/**
 * Sun position class
 */
public final class SunClass {
    /** Constant for Next Event */
    private static final String NEXT_EVENT = "Next event";
    /** Constant for Prior Event */
    private static final String PRIOR_EVENT = "Prior event";
    /** Zenith for official sunrise/sunset (90° 50') */
    private static final double ZENITH = 90.833;
    /** Latitude variable */
    private static double dblLatitude;
    /** Longitude variable */
    private static double dblLongitude;
    /** ZoneId variable */
    private static ZoneId internalZoneId;
    /** Properties for output */
    private static final Map<String, Object> MAP_SUN = new ConcurrentHashMap<>();
    /** formatter Variable */
    /* default */ private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TimingClass.DATE_TIME_MS_LONG, Locale.US);

    /**
     * Calculates Sunrise and Sunset for a given location
     * @param crtLocationDetail location detail as String
     * @return Properties
     */
    public static Map<String, Object> getSunRiseAndSet(final String crtLocationDetail) {
        final ZonedDateTime nowZ = ZonedDateTime.now(internalZoneId);
        MAP_SUN.put("Location [Street, City, Division, Country]", crtLocationDetail);
        MAP_SUN.put("Current Timestamp", nowZ.format(formatter));
        final ZonedDateTime sunrise = calculateSunSetOrRise(nowZ, true);
        if (sunrise != null) {
            MAP_SUN.put("Today Sunrise", sunrise.format(formatter));
        } else {
            MAP_SUN.put("Today Sunrise", "Sun does not rise on this date at this location");
        }
        final ZonedDateTime sunset = calculateSunSetOrRise(nowZ, false);
        if (sunset != null) {
            MAP_SUN.put("Today Sunset", sunset.format(formatter));
        } else {
            MAP_SUN.put("Today Sunset", "Sun does not set on this date at this location");
        }
        if ((sunrise != null)
            && (sunset != null)) {
            MAP_SUN.put("Today Light duration", TimingClass.AgingSubClass.computeAgingIntoHumanReadableWords(sunrise, sunset));
            enhanceSunStatistics(nowZ, sunrise, sunset);
        }
        return MAP_SUN;
    }

    /**
     * Calculate Sunrise/Sunset
     * @param inNowZ input time-stamp
     * @param isSunrise boolean if Sunrise 
     * @return ZonedDateTime
     */
    private static ZonedDateTime calculateSunSetOrRise(final ZonedDateTime inNowZ, final boolean isSunrise) {
        final LocalDate inLocalDate = inNowZ.toLocalDate();
        final int dayOfYear = inLocalDate.getDayOfYear();
        // 1. Convert longitude to hour value and estimate time
        final double lonHour = dblLongitude / 15.0;
        final double estimatedTime = dayOfYear + ((isSunrise ? 6.0 : 18.0) - lonHour) / 24.0;
        // 2. Sun's mean anomaly
        final double sunMeanAnomaly = (0.9856 * estimatedTime) - 3.289;
        // 3. Sun's true longitude
        double sunLongitude = sunMeanAnomaly
                + (1.916 * sin(toRadians(sunMeanAnomaly)))
                + (0.020 * sin(toRadians(2 * sunMeanAnomaly)))
                + 282.634;
        sunLongitude = (sunLongitude + 360) % 360;
        // 4. Sun's right ascension
        double sunRightAscension = toDegrees(atan(0.917_64 * tan(toRadians(sunLongitude))));
        sunRightAscension = (sunRightAscension + 360) % 360;
        // Adjust quadrant of sunRightAscension
        final double lQuadrant = floor(sunLongitude / 90) * 90;
        final double raQuadrant = floor(sunRightAscension / 90) * 90;
        sunRightAscension = (sunRightAscension + (lQuadrant - raQuadrant)) / 15.0;
        // 5. Sun's declination
        final double sinDec = 0.397_82 * sin(toRadians(sunLongitude));
        final double cosDec = cos(asin(sinDec));
        // 6. Local hour angle
        final double cosH = (cos(toRadians(ZENITH))
                - (sinDec * sin(toRadians(dblLatitude)))) / (cosDec * cos(toRadians(dblLatitude)));
        ZonedDateTime outZonedDateTime = null;
        if (cosH >= -1
                && cosH <= 1) { // only if Sun rises/sets
            // 7. Local mean time
            final double localMeanHour = (isSunrise ? (360 - toDegrees(acos(cosH))) : toDegrees(acos(cosH))) / 15.0;
            final double localMeanTime = localMeanHour + sunRightAscension - (0.065_71 * estimatedTime) - 6.622;
            // 8. UTC time
            final double utcTime = (localMeanTime - lonHour + 24) % 24;
            // 9. Convert to ZonedDateTime
            final LocalTime finalTime = LocalTime.ofNanoOfDay((long)(utcTime * 3_600_000_000_000L));
            outZonedDateTime = ZonedDateTime.of(inLocalDate, finalTime, ZoneOffset.UTC).withZoneSameInstant(internalZoneId);
        }
        return outZonedDateTime;
    }

    /**
     * Enhances sun position details
     * @param nowZ now with Time Zone
     * @param sunrise sun rise time
     * @param sunset sun set time
     */
    private static void enhanceSunStatistics(final ZonedDateTime nowZ, final ZonedDateTime sunrise, final ZonedDateTime sunset) {
        final ZonedDateTime yesterdayZ = ZonedDateTime.now(internalZoneId).minusDays(1);
        final ZonedDateTime sunrisePrior = calculateSunSetOrRise(yesterdayZ, true);
        MAP_SUN.put("Yesterday Sunrise", sunrisePrior.format(formatter));
        final ZonedDateTime sunsetPrior = calculateSunSetOrRise(yesterdayZ, false);
        MAP_SUN.put("Yesterday Sunset", sunsetPrior.format(formatter));
        MAP_SUN.put("Yesterday Light duration", TimingClass.AgingSubClass.computeAgingIntoHumanReadableWords(sunrisePrior, sunsetPrior));
        MAP_SUN.put("Yesterday Night Duration", TimingClass.AgingSubClass.computeAgingIntoHumanReadableWords(sunsetPrior, sunrise));
        final ZonedDateTime tomorrowZ = ZonedDateTime.now(internalZoneId).plusDays(1);
        final ZonedDateTime sunriseNext = calculateSunSetOrRise(tomorrowZ, true);
        MAP_SUN.put("Tomorrow Sunrise", sunriseNext.format(formatter));
        final ZonedDateTime sunsetNext = calculateSunSetOrRise(tomorrowZ, false);
        MAP_SUN.put("Tomorrow Sunset", sunsetNext.format(formatter));
        MAP_SUN.put("Tomorrow Light duration", TimingClass.AgingSubClass.computeAgingIntoHumanReadableWords(sunriseNext, sunsetNext));
        MAP_SUN.put("Today Night duration", TimingClass.AgingSubClass.computeAgingIntoHumanReadableWords(sunset, sunriseNext));
        String strSunSituation = "DOWN";
        String strCrtSituation = "After sunset";
        if (nowZ.isBefore(sunrise)) {
            strCrtSituation = "Before sunrise";
            MAP_SUN.put(PRIOR_EVENT, String.format("Sunset since %s", TimingClass.AgingSubClass.computeAgingIntoHumanReadableWords(sunsetPrior, nowZ)));
            MAP_SUN.put(NEXT_EVENT, String.format("Sunrise in %s", TimingClass.AgingSubClass.computeAgingIntoHumanReadableWords(nowZ, sunrise)));
        } else if (nowZ.isBefore(sunset)) {
            strSunSituation = "UP";
            strCrtSituation = "In between sunrise and sunset";
            MAP_SUN.put(PRIOR_EVENT, String.format("Sunrise since %s", TimingClass.AgingSubClass.computeAgingIntoHumanReadableWords(sunrise, nowZ)));
            MAP_SUN.put(NEXT_EVENT, String.format("Sunset in %s", TimingClass.AgingSubClass.computeAgingIntoHumanReadableWords(nowZ, sunset)));
        } else {
            MAP_SUN.put(PRIOR_EVENT, String.format("Sunset since %s", TimingClass.AgingSubClass.computeAgingIntoHumanReadableWords(sunset, nowZ)));
            MAP_SUN.put(NEXT_EVENT, String.format("Sunrise in %s", TimingClass.AgingSubClass.computeAgingIntoHumanReadableWords(nowZ, sunriseNext)));
        }
        MAP_SUN.put("Sun situation", strSunSituation);
        MAP_SUN.put("Current Situation", strCrtSituation);
    }

    /**
     * Setter for dblLatitude
     * @param inLatitude input Latitude
     */
    public static void setLatitude(final double inLatitude) {
        dblLatitude = inLatitude;
        MAP_SUN.put("Latitude", dblLatitude);
    }

    /**
     * Setter for dblLongitude
     * @param inLongitude input Longitude
     */
    public static void setLongitude(final double inLongitude) {
        dblLongitude = inLongitude;
        MAP_SUN.put("Longitude", dblLongitude);
    }

    /**
     * Setter for strZoneName
     * @param inZoneName input Zone Name
     */
    public static void setZoneId(final String inZoneName) {
        try {
            MAP_SUN.put("Zone Name", inZoneName);
            // Pre-cache available IDs for high-performance lookup
            final ZoneId zoneId = ZoneId.of(inZoneName);
            final String strFeedback = String.format("Given zone name %s has the corresponding ZoneId %s", inZoneName, zoneId);
            LogExposureClass.LOGGER.debug(strFeedback);
            internalZoneId = zoneId;
        } catch (DateTimeException e) {
            final String strFeedback = String.format("Given zone name %s does not seem to be a valid one... %s", inZoneName, e.getMessage());
            LogExposureClass.LOGGER.debug(strFeedback);
        }
    }

    private SunClass() {
        // intentionally blank
    }

}
