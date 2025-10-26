package com.example.travelitineraryplanner.data.remote;

import android.content.Context;

import com.example.travelitineraryplanner.data.local.entities.TripRequest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Simple fake service that returns the contents of assets/sample_itinerary.json
 * as the generated itinerary JSON.
 */
public class FakeItineraryService implements ItineraryService {
    private final Context context;

    public FakeItineraryService(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String generateItinerary(TripRequest request) throws Exception {
        InputStream is = context.getAssets().open("sample_itinerary.json");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        br.close();
        return sb.toString();
    }
}
