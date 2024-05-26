package com.example.navmap;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

public class WhuRoute {
    public String    m_strRoute;
    private Polyline routeGeometry;
     //路径结果解析
    WhuRoute(String strRoute)
    {
        try {
            m_strRoute = strRoute;
            JSONArray featuresArray = new JSONObject(strRoute).getJSONArray("features");

            //create the polyline from the point collection
            routeGeometry = new Polyline();
            routeGeometry.getOutlinePaint().setColor(Color.GREEN);
             for(int i=0; i<featuresArray.length(); i++) 
             {
                 JSONObject jfeature = featuresArray.getJSONObject(i);
                  if(jfeature.has("geometry"))
                  {
                      JSONObject geometryObject = jfeature.getJSONObject("geometry");
                      JSONArray pathsArray = geometryObject.getJSONArray("coordinates");
                      for(int j=0; j<pathsArray.length(); j++)
                      {
                          JSONArray pointArray = pathsArray.getJSONArray(j);
                          //投影转换
                          GeoPoint gpt = WebMecator.webMecatorxy2bl(pointArray.getDouble(0),pointArray.getDouble(1));
                          routeGeometry.addPoint(gpt);
                      }

                  }
             }

           // create and add points to the point collection

        }
        catch (JSONException e) {
            e.printStackTrace();
            m_strRoute = e.getMessage();
        }

    }

    Polyline getRouteGeometry()
    {
       return routeGeometry;
    }

}
