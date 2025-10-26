package com.example.pp.chat.util;


public class Geo {
    private static final double R = 6371_000.0;

    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2){
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*
                        Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R*c;
    }

    public static double metersToKmRounded2(double meters){
        return Math.round((meters/1000.0)*100.0)/100.0;
    }
}
