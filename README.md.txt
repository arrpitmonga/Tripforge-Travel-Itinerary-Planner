Tripforge – AI-Powered Travel Itinerary Planner

Tripforge is an offline, AI-powered travel itinerary generator that uses a locally stored TensorFlow Lite model to produce realistic, day-wise travel plans for Indian cities and states without any internet or backend dependency.

The project combines machine learning, geospatial intelligence, and budgeting logic to generate itineraries tailored to user preferences such as location, budget, and trip duration.

Project Overview

Tripforge consists of two main components:

Component	Description
Android Application	Built using Android Studio (Java/Kotlin). Loads a local TensorFlow Lite model to generate itineraries entirely offline.
Model Training Module	Contains Python scripts and Jupyter notebooks for data preprocessing, model training, and exporting the .tflite model and assets used in the Android app.
Repository Structure
Tripforge-Travel-Itinerary-Planner/
├── android-app/                     # Android Studio project
│   ├── app/
│   └── build.gradle
│
├── model/                           # ML model training and preprocessing
│   ├── data/
│   ├── notebooks/
│   ├── scripts/
│   ├── requirements.txt
│   └── exports/                     # Files used inside the Android app
│       ├── itinerary_model.tflite
│       ├── pois_for_app.csv
│       ├── encoders.json
│       └── scaler.json
│
├── .gitignore
├── android-deps.md
└── README.md

Key Features
Machine Learning Model

Trained on over 300 Points of Interest (POIs) across India.

Predicts suitability scores for attractions based on:

City or state.

Estimated visit cost.

Time required.

Budget level (Low, Moderate, High).

Proximity to the chosen destination.

Outputs ranked POIs that are grouped into a multi-day itinerary.

Region-Aware Planning

Expands results to nearby cities (within a 200 km radius) if insufficient POIs are found locally.

Ensures regionally consistent itineraries and avoids unrealistic long-distance jumps.

Supports both city-level and state-level queries.

Budget-Aware Planning
Budget Level	Daily Cap	Description
Low	₹1000/day	Basic, low-cost attractions.
Moderate	₹2500/day	Balanced itinerary.
High	₹5000/day	Premium experiences.
Time Management

Distributes activities over available trip days (typically 6 hours per day).

Ensures that cost and time limits per day are not exceeded.

Geospatial Awareness

Each POI has latitude and longitude values (generated using Geopy).

The model prioritizes geographically close POIs and expands outward as necessary.

User Input and Output

Inputs:

Destination (City or State)

Number of Days

Budget Level

Outputs:

Ranked list of attractions per day.

Estimated visit cost and time for each attraction.

Automatic day-wise grouping of POIs.

Model Artifacts Used in the App
File	Description
itinerary_model.tflite	Quantized TensorFlow Lite model used for on-device inference.
pois_for_app.csv	Compact dataset containing processed POIs.
encoders.json	Label encoder mappings for categorical variables.
scaler.json	Mean and scale values for numeric normalization and de-normalization.

These files must be placed in the Android app’s app/src/main/assets/ directory.

Model Training Environment
1. Setup
cd model
python -m venv venv
venv\Scripts\activate       # or source venv/bin/activate on Linux/Mac
pip install -r requirements.txt

2. Rebuild and Train
python scripts/train_model.py

3. Export

The export script will produce the following files in model/exports/:

itinerary_model.tflite

pois_for_app.csv

encoders.json

scaler.json

Android App Integration
1. Dependencies

Add the TensorFlow Lite dependency to your app-level build.gradle:

dependencies {
    implementation 'org.tensorflow:tensorflow-lite:2.12.0'
    // Optional helper library
    // implementation 'org.tensorflow:tensorflow-lite-support:0.4.3'
}

2. Assets Placement

Place the exported files in:

app/src/main/assets/
 ├── itinerary_model.tflite
 ├── pois_for_app.csv
 ├── encoders.json
 └── scaler.json

3. Model Usage

Use a helper class such as ItineraryPlanner.java to:

Load and run the TensorFlow Lite model.

Score all POIs for the selected destination.

Generate a multi-day itinerary.

Example:

ItineraryPlanner planner = new ItineraryPlanner(this);
Map<Integer, List<ItineraryPlanner.Poi>> itinerary =
    planner.planItinerary("Jaipur", 3, "moderate", 200);

for (int day : itinerary.keySet()) {
    Log.d("Tripforge", "Day " + day + ":");
    for (ItineraryPlanner.Poi poi : itinerary.get(day)) {
        Log.d("Tripforge", " • " + poi.name + " (" + poi.category + ")");
    }
}

Example Output
Suggested 3-day itinerary for Jaipur (moderate budget):

Day 1:
 • Amber Fort (fort) — ₹700 est., 2.0 h
 • Hawa Mahal (palace) — ₹400 est., 1.0 h

Day 2:
 • City Palace (palace) — ₹850 est., 2.0 h
 • Albert Hall Museum (museum) — ₹600 est., 1.5 h

Day 3:
 • Pushkar Lake (temple) — ₹450 est., 1.5 h
 • Ajmer Sharif Dargah (shrine) — ₹300 est., 1.0 h

Deployment and GitHub Release

If the .tflite model file exceeds 50 MB:

Do not commit it directly to the repository.

Create a GitHub Release under the "Releases" tab.

Upload the itinerary_model.tflite file there.

Add a download reference link in this README file.

Best Practices

Maintain the exact input order used during training when invoking the TensorFlow Lite model.

The app operates entirely offline; no network or API calls are required.

Avoid retraining or renaming the encoder/scaler files unless you re-export all assets.

Do not include large raw datasets or intermediate files in the repository.

For smaller app size, consider replacing the CSV with an SQLite database.

Tools Used
Tool	Purpose
TensorFlow 2.12	Model training and TensorFlow Lite conversion
Scikit-Learn	Data preprocessing, encoding, and scaling
Geopy	Automatic geocoding for POIs
Pandas / NumPy	Data manipulation and feature engineering
Android Studio	Mobile app development
TensorFlow Lite (Java)	On-device inference for itinerary generation
License

This project is released under the MIT License.
You are free to use, modify, and distribute the software under the terms of the license.

Contributors

Developers & Machine Learning Engineers: Arpit Monga, Parth Bhoir, Pranay Gaikwad, Neel Rawale
Project: Tripforge – AI Travel Itinerary Planner
Year: 2025

Summary

Tripforge is a region-aware, budget-conscious, offline AI system that transforms travel data into actionable itineraries.
It generates realistic, data-driven travel plans entirely on-device, enabling users to explore intelligently without needing an internet connection.

### Model Download
The latest trained TensorFlow Lite model can be downloaded from the
[Tripforge v1.0.0 Release](https://github.com/arrpitmonga/Tripforge-Travel-Itinerary-Planner/releases/latest).
