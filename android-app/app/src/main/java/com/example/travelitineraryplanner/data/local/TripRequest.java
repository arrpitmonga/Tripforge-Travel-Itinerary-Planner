package com.example.travelitineraryplanner.data.local;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "trip_requests")
public class TripRequest {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String userId;
    public String destination;
    public int duration;
    public String budget;
    public Date createdAt;
    public String firestoreId; // For syncing with Firestore
    
    public TripRequest() {}
    
    @Ignore
    public TripRequest(String userId, String destination, int duration, String budget) {
        this.userId = userId;
        this.destination = destination;
        this.duration = duration;
        this.budget = budget;
        this.createdAt = new Date();
    }
}
