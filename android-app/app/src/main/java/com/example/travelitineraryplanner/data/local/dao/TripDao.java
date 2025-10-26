package com.example.travelitineraryplanner.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.travelitineraryplanner.data.local.entities.TripRequest;

import java.util.List;

@Dao
public interface TripDao {

    @Insert
    long insertRequest(TripRequest request);

    @Update
    void updateRequest(TripRequest request);

    @Delete
    void deleteRequest(TripRequest request);

    // Observe all requests (most useful for UI)
    @Query("SELECT * FROM trip_requests ORDER BY createdAt DESC")
    LiveData<List<TripRequest>> getAllRequests();

    // Synchronous fetch (single)
    @Query("SELECT * FROM trip_requests WHERE id = :id LIMIT 1")
    TripRequest getRequestById(long id);
}
