package com.example.travelitineraryplanner.ml;

import java.io.Serializable;

// Simple POI model used by the planner
public class Poi implements Serializable {
    public String id = "";
    public String name = "";
    public String category = "";
    public String description = "";
    public String city = "";
    public String state = "";
    public double latitude = 0.0;
    public double longitude = 0.0;
    public double estimatedCost = 0.0;
    public double timeHours = 1.0;
    public double popularityScore = 0.5;
    public String costCategory = "medium";
    public String thumbnailUrl = "";
    public String address = "";
    public int day = 1;
    public String time = "";

    // Scoring fields (computed)
    public double proximityScore = 0.0;
    public double proximityBoost = 1.0;
    public double budgetMatchScore = 0.5;
    public double modelScore = 0.0;
    public double finalScore = 0.0;

    // Haversine distance (km) from this POI to (lat, lon)
    public double distanceTo(double lat, double lon) {
        final double R = 6371.0; // earth radius km
        double dLat = Math.toRadians(lat - this.latitude);
        double dLon = Math.toRadians(lon - this.longitude);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(lat)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}
