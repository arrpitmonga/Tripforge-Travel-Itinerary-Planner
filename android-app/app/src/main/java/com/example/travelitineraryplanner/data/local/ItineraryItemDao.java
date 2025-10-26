package com.example.travelitineraryplanner.data.local;

import androidx.room.*;
import androidx.lifecycle.LiveData;
import java.util.List;

@Dao
public interface ItineraryItemDao {
    @Query("SELECT * FROM itinerary_items WHERE tripRequestId = :tripRequestId ORDER BY day, time")
    LiveData<List<ItineraryItem>> getItineraryItemsByTrip(int tripRequestId);
    
    @Query("SELECT * FROM itinerary_items WHERE tripRequestId = :tripRequestId ORDER BY day, time")
    List<ItineraryItem> getItineraryItemsByTripSync(int tripRequestId);
    
    @Query("SELECT * FROM itinerary_items WHERE tripRequestId = :tripRequestId AND day = :day ORDER BY time")
    LiveData<List<ItineraryItem>> getItineraryItemsByTripAndDay(int tripRequestId, int day);
    
    @Insert
    void insertItineraryItem(ItineraryItem item);
    
    @Insert
    void insertItineraryItems(List<ItineraryItem> items);
    
    @Update
    void updateItineraryItem(ItineraryItem item);
    
    @Delete
    void deleteItineraryItem(ItineraryItem item);
    
    @Query("DELETE FROM itinerary_items WHERE tripRequestId = :tripRequestId")
    void deleteItineraryItemsForTrip(int tripRequestId);
    
    @Query("DELETE FROM itinerary_items WHERE tripRequestId IN (SELECT id FROM trip_requests WHERE userId = :userId)")
    void deleteAllItineraryItemsForUser(String userId);
}

