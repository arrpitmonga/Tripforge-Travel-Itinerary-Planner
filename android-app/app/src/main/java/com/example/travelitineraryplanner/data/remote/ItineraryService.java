package com.example.travelitineraryplanner.data.remote;

import com.example.travelitineraryplanner.data.local.entities.TripRequest;

public interface ItineraryService {
    /**
     * Synchronously generate itinerary JSON for the given request.
     * Can throw an Exception if generation fails.
     */
    String generateItinerary(TripRequest request) throws Exception;
}
