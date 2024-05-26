package com.example.navmap;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GPRMCData {

    private Time utcTime;
    private String status;
    private double latitude;
    private double longitude;
    private double speedKnots;
    private double courseDegrees;
    private Date date;
    private double magneticVariation;
    private String magneticVariationDir;
    private String modeIndicator;

    public GPRMCData() {}

    public GPRMCData(Time utcTime, String status, double latitude, double longitude, double speedKnots,
                     double courseDegrees, Date date, double magneticVariation, String magneticVariationDir,
                     String modeIndicator) {
        this.utcTime = utcTime;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speedKnots = speedKnots;
        this.courseDegrees = courseDegrees;
        this.date = date;
        this.magneticVariation = magneticVariation;
        this.magneticVariationDir = magneticVariationDir;
        this.modeIndicator = modeIndicator;
    }

    public Time getUtcTime() {
        return utcTime;
    }

    public void setUtcTime(Time utcTime) {
        this.utcTime = utcTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getSpeedKnots() {
        return speedKnots;
    }

    public void setSpeedKnots(double speedKnots) {
        this.speedKnots = speedKnots;
    }

    public double getCourseDegrees() {
        return courseDegrees;
    }

    public void setCourseDegrees(double courseDegrees) {
        this.courseDegrees = courseDegrees;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getMagneticVariation() {
        return magneticVariation;
    }

    public void setMagneticVariation(double magneticVariation) {
        this.magneticVariation = magneticVariation;
    }

    public String getMagneticVariationDir() {
        return magneticVariationDir;
    }

    public void setMagneticVariationDir(String magneticVariationDir) {
        this.magneticVariationDir = magneticVariationDir;
    }

    public String getModeIndicator() {
        return modeIndicator;
    }

    public void setModeIndicator(String modeIndicator) {
        this.modeIndicator = modeIndicator;
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return "GPRMCData{" +
                "utcTime=" + (utcTime != null ? new SimpleDateFormat("HH:mm:ss").format(utcTime) : "null") +
                ", status='" + status + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", speedKnots=" + speedKnots +
                ", courseDegrees=" + courseDegrees +
                ", date=" + (date != null ? dateFormat.format(date) : "null") +
                ", magneticVariation=" + magneticVariation +
                ", magneticVariationDir='" + magneticVariationDir + '\'' +
                ", modeIndicator='" + modeIndicator + '\'' +
                '}';
    }
}
