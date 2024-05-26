package com.example.navmap;

import java.sql.Time;
import java.util.Date;

public class GPRMCParser {

    private static final double KNOTS_TO_KILOMETERS_PER_HOUR = 1.852;
    private static final double KILOMETERS_TO_METERS = 1000;
    private static final double HOURS_TO_SECONDS = 3600.0;

    public static GPRMCData parse(String gprmcString) {
        String[] parts = gprmcString.split(",");

        if (!isValidGPRMCFormat(parts)) {
            System.out.println("Invalid GPRMC message format");
            return null;
        }

        try {
            Time utcTime = parseUTCTime(parts[1]);
            String status = parseStatus(parts[2]);
            double latitude = parseCoordinate(parts[3], parts[4]);
            double longitude = parseCoordinate(parts[5], parts[6]);
            double speedKnots = Double.parseDouble(parts[7]);
            double courseDegrees = Double.parseDouble(parts[8]);
            Date date = parseDate(parts[9]);
            double magneticVariation = Double.parseDouble(parts[10]);
            String magneticVariationDir = parseDir(parts[11]);
            String modeIndicator = parseModel(parts[12]);

            return new GPRMCData(utcTime, status, latitude, longitude, speedKnots, courseDegrees,
                    date, magneticVariation, magneticVariationDir, modeIndicator);
        } catch (Exception e) {
            System.out.println("Error parsing GPRMC message: " + e.getMessage());
            return null;
        }
    }

    private static boolean isValidGPRMCFormat(String[] parts) {
        return parts[0].equals("$GPRMC");
    }

    private static Time parseUTCTime(String utcTimeStr) throws Exception {
        int hours = Integer.parseInt(utcTimeStr.substring(0, 2));
        int minutes = Integer.parseInt(utcTimeStr.substring(2, 4));
        String seconds = utcTimeStr.substring(4);
        String[] split = seconds.split("\\.");
        String timeStr = hours + ":" + minutes + ":" + split[0] + "." + split[1];
        return Time.valueOf(timeStr);
    }

    private static String parseStatus(String status) {
        return status.equals("A") ? "Valid" : "Invalid";
    }

    private static double parseCoordinate(String coordStr, String dirStr) {
        double coord = Double.parseDouble(coordStr.substring(0, 2)) +
                Double.parseDouble(coordStr.substring(2)) / 60.0;
        return dirStr.equals("S") || dirStr.equals("W") ? -coord : coord;
    }

    private static Date parseDate(String dateStr) throws Exception {
        int year = 2000 + Integer.parseInt(dateStr.substring(4, 6));
        int month = Integer.parseInt(dateStr.substring(2, 4));
        int day = Integer.parseInt(dateStr.substring(0, 2));
        return new Date(year - 1900, month - 1, day);
    }

    public static double convertKnotsToMetersPerSecond(double speedInKnots) {
        if (speedInKnots == 0) {
            return speedInKnots;
        }
        double speedInKilometersPerHour = speedInKnots * KNOTS_TO_KILOMETERS_PER_HOUR * KILOMETERS_TO_METERS;
        return speedInKilometersPerHour / HOURS_TO_SECONDS;
    }

    public static String parseModel(String charStr) {
        switch (charStr) {
            case "A":
                return "Autonomous positioning";
            case "D":
                return "Differential";
            case "E":
                return "Estimate";
            default:
                return "Invalid data";
        }
    }

    private static String parseDir(String group) {
        return group.equals("E") ? "East" : "West";
    }
}
