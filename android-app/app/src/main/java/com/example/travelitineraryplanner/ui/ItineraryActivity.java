package com.example.travelitineraryplanner.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelitineraryplanner.data.repository.TripRepository;

import com.example.travelitineraryplanner.R;
import com.example.travelitineraryplanner.ml.ItineraryResult;
import com.example.travelitineraryplanner.ml.Poi;
import com.example.travelitineraryplanner.ui.adapter.ItineraryAdapter;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ItineraryActivity - displays a generated itinerary and allows saving/sharing.
 * Matches layout file: res/layout/activity_itinerary.xml
 */
public class ItineraryActivity extends AppCompatActivity implements ItineraryAdapter.OnPoiClickListener {

    private Toolbar toolbar;
    private TextView summaryText;
    private ProgressBar progressBar;
    private MaterialCardView tripSummaryCard;
    private TextView tripSummaryHeader;
    private TextView totalHoursText;
    private TextView totalCostText;
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private FloatingActionButton saveButton;
    private FloatingActionButton shareButton;

    private ItineraryResult itineraryResult;
    private ItineraryAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_itinerary); // matches the uploaded XML. :contentReference[oaicite:1]{index=1}

        bindViews();
        setupToolbar();
        setupRecycler();

        // Try load itinerary data from intent
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("itinerary_result")) {
                // New itinerary from generation
                Serializable raw = intent.getSerializableExtra("itinerary_result");
                if (raw instanceof ItineraryResult) {
                    itineraryResult = (ItineraryResult) raw;
                    displayItinerary();
                } else {
                    showError("Invalid itinerary data received");
                }
            } else if (intent.hasExtra("trip_id") && intent.hasExtra("view_saved_trip")) {
                // Load saved trip from database
                String tripId = intent.getStringExtra("trip_id");
                String destination = intent.getStringExtra("destination");
                int duration = intent.getIntExtra("duration", 0);
                String budget = intent.getStringExtra("budget");
                
                // Set toolbar title
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(destination);
                }
                
                // Show loading state
                progressBar.setVisibility(View.VISIBLE);
                emptyStateText.setVisibility(View.GONE);
                
                // Load trip items from repository
                TripRepository repository = new TripRepository(getApplication());
                repository.getItineraryItemsByTripId(tripId, items -> {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        
                        if (items != null && !items.isEmpty()) {
                            // Create a basic ItineraryResult to display
                            itineraryResult = new ItineraryResult();
                            // Initialize metadata if it's null
                            if (itineraryResult.metadata == null) {
                                itineraryResult.metadata = new ItineraryResult.Metadata();
                            }
                            itineraryResult.metadata.location = destination;
                            itineraryResult.metadata.days = duration;
                            itineraryResult.metadata.budget = budget;
                            
                            // Convert ItineraryItems to POIs
                            List<Poi> pois = new ArrayList<>();
                            for (int i = 0; i < items.size(); i++) {
                                Poi poi = new Poi();
                                poi.name = items.get(i).name;
                                poi.description = items.get(i).description;
                                poi.category = items.get(i).category;
                                poi.thumbnailUrl = items.get(i).thumbnailUrl;
                                poi.day = items.get(i).day;
                                pois.add(poi);
                            }
                            itineraryResult.pois = pois;
                            
                            displayItinerary();
                            
                            // Hide save button since it's already saved
                            saveButton.setVisibility(View.GONE);
                        } else {
                            showError("No itinerary items found for this trip");
                        }
                    });
                });
            } else {
                showError("No itinerary data to display");
            }
        } else {
            showError("No itinerary data to display");
        }

        saveButton.setOnClickListener(v -> saveItinerary());
        shareButton.setOnClickListener(v -> shareItinerary());
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        summaryText = findViewById(R.id.summary_text);
        progressBar = findViewById(R.id.progress_bar);
        tripSummaryCard = findViewById(R.id.trip_summary_card);
        tripSummaryHeader = findViewById(R.id.trip_summary_header);
        totalHoursText = findViewById(R.id.total_hours);
        totalCostText = findViewById(R.id.total_cost);
        recyclerView = findViewById(R.id.recycler_view);
        emptyStateText = findViewById(R.id.empty_state);
        saveButton = findViewById(R.id.save_button);
        shareButton = findViewById(R.id.share_button);

        // Initially hide action FABs until we have a result
        saveButton.setVisibility(View.GONE);
        shareButton.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // enable back arrow
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecycler() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Adapter will be set once we have itinerary data
    }

    private void displayItinerary() {
        if (itineraryResult == null || itineraryResult.dayPlans == null || itineraryResult.dayPlans.isEmpty()) {
            showError(getString(R.string.no_itinerary_items));
            return;
        }

        // Hide loading / error
        progressBar.setVisibility(View.GONE);
        summaryText.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        // Show trip summary card and totals
        tripSummaryCard.setVisibility(View.VISIBLE);
        tripSummaryHeader.setText(getString(R.string.trip_summary));
        if (itineraryResult.totals != null) {
            totalHoursText.setText(String.format("%.1f hours", itineraryResult.totals.totalTimeHours));
            totalCostText.setText(String.format("₹%.0f", itineraryResult.totals.totalEstimatedCost));
        } else {
            totalHoursText.setText("0.0 hours");
            totalCostText.setText("₹0");
        }

        // Create adapter and attach
        adapter = new ItineraryAdapter(itineraryResult.dayPlans, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setVisibility(View.VISIBLE);

        // Show action buttons (save/share)
        saveButton.setVisibility(View.VISIBLE);
        shareButton.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        tripSummaryCard.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        shareButton.setVisibility(View.GONE);

        summaryText.setText(message);
        summaryText.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
    }

    /**
     * Called when user taps Save.
     * Saves the itinerary to the database using TripRepository.
     */
    private void saveItinerary() {
        if (itineraryResult == null) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multiple saves
        saveButton.setEnabled(false);
        
        // Create a TripRepository instance
        TripRepository tripRepository = new TripRepository(getApplication());
        
        // Extract trip details from itineraryResult
        // Add null check for metadata
        if (itineraryResult.metadata == null) {
            itineraryResult.metadata = new ItineraryResult.Metadata();
        }
        String destination = itineraryResult.metadata.location != null ? itineraryResult.metadata.location : "Unknown";
        int duration = itineraryResult.dayPlans != null ? itineraryResult.dayPlans.size() : 1;
        String budget = itineraryResult.metadata.budget != null ? itineraryResult.metadata.budget : "MODERATE";
        
        // Create MutableLiveData to receive the result
        MutableLiveData<Long> resultLiveData = new MutableLiveData<>();
        resultLiveData.observe(this, tripId -> {
            if (tripId != null && tripId > 0) {
                Toast.makeText(this, "Trip saved successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save trip", Toast.LENGTH_SHORT).show();
            }
            // Re-enable the save button
            saveButton.setEnabled(true);
        });
        
        // Save the trip
        tripRepository.createTrip(destination, duration, budget, resultLiveData);
    }

    /**
     * Share simple text representation
     */
    private void shareItinerary() {
        if (itineraryResult == null) {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        // Add null check for metadata
        if (itineraryResult.metadata == null) {
            itineraryResult.metadata = new ItineraryResult.Metadata();
        }
        String location = itineraryResult.metadata.location != null ? itineraryResult.metadata.location : "Unknown";
        sb.append("Trip: ").append(itineraryResult.summary != null ? itineraryResult.summary : location).append("\n\n");
        for (Map.Entry<Integer, List<Poi>> entry : itineraryResult.dayPlans.entrySet()) {
            sb.append("Day ").append(entry.getKey()).append(":\n");
            for (Poi p : entry.getValue()) {
                sb.append("• ").append(p.name).append(" (").append(String.format("%.1fh", p.timeHours)).append(", ₹").append(String.format("%.0f", p.estimatedCost)).append(")\n");
            }
            sb.append("\n");
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, "My Trip Itinerary");
        share.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(share, "Share itinerary"));
    }

    @Override
    public void onPoiClick(Poi poi) {
        // Show POI details dialog (simple toast for now)
        if (poi != null) {
            String msg = poi.name + " — " + (poi.category != null ? poi.category : "");
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
