package com.example.travelitineraryplanner.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;

import com.example.travelitineraryplanner.data.local.entities.ItineraryItem;
import com.example.travelitineraryplanner.data.local.entities.ItineraryRaw;

import java.util.List;

@Dao
public interface ItineraryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRaw(ItineraryRaw raw);

    @Query("SELECT jsonResponse FROM itinerary_raw WHERE requestId = :reqId LIMIT 1")
    String getRawByRequestId(long reqId);

    @Insert
    void insertItems(List<ItineraryItem> items);

    // Note: query uses requestId (matches the entity) and orders by dayNumber and orderInDay
    @Query("SELECT * FROM itinerary_items WHERE requestId = :reqId ORDER BY dayNumber, orderInDay")
    LiveData<List<ItineraryItem>> getItemsForRequest(long reqId);

    @Query("DELETE FROM itinerary_items WHERE requestId = :reqId")
    void deleteItemsForRequest(long reqId);
}
