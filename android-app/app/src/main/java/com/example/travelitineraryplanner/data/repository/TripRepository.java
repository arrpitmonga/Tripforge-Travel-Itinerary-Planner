package com.example.travelitineraryplanner.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.travelitineraryplanner.data.local.AppDatabase;
import com.example.travelitineraryplanner.data.local.ItineraryItemDao;
import com.example.travelitineraryplanner.data.local.TripRequestDao;
import com.example.travelitineraryplanner.data.local.TripRequest;
import com.example.travelitineraryplanner.data.local.ItineraryItem;

import com.example.travelitineraryplanner.ml.ItineraryPlanner;
import com.example.travelitineraryplanner.ml.ItineraryResult;
import com.example.travelitineraryplanner.ml.Poi;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TripRepository - glue between Room DB, Firestore and the ML itinerary planner.
 * Replace the old TripRepository with this file. Matches DAO and entity signatures
 * found in your project.
 */
public class TripRepository {
    private final TripRequestDao tripRequestDao;
    private final ItineraryItemDao itineraryItemDao;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final ItineraryPlanner itineraryPlanner;
    private final ExecutorService executor;
    private final Application application;

    public TripRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        this.tripRequestDao = db.tripRequestDao();
        this.itineraryItemDao = db.itineraryItemDao();
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.itineraryPlanner = new ItineraryPlanner(application);
        this.executor = Executors.newFixedThreadPool(3);
    }
    
    public void deleteTrip(TripRequest trip) {
        executor.execute(() -> {
            // Delete from local database
            tripRequestDao.deleteTrip(trip);
            
            // Delete associated itinerary items
            itineraryItemDao.deleteItineraryItemsForTrip(trip.id);
            
            // Delete from Firestore if user is authenticated
            String userId = getCurrentUserId();
            if (userId != null && !userId.equals("guest_user")) {
                firestore.collection("users").document(userId)
                    .collection("trips").document(String.valueOf(trip.id))
                    .delete();
            }
        });
    }
    
    public interface ItineraryItemsCallback {
        void onItemsLoaded(List<ItineraryItem> items);
    }
    
    public void getItineraryItemsByTripId(String tripId, ItineraryItemsCallback callback) {
        executor.execute(() -> {
            try {
                int tripIdInt = Integer.parseInt(tripId);
                List<ItineraryItem> items = itineraryItemDao.getItineraryItemsByTripSync(tripIdInt);
                callback.onItemsLoaded(items);
            } catch (NumberFormatException e) {
                // Handle error - return empty list
                callback.onItemsLoaded(new ArrayList<>());
            }
        });
    }

    // -------------------------
    // Read APIs for ViewModels
    // -------------------------
    public LiveData<List<TripRequest>> getUserTrips() {
        String userId = getCurrentUserId();
        System.out.println("TripRepository: getUserTrips called, userId: " + userId);
        if (userId == null) {
            System.out.println("TripRepository: No user ID, returning empty list");
            // return empty live data so UI won't NPE
            return new MutableLiveData<>(new ArrayList<>());
        }
        System.out.println("TripRepository: Getting trips for user: " + userId);
        return tripRequestDao.getTripsByUser(userId);
    }

    public LiveData<TripRequest> getTripById(int tripId) {
        return tripRequestDao.getTripById(tripId);
    }

    public LiveData<List<ItineraryItem>> getItineraryItems(int tripId) {
        return itineraryItemDao.getItineraryItemsByTrip(tripId);
    }

    // Synchronous methods for immediate ID retrieval
    public long insertTripSync(TripRequest trip) {
        return tripRequestDao.insertTrip(trip);
    }

    public void insertItineraryItemsSync(List<ItineraryItem> items) {
        for (ItineraryItem item : items) {
            itineraryItemDao.insertItineraryItem(item);
        }
    }

    // -------------------------
    // Create trip: save request -> generate itinerary -> save items
    // resultLiveData will receive inserted tripId (long) or -1 on failure
    // -------------------------
    public void createTrip(String destination, int duration, String budget, MutableLiveData<Long> resultLiveData) {
        executor.execute(() -> {
            try {
                String userId = getCurrentUserId();
                // userId will never be null now due to our fix in getCurrentUserId()
                
                // Validate inputs to prevent errors
                String finalDestination = destination;
                if (finalDestination == null || finalDestination.trim().isEmpty()) {
                    finalDestination = "Unknown Destination";
                }
                
                String finalBudget = budget;
                if (finalBudget == null || finalBudget.trim().isEmpty()) {
                    finalBudget = "MODERATE";
                }
                
                int finalDuration = duration;
                if (finalDuration <= 0) {
                    finalDuration = 1; // Ensure at least 1 day
                }

                // Create TripRequest (uses constructor in your TripRequest)
                TripRequest trip = new TripRequest(userId, finalDestination, finalDuration, finalBudget);
                long insertedId = tripRequestDao.insertTrip(trip); // DAO method exists
                if (insertedId <= 0) {
                    resultLiveData.postValue(-1L);
                    return;
                }
                // Some TripRequest classes store id as int; set if present
                try { trip.id = (int) insertedId; } catch (Exception ignored) {}

                // Generate itinerary via ML planner
                ItineraryResult itineraryResult = itineraryPlanner.planItinerary(finalDestination, finalDuration, finalBudget);

                // Defensive: If ML returns nothing, still create an empty day structure
                List<ItineraryItem> itemsToSave = new ArrayList<>();
                if (itineraryResult != null && itineraryResult.dayPlans != null && !itineraryResult.dayPlans.isEmpty()) {
                    for (Map.Entry<Integer, List<Poi>> entry : itineraryResult.dayPlans.entrySet()) {
                        int day = entry.getKey();
                        List<Poi> pois = entry.getValue();
                        int order = 0;
                        for (Poi poi : pois) {
                            order++;
                            // Use the constructor your ItineraryItem class has for required fields,
                            // then set optional fields by assignment for compatibility.
                            ItineraryItem it = new ItineraryItem(
                                    (int) insertedId,
                                    day,
                                    poi.name == null ? "" : poi.name,
                                    poi.category == null ? "" : poi.category,
                                    poi.estimatedCost,
                                    String.valueOf(poi.timeHours),
                                    poi.thumbnailUrl == null ? "" : poi.thumbnailUrl
                            );
                            // set optional extras if your entity has them (fields exist in your ItineraryItem)
                            try { it.description = poi.description; } catch (Exception ignored) {}
                            try { it.address = poi.address; } catch (Exception ignored) {}
                            try { it.latitude = poi.latitude; it.longitude = poi.longitude; } catch (Exception ignored) {}

                            itemsToSave.add(it);
                        }
                    }
                } else {
                    // No ML result: fallback - create empty day entries so UI has something
                    // We'll create one placeholder item per day (so users see a card to edit later)
                    for (int d = 1; d <= finalDuration; d++) {
                        ItineraryItem it = new ItineraryItem(
                                (int) insertedId,
                                d,
                                "No recommended POIs",
                                "info",
                                0.0,
                                "1.0",
                                ""
                        );
                        it.description = "No suggestions available for the selected location. Try a nearby city or broader state name.";
                        itemsToSave.add(it);
                    }
                }

                // Save items to Room (uses DAO insertItineraryItems)
                if (!itemsToSave.isEmpty()) {
                    itineraryItemDao.insertItineraryItems(itemsToSave);
                }

                // Try to persist to Firestore (non-blocking for success)
                try {
                    saveTripToFirestore(trip, itemsToSave);
                } catch (Exception e) {
                    // do not fail on Firestore issues
                    e.printStackTrace();
                }

                // Return inserted id
                resultLiveData.postValue(insertedId);
            } catch (Exception ex) {
                ex.printStackTrace();
                resultLiveData.postValue(-1L);
            }
        });
    }

    // -------------------------
    // Save a trip + items to Firestore for cross-device sync (optional)
    // -------------------------
    private void saveTripToFirestore(TripRequest trip, List<ItineraryItem> items) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> tripData = new HashMap<>();
        tripData.put("destination", trip.destination);
        tripData.put("duration", trip.duration);
        tripData.put("budget", trip.budget);
        tripData.put("createdAt", trip.createdAt == null ? new Date() : trip.createdAt);

        List<Map<String, Object>> list = new ArrayList<>();
        for (ItineraryItem it : items) {
            Map<String, Object> m = new HashMap<>();
            // Use reflective safe gets for fields that may or may not exist
            m.put("day", tryGetField(it, "day", 1));
            m.put("title", tryGetField(it, "title", tryGetField(it, "name", "")));
            m.put("category", tryGetField(it, "category", ""));
            m.put("estimatedCost", tryGetField(it, "estimatedCost", 0.0));
            m.put("time", tryGetField(it, "time", "1.0"));
            m.put("description", tryGetField(it, "description", ""));
            m.put("address", tryGetField(it, "address", ""));
            m.put("latitude", tryGetField(it, "latitude", 0.0));
            m.put("longitude", tryGetField(it, "longitude", 0.0));
            list.add(m);
        }
        tripData.put("itinerary", list);

        firestore.collection("users").document(userId)
                .collection("trips")
                .add(tripData)
                .addOnSuccessListener(documentReference -> {
                    // update local entity's firestoreId in background
                    try {
                        trip.firestoreId = documentReference.getId();
                        executor.execute(() -> tripRequestDao.updateTrip(trip));
                    } catch (Exception ignored) {}
                })
                .addOnFailureListener(e -> {
                    // ignore Firestore failure (local was saved)
                    e.printStackTrace();
                });
    }

    // -------------------------
    // Firestore -> local sync helper (optional)
    // -------------------------
    public void loadTripsFromFirestore() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        firestore.collection("users").document(userId)
                .collection("trips")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    executor.execute(() -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Map<String, Object> data = document.getData();
                                // transform and upsert to local DB if needed
                                // left intentionally minimal — expand if you want merge behavior
                            } catch (Exception ignored) {}
                        }
                    });
                });
    }

    // -------------------------
    // Utility helpers
    // -------------------------
    private String getCurrentUserId() {
        try {
            if (auth == null) {
                System.out.println("TripRepository: Auth is null");
                return "guest_user"; // Fallback to guest user instead of null
            }
            if (auth.getCurrentUser() == null) {
                System.out.println("TripRepository: No current user found");
                return "guest_user"; // Fallback to guest user instead of null
            }
            String userId = auth.getCurrentUser().getUid();
            System.out.println("TripRepository: Current user ID: " + userId);
            return userId;
        } catch (Exception e) {
            System.out.println("TripRepository: Error getting user ID: " + e.getMessage());
            return "guest_user"; // Fallback to guest user instead of null
        }
    }

    private Object tryGetField(Object obj, String fieldName, Object fallback) {
        if (obj == null) return fallback;
        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            Object val = f.get(obj);
            return val == null ? fallback : val;
        } catch (Exception ignored) {
            // try getter
            try {
                java.lang.reflect.Method m = obj.getClass().getMethod("get" + capitalize(fieldName));
                Object val = m.invoke(obj);
                return val == null ? fallback : val;
            } catch (Exception e) {
                return fallback;
            }
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // Cleanup on ViewModel clear
    public void cleanup() {
        try {
            if (executor != null && !executor.isShutdown()) executor.shutdown();
        } catch (Exception ignored) {}
    }
}
