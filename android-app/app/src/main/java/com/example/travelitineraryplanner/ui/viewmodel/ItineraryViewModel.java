package com.example.travelitineraryplanner.ui.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.travelitineraryplanner.data.local.ItineraryItem;
import com.example.travelitineraryplanner.data.repository.TripRepository;
import java.util.List;

public class ItineraryViewModel extends AndroidViewModel {
    private TripRepository tripRepository;

    public ItineraryViewModel(Application application) {
        super(application);
        tripRepository = new TripRepository(application);
    }

    public LiveData<List<ItineraryItem>> getItineraryItems(int tripId) {
        return tripRepository.getItineraryItems(tripId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        tripRepository.cleanup();
    }
}







