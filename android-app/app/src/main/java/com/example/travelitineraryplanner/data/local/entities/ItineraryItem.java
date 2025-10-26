package com.example.travelitineraryplanner.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

/**
 * Room entity for an itinerary item.
 * Important: index on tripRequestId to avoid Room warning.
 */
@Entity(tableName = "itinerary_items",
        foreignKeys = @ForeignKey(entity = com.example.travelitineraryplanner.data.local.entities.TripRequest.class,
                parentColumns = "id",
                childColumns = "tripRequestId",
                onDelete = CASCADE),
        indices = {@Index(value = {"tripRequestId"})}
)
public class ItineraryItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long tripRequestId;

    @ColumnInfo(name = "day")
    public int day;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "time")
    public String time; // keep string if DB expects string representation

    @ColumnInfo(name = "estimatedCost")
    public double estimatedCost;

    @ColumnInfo(name = "thumbnailUrl")
    public String thumbnailUrl;

    @ColumnInfo(name = "address")
    public String address;

    @ColumnInfo(name = "latitude")
    public double latitude;

    @ColumnInfo(name = "longitude")
    public double longitude;

    // default constructor
    public ItineraryItem() { }

    // convenience constructor
    public ItineraryItem(long tripRequestId, int day, String name, String category, String time, double estimatedCost,
                         String thumbnailUrl, String address, double latitude, double longitude) {
        this.tripRequestId = tripRequestId;
        this.day = day;
        this.name = name;
        this.category = category;
        this.time = time;
        this.estimatedCost = estimatedCost;
        this.thumbnailUrl = thumbnailUrl;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
