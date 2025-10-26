package com.example.travelitineraryplanner.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "trip_requests")
public class TripRequest {
    @PrimaryKey(autoGenerate = true)
    public long id = 0;

    public String destination;
    public int durationDays;
    public String peopleType; // "solo", "couple", "family"
    public String budgetLevel; // "low", "moderate", "high"
    public long createdAt;
    public String status; // "pending", "completed", "failed"

    public TripRequest(String destination, int durationDays, String peopleType, String budgetLevel) {
        this.destination = destination;
        this.durationDays = durationDays;
        this.peopleType = peopleType;
        this.budgetLevel = budgetLevel;
        this.createdAt = System.currentTimeMillis();
        this.status = "pending";
    }
}
