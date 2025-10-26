package com.example.travelitineraryplanner.ui.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.travelitineraryplanner.data.local.TripRequest;
import com.example.travelitineraryplanner.data.repository.TripRepository;
import java.util.List;

public class TripViewModel extends AndroidViewModel {
    private TripRepository tripRepository;
    private LiveData<List<TripRequest>> userTrips;

    public TripViewModel(Application application) {
        super(application);
        tripRepository = new TripRepository(application);
        System.out.println("TripViewModel: Initializing and getting user trips");
        userTrips = tripRepository.getUserTrips();
    }

    public LiveData<List<TripRequest>> getUserTrips() {
        return userTrips;
    }
    
    public void deleteTrip(TripRequest trip) {
        new Thread(() -> {
            tripRepository.deleteTrip(trip);
        }).start();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        tripRepository.cleanup();
    }
}



