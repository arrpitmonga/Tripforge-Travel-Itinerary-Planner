package com.example.travelitineraryplanner.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "itinerary_raw")
public class ItineraryRaw {
    @PrimaryKey
    public long requestId;

    public String jsonResponse;
    public long createdAt;

    public ItineraryRaw(long requestId, String jsonResponse) {
        this.requestId = requestId;
        this.jsonResponse = jsonResponse;
        this.createdAt = System.currentTimeMillis();
    }
}
