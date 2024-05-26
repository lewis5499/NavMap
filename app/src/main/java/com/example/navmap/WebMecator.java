package com.example.navmap;
import androidx.annotation.NonNull;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.constants.GeoConstants;
import org.osmdroid.views.util.constants.MathConstants;

public class WebMecator {
    @NonNull
    public static double[] webMecatorbl2xy(@NonNull GeoPoint gpt)
    {
        double[] xy = new double[2];
        xy[0] = gpt.getLongitude() * GeoConstants.RADIUS_EARTH_METERS * MathConstants.DEG2RAD;
        xy[1] = GeoConstants.RADIUS_EARTH_METERS* Math.log(Math.tan(Math.PI/4 + gpt.getLatitude()*MathConstants.DEG2RAD / 2));
        return xy;
    }
    @NonNull
    public static GeoPoint webMecatorxy2bl(double x, double y)
    {
        double lon = x / GeoConstants.RADIUS_EARTH_METERS * MathConstants.RAD2DEG;
        double U = y / GeoConstants.RADIUS_EARTH_METERS;
        double lat =  2 * (Math.atan(Math.exp(U))- Math.PI/4) * MathConstants.RAD2DEG;
        return new GeoPoint(lat,lon);
    }
}
