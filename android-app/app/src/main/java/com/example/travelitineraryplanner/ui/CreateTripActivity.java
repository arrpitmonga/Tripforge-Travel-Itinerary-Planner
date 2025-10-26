package com.example.travelitineraryplanner.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.travelitineraryplanner.R;
import com.example.travelitineraryplanner.data.repository.TripRepository;
import com.example.travelitineraryplanner.ml.ItineraryResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CreateTripActivity - collects trip input, runs planner off the UI thread,
 * then navigates to ItineraryActivity with the generated ItineraryResult.
 *
 * Layout expected: activity_create_trip.xml (must contain the ids used below).
 */
public class CreateTripActivity extends AppCompatActivity {

    private EditText destinationEditText;
    private EditText durationEditText;
    private AutoCompleteTextView budgetSpinner;
    private Button generateItineraryButton;
    private ProgressBar progressBar;
    private TextView errorTextView;
    private ImageView backButton;

    private TripRepository tripRepository;
    private ExecutorService executor;

    private static final String[] BUDGET_OPTIONS = {"Budget", "Moderate", "Luxury"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_trip);

        // repository + executor (keep as in original; TripRepository should accept Application)
        tripRepository = new TripRepository(getApplication());
        executor = Executors.newFixedThreadPool(2);

        // Views
        destinationEditText = findViewById(R.id.destinationEditText);
        durationEditText = findViewById(R.id.durationEditText);
        budgetSpinner = findViewById(R.id.budgetSpinner);
        generateItineraryButton = findViewById(R.id.generateItineraryButton);
        progressBar = findViewById(R.id.progressBar);
        errorTextView = findViewById(R.id.errorTextView);
        backButton = findViewById(R.id.backButton);

        // Wire back button (layout has ImageView backButton)
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        setupBudgetSpinner();

        // generate
        generateItineraryButton.setOnClickListener(v -> generateItinerary());
    }

    private void setupBudgetSpinner() {
        // attach adapter so the dropdown has values
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                BUDGET_OPTIONS
        );
        budgetSpinner.setAdapter(adapter);

        // default: Moderate
        budgetSpinner.setText(BUDGET_OPTIONS[1], false);

        // show dropdown when clicked or focused
        budgetSpinner.setOnClickListener(v -> budgetSpinner.showDropDown());
        budgetSpinner.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) budgetSpinner.showDropDown();
        });
    }

    private void generateItinerary() {
        String destination = destinationEditText.getText().toString().trim();
        String durationText = durationEditText.getText().toString().trim();
        String budget = budgetSpinner.getText().toString().trim();

        // simple logging for debug
        System.out.println("CreateTripActivity.generateItinerary -> destination=" + destination
                + " duration=" + durationText + " budget=" + budget);

        if (TextUtils.isEmpty(destination)) {
            showError("Please enter destination");
            return;
        }

        if (TextUtils.isEmpty(durationText)) {
            showError("Please enter duration");
            return;
        }

        final int duration;
        try {
            duration = Integer.parseInt(durationText);
            if (duration < 1 || duration > 365) {
                showError("Duration must be between 1 and 365 days");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid number for duration");
            return;
        }

        if (TextUtils.isEmpty(budget)) {
            showError("Please select budget");
            return;
        }

        setLoading(true);
        hideError();

        // Run planner on background thread to avoid blocking UI
        executor.execute(() -> {
            try {
                // create planner instance (original code used new ItineraryPlanner(this))
                com.example.travelitineraryplanner.ml.ItineraryPlanner planner =
                        new com.example.travelitineraryplanner.ml.ItineraryPlanner(CreateTripActivity.this);

                // run planning
                ItineraryResult result = planner.planItinerary(destination, duration, budget);

                // ensure planner resources are released if provided
                try {
                    planner.shutdown();
                } catch (Exception ignore) {
                }

                // make final for use on UI thread
                final ItineraryResult finalResult = result;

                runOnUiThread(() -> {
                    setLoading(false);
                    if (finalResult != null && finalResult.dayPlans != null && !finalResult.dayPlans.isEmpty()) {
                        Toast.makeText(CreateTripActivity.this, "Itinerary generated successfully!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(CreateTripActivity.this, ItineraryActivity.class);
                        // pass result — must be Serializable/Parcelable in your model
                        intent.putExtra("itinerary_result", finalResult);
                        startActivity(intent);
                        finish();
                    } else {
                        showError("Failed to generate itinerary. Please try again.");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    setLoading(false);
                    showError("Failed to generate itinerary: " + e.getMessage());
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (generateItineraryButton != null) generateItineraryButton.setEnabled(!loading);
    }

    private void showError(String message) {
        if (errorTextView != null) {
            errorTextView.setText(message);
            errorTextView.setVisibility(View.VISIBLE);
        } else {
            // fallback
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void hideError() {
        if (errorTextView != null) {
            errorTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdownNow();
    }
}
