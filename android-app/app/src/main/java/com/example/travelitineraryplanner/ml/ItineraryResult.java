package com.example.travelitineraryplanner.ml;

import java.io.Serializable;
import java.util.*;

/**
 * Result object for itinerary generation containing structured data and metadata
 */
public class ItineraryResult implements Serializable {
    public Metadata metadata;
    public Map<Integer, List<Poi>> dayPlans;
    public List<Poi> pois;
    public Totals totals;
    public String summary;
    
    public ItineraryResult() {
        this.dayPlans = new HashMap<>();
        this.pois = new ArrayList<>();
    }
    
    public static class Metadata implements Serializable {
        public String location;
        public int days;
        public String budget;
        public long generatedAt;
        public List<String> sourceCities;
        
        public Metadata() {
            this.sourceCities = new ArrayList<>();
        }
    }
    
    public static class Totals implements Serializable {
        public double totalEstimatedCost;
        public double totalTimeHours;
        
        public Totals() {}
        
        public Totals(double totalEstimatedCost, double totalTimeHours) {
            this.totalEstimatedCost = totalEstimatedCost;
            this.totalTimeHours = totalTimeHours;
        }
    }
    
    /**
     * Generate a short textual summary for UI display
     */
    public String generateSummary() {
        int totalPois = dayPlans.values().stream()
                .mapToInt(List::size)
                .sum();
        
        return String.format("%d-day %s itinerary for %s — %d POIs, est ₹%.0f total.",
                metadata.days, metadata.budget, metadata.location, totalPois, totals.totalEstimatedCost);
    }
}
