package com.example.travelitineraryplanner.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.travelitineraryplanner.R;
import com.example.travelitineraryplanner.data.local.TripRequest;
import com.example.travelitineraryplanner.ui.adapter.TripAdapter;
import com.example.travelitineraryplanner.ui.viewmodel.TripViewModel;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class OldTripsActivity extends AppCompatActivity {

    private RecyclerView tripsRecyclerView;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private TripAdapter tripAdapter;
    private TripViewModel tripViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_old_trips);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tripsRecyclerView = findViewById(R.id.tripsRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);

        setupRecyclerView();
        setupViewModel();
    }

    private void setupRecyclerView() {
        tripAdapter = new TripAdapter(new ArrayList<>());
        
        // Use anonymous classes instead of implementing interfaces
        tripAdapter.setOnTripClickListener(trip -> {
            Intent intent = new Intent(this, ItineraryActivity.class);
            intent.putExtra("trip_id", trip.id);
            intent.putExtra("destination", trip.destination);
            intent.putExtra("duration", trip.duration);
            intent.putExtra("budget", trip.budget);
            intent.putExtra("view_saved_trip", true);
            startActivity(intent);
        });
        
        tripAdapter.setOnTripEditListener(trip -> {
            Intent intent = new Intent(this, ItineraryActivity.class);
            intent.putExtra("trip_id", trip.id);
            intent.putExtra("destination", trip.destination);
            intent.putExtra("duration", trip.duration);
            intent.putExtra("budget", trip.budget);
            intent.putExtra("edit_mode", true);
            startActivity(intent);
        });
        
        tripAdapter.setOnTripDeleteListener(trip -> {
            new AlertDialog.Builder(this)
                .setTitle("Delete Trip")
                .setMessage("Are you sure you want to delete this trip to " + trip.destination + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    tripViewModel.deleteTrip(trip);
                    Toast.makeText(this, "Trip deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
        
        tripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tripsRecyclerView.setAdapter(tripAdapter);
    }

    private void setupViewModel() {
        tripViewModel = new ViewModelProvider(this).get(TripViewModel.class);
        
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        tripsRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        
        tripViewModel.getUserTrips().observe(this, new Observer<List<TripRequest>>() {
            @Override
            public void onChanged(List<TripRequest> trips) {
                System.out.println("OldTripsActivity: Received " + (trips != null ? trips.size() : 0) + " trips");
                progressBar.setVisibility(View.GONE);
                
                if (trips != null && !trips.isEmpty()) {
                    System.out.println("OldTripsActivity: Showing trips list");
                    for (TripRequest trip : trips) {
                        System.out.println("Trip: " + trip.destination + " (" + trip.duration + " days, " + trip.budget + ")");
                    }
                    tripAdapter.setTrips(trips);
                    tripsRecyclerView.setVisibility(View.VISIBLE);
                    emptyStateLayout.setVisibility(View.GONE);
                } else {
                    System.out.println("OldTripsActivity: Showing empty state - no trips found");
                    tripsRecyclerView.setVisibility(View.GONE);
                    emptyStateLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }



    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
