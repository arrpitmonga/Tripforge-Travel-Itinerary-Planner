# Travel Itinerary Planner

An Android app that uses AI-powered recommendations to create personalized travel itineraries. Built with Java, Firebase, and TensorFlow Lite.

## Features

- **User Authentication**: Firebase Auth for secure login and account creation
- **AI-Powered Itinerary Generation**: TensorFlow Lite model for intelligent POI recommendations
- **Offline-First Architecture**: Room database for local caching and offline access
- **Cloud Sync**: Firestore integration for cross-device synchronization
- **Modern UI**: Material Design 3 with clean, professional interface

## Architecture

- **Activities**: Java-based activities for all screens
- **Repository Pattern**: Centralized data management with Firebase + Room
- **MVVM**: ViewModels for reactive UI updates
- **ML Integration**: TensorFlow Lite for itinerary planning
- **Offline Support**: Local Room database for cached data

## Setup Instructions

### 1. Firebase Configuration

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select existing one
3. Add Android app with package name: `com.example.travelitineraryplanner`
4. Download `google-services.json` and place it in `app/` directory
5. Enable Authentication (Email/Password) in Firebase Console
6. Enable Firestore Database in Firebase Console

### 2. Firebase Console Setup

#### Authentication Setup:
1. Go to Authentication > Sign-in method
2. Enable "Email/Password" provider
3. Save changes

#### Firestore Setup:
1. Go to Firestore Database
2. Create database in production mode
3. Set up security rules (see below)

#### Firestore Security Rules:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/trips/{tripId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### 3. Build and Run

1. Open project in Android Studio
2. Sync project with Gradle files
3. Build and run on device or emulator (API 26+)

### 4. Sample Test Trip

1. Launch the app
2. Create account or login
3. Tap "Create New Trip"
4. Enter destination: "Paris"
5. Enter duration: "3"
6. Select budget: "Moderate"
7. Tap "Generate Itinerary"
8. View your AI-generated itinerary!

## Project Structure

```
app/src/main/java/com/example/travelitineraryplanner/
├── ui/                          # Activities and UI components
│   ├── WelcomeActivity.java     # Launcher screen
│   ├── LoginActivity.java       # Authentication
│   ├── HomeActivity.java        # Main dashboard
│   ├── CreateTripActivity.java  # Trip creation form
│   ├── OldTripsActivity.java    # Saved trips list
│   ├── ItineraryActivity.java   # Itinerary display
│   ├── adapter/                 # RecyclerView adapters
│   └── viewmodel/               # ViewModels
├── data/                        # Data layer
│   ├── local/                   # Room database
│   └── repository/              # Repository pattern
└── ml/                          # Machine Learning
    ├── ItineraryPlanner.java    # TensorFlow Lite integration
    └── Poi.java                 # POI data model
```

## Dependencies

- **Firebase**: Authentication and Firestore
- **Room**: Local database
- **TensorFlow Lite**: ML model inference
- **Glide**: Image loading
- **Material Design**: UI components
- **AndroidX**: Lifecycle, ViewModel, LiveData

## ML Model Integration

The app includes a comprehensive TensorFlow Lite model (`itinerary_model_consistent_final_quant_dynamic.tflite`) and supporting files:
- `encoders.json`: Location, budget, cost category, and POI category encoders
- `scaler.json`: Feature scaling parameters for numerical features
- `pois_for_app.csv`: Complete POI database with 200+ attractions across India

### Model Input Schema (Feature Order)

The TensorFlow Lite model expects features in this exact order:

```java
// Feature vector (8 features total):
float[] features = new float[8];

// [0] estimated_visit_cost_inr (scaled)
features[0] = (estimatedCost - mean) / scale;

// [1] time_hours (scaled) 
features[1] = (timeHours - mean) / scale;

// [2] dist_km_to_city_center (scaled)
features[2] = (distance - mean) / scale;

// [3] le_city (encoded) - city name as integer
features[3] = locationEncoder.get(cityName);

// [4] le_budget (encoded) - budget level as integer
features[4] = budgetEncoder.get(budgetLevel);

// [5] le_costcat (encoded) - cost category as integer
features[5] = costCategoryEncoder.get(costCategory);

// [6] le_cat (encoded) - POI category as integer
features[6] = categoryEncoder.get(poiCategory);

// [7] popularity_score (normalized)
features[7] = popularityScore;
```

### Asset Files

- **`itinerary_model_consistent_final_quant_dynamic.tflite`**: Main ML model for POI ranking
- **`encoders.json`**: Label encoders for categorical features
- **`scaler.json`**: StandardScaler parameters for numerical features  
- **`pois_for_app.csv`**: POI database with columns: city, state, attraction_name, category, latitude, longitude, estimated_visit_cost_inr, time_hours, cost_category, popularity_score

### Replacing Assets Safely

To replace model assets:

1. **Model**: Replace `.tflite` file with same name
2. **Encoders**: Update `encoders.json` with new categorical mappings
3. **Scaler**: Update `scaler.json` with new scaling parameters
4. **POI Data**: Update `pois_for_app.csv` with new POI database

**Important**: Ensure feature schema matches exactly - changing the order or number of features will break model inference.

### Algorithm Implementation

The itinerary generation follows this exact process:

1. **Scope Selection**: Find POIs in target city, expand to nearby cities if needed
2. **Pre-filtering**: Remove incompatible POIs based on budget and time constraints
3. **ML Scoring**: Use TensorFlow Lite model to rank POIs by relevance
4. **Proximity Boost**: Apply distance-based scoring boost (0.1 weight)
5. **Daily Packing**: Greedy algorithm to pack POIs into days respecting time/budget limits
6. **Region Expansion**: Expand search radius if insufficient POIs found

### Performance Requirements

- **Model Inference**: <1 second for ~300 POIs
- **Memory Usage**: Cached POI data and encoders in memory
- **Background Processing**: All ML operations run off main thread
- **Fallback**: Graceful degradation if model fails to load
- Outputs POI recommendation probabilities
- Is optimized for mobile inference

## Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## Troubleshooting

### Common Issues:

1. **Firebase connection failed**: Ensure `google-services.json` is in `app/` directory
2. **Authentication errors**: Check Firebase Auth is enabled in console
3. **Model loading errors**: App falls back to sample data if model fails
4. **Build errors**: Ensure all dependencies are synced in Android Studio

### Debug Mode:
- Check Logcat for detailed error messages
- Verify Firebase project configuration
- Test with sample data if ML model issues occur

## Contributing

1. Fork the repository
2. Create feature branch
3. Make changes
4. Add tests
5. Submit pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
- Check the troubleshooting section
- Review Firebase documentation
- Open an issue on GitHub



