package com.example.travelitineraryplanner.data.local;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "itinerary_items",
    foreignKeys = @ForeignKey(
        entity = TripRequest.class,
        parentColumns = "id",
        childColumns = "tripRequestId",
        onDelete = ForeignKey.CASCADE
    )
)
public class ItineraryItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int tripRequestId;
    public int day;
    public String name;
    public String category;
    public double estimatedCost;
    public String time;
    public String thumbnailUrl;
    public String description;
    public String address;
    public double latitude;
    public double longitude;
    
    public ItineraryItem() {}
    
    @Ignore
    public ItineraryItem(int tripRequestId, int day, String name, String category, 
                        double estimatedCost, String time, String thumbnailUrl) {
        this.tripRequestId = tripRequestId;
        this.day = day;
        this.name = name;
        this.category = category;
        this.estimatedCost = estimatedCost;
        this.time = time;
        this.thumbnailUrl = thumbnailUrl;
    }
}
