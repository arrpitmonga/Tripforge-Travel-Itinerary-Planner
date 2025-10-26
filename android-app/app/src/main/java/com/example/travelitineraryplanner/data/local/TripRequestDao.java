package com.example.travelitineraryplanner.data.local;

import androidx.room.*;
import androidx.lifecycle.LiveData;
import java.util.List;

@Dao
public interface TripRequestDao {
    @Query("SELECT * FROM trip_requests WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<TripRequest>> getTripsByUser(String userId);
    
    @Query("SELECT * FROM trip_requests WHERE id = :id")
    LiveData<TripRequest> getTripById(int id);
    
    @Query("SELECT * FROM trip_requests WHERE firestoreId = :firestoreId")
    TripRequest getTripByFirestoreId(String firestoreId);
    
    @Insert
    long insertTrip(TripRequest trip);
    
    @Update
    void updateTrip(TripRequest trip);
    
    @Delete
    void deleteTrip(TripRequest trip);
    
    @Query("DELETE FROM trip_requests WHERE userId = :userId")
    void deleteAllTripsForUser(String userId);
}

