package com.example.travelitineraryplanner.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import org.tensorflow.lite.Interpreter;
import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Advanced Itinerary Planner with ML model integration
 * Implements exact requirements for realistic, city/state-specific multi-day itineraries
 */
public class ItineraryPlanner {
    // Constants as per requirements
    private static final int MIN_POIS_PER_DAY = 2;
    private static final double MAX_SINGLE_POI_HOURS = 6.0;
    private static final double MAX_HOURS_PER_DAY = 6.0;
    private static final double DAILY_BUDGET_LOW = 1000.0;
    private static final double DAILY_BUDGET_MODERATE = 2500.0;
    private static final double DAILY_BUDGET_HIGH = 5000.0;
    private static final double MAX_DISTANCE_KM = 200.0;
    
    // Asset file names
    private static final String MODEL_FILE = "itinerary_model_consistent_final_quant_dynamic.tflite";
    private static final String ENCODERS_FILE = "encoders.json";
    private static final String SCALER_FILE = "scaler.json";
    private static final String POIS_FILE = "pois_for_app.csv";
    
    // Model and data
    private Interpreter tflite;
    private Context context;
    private Map<String, Integer> locationEncoder;
    private Map<String, Integer> budgetEncoder;
    private Map<String, Integer> costCategoryEncoder;
    private Map<String, Integer> categoryEncoder;
    private Map<String, Double> scaler;
    private List<Poi> allPois;
    private ExecutorService executor;
    
    // Feature schema for model input (documented order)
    // [0] estimated_visit_cost_inr (scaled)
    // [1] time_hours (scaled) 
    // [2] dist_km_to_city_center (scaled)
    // [3] le_city (encoded)
    // [4] le_budget (encoded)
    // [5] le_costcat (encoded)
    // [6] le_cat (encoded)
    // [7] popularity_score (normalized)
    private static final int FEATURE_COUNT = 8;
    
    public ItineraryPlanner(Context context) {
        try {
            this.context = context;
            executor = Executors.newFixedThreadPool(4);
            System.out.println("Starting ItineraryPlanner initialization...");
            
            loadModel(context);
            loadEncoders(context);
            loadScaler(context);
            loadPois(context);
            
            System.out.println("ItineraryPlanner initialized successfully");
            System.out.println("Total POIs loaded: " + (allPois != null ? allPois.size() : 0));
            System.out.println("Model loaded: " + (tflite != null ? "Yes" : "No"));
        } catch (Exception e) {
            System.out.println("Error initializing ItineraryPlanner: " + e.getMessage());
            e.printStackTrace();
            // Initialize with defaults
            this.context = context;
            executor = Executors.newFixedThreadPool(4);
            locationEncoder = new HashMap<>();
            budgetEncoder = new HashMap<>();
            costCategoryEncoder = new HashMap<>();
            categoryEncoder = new HashMap<>();
            scaler = new HashMap<>();
            allPois = new ArrayList<>();
        }
    }
    
    private void loadModel(Context context) {
        try {
            tflite = new Interpreter(loadModelFile(context));
            System.out.println("Successfully loaded TensorFlow Lite model");
        } catch (Exception e) {
            tflite = null;
            System.out.println("Warning: Could not load TensorFlow Lite model, using fallback scoring: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            inputStream.close();
            fileDescriptor.close();
            return buffer;
        } catch (IOException e) {
            System.out.println("Error loading model file: " + e.getMessage());
            throw e;
        }
    }
    
    private String loadAssetAsString(Context context, String filename) throws IOException {
        AssetManager assetManager = context.getAssets();
        InputStream is = assetManager.open(filename);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
    }
    
    private void loadEncoders(Context context) throws IOException {
        try {
            String json = loadAssetAsString(context, ENCODERS_FILE);
            JSONObject encoders = new JSONObject(json);
            
            // Load location encoder
            locationEncoder = new HashMap<>();
            JSONArray cityClasses = encoders.getJSONObject("le_city").getJSONArray("classes");
            for (int i = 0; i < cityClasses.length(); i++) {
                locationEncoder.put(cityClasses.getString(i), i);
            }
            
            // Load budget encoder
            budgetEncoder = new HashMap<>();
            JSONArray budgetClasses = encoders.getJSONObject("le_budget").getJSONArray("classes");
            for (int i = 0; i < budgetClasses.length(); i++) {
                budgetEncoder.put(budgetClasses.getString(i), i);
            }
            
            // Load cost category encoder
            costCategoryEncoder = new HashMap<>();
            JSONArray costCatClasses = encoders.getJSONObject("le_costcat").getJSONArray("classes");
            for (int i = 0; i < costCatClasses.length(); i++) {
                costCategoryEncoder.put(costCatClasses.getString(i), i);
            }
            
            // Load category encoder
            categoryEncoder = new HashMap<>();
            JSONArray catClasses = encoders.getJSONObject("le_cat").getJSONArray("classes");
            for (int i = 0; i < catClasses.length(); i++) {
                categoryEncoder.put(catClasses.getString(i), i);
            }
            
            System.out.println("Successfully loaded encoders: " + 
                              "locations=" + locationEncoder.size() + 
                              ", budgets=" + budgetEncoder.size() + 
                              ", categories=" + categoryEncoder.size());
                              
        } catch (org.json.JSONException e) {
            System.out.println("Error loading encoders: " + e.getMessage());
            e.printStackTrace();
            
            // Initialize with default values instead of throwing exception
            initializeDefaultEncoders();
        } catch (IOException e) {
            System.out.println("Error reading encoder file: " + e.getMessage());
            e.printStackTrace();
            
            // Initialize with default values
            initializeDefaultEncoders();
        }
    }
    
    private void initializeDefaultEncoders() {
        // Default location encoder with common Indian cities
        locationEncoder = new HashMap<>();
        locationEncoder.put("Delhi", 0);
        locationEncoder.put("Mumbai", 1);
        locationEncoder.put("Bangalore", 2);
        locationEncoder.put("Chennai", 3);
        locationEncoder.put("Kolkata", 4);
        locationEncoder.put("Hyderabad", 5);
        locationEncoder.put("Jaipur", 6);
        locationEncoder.put("Agra", 7);
        locationEncoder.put("Goa", 8);
        
        // Default budget encoder
        budgetEncoder = new HashMap<>();
        budgetEncoder.put("LOW", 0);
        budgetEncoder.put("MODERATE", 1);
        budgetEncoder.put("HIGH", 2);
        
        // Default cost category encoder
        costCategoryEncoder = new HashMap<>();
        costCategoryEncoder.put("FREE", 0);
        costCategoryEncoder.put("BUDGET", 1);
        costCategoryEncoder.put("MODERATE", 2);
        costCategoryEncoder.put("LUXURY", 3);
        
        // Default category encoder
        categoryEncoder = new HashMap<>();
        categoryEncoder.put("Historical", 0);
        categoryEncoder.put("Religious", 1);
        categoryEncoder.put("Museum", 2);
        categoryEncoder.put("Park", 3);
        categoryEncoder.put("Beach", 4);
        categoryEncoder.put("Monument", 5);
        categoryEncoder.put("Market", 6);
        
        System.out.println("Initialized default encoders as fallback");
    }
    
    private void loadScaler(Context context) throws IOException {
        try {
            String json = loadAssetAsString(context, SCALER_FILE);
            JSONObject scalerData = new JSONObject(json);
            
            scaler = new HashMap<>();
            JSONArray means = scalerData.getJSONArray("mean");
            JSONArray scales = scalerData.getJSONArray("scale");
            JSONArray cols = scalerData.getJSONArray("cols");
            
            for (int i = 0; i < cols.length(); i++) {
                String col = cols.getString(i);
                scaler.put(col + "_mean", means.getDouble(i));
                scaler.put(col + "_scale", scales.getDouble(i));
            }
            
            System.out.println("Successfully loaded scaler data");
        } catch (org.json.JSONException e) {
            System.out.println("Error loading scaler: " + e.getMessage());
            e.printStackTrace();
            
            // Initialize with default values instead of throwing exception
            initializeDefaultScaler();
        } catch (IOException e) {
            System.out.println("Error reading scaler file: " + e.getMessage());
            e.printStackTrace();
            
            // Initialize with default values
            initializeDefaultScaler();
        }
    }
    
    private void initializeDefaultScaler() {
        // Initialize with reasonable default values
        scaler = new HashMap<>();
        
        // Default means
        scaler.put("estimated_visit_cost_inr_mean", 500.0);
        scaler.put("time_hours_mean", 2.0);
        scaler.put("dist_km_to_city_center_mean", 5.0);
        
        // Default scales
        scaler.put("estimated_visit_cost_inr_scale", 300.0);
        scaler.put("time_hours_scale", 1.0);
        scaler.put("dist_km_to_city_center_scale", 3.0);
        
        System.out.println("Initialized default scaler values as fallback");
    }
    
    private void loadPois(Context context) throws IOException {
        allPois = new ArrayList<>();
        
        try {
            String csv = loadAssetAsString(context, POIS_FILE);
            String[] lines = csv.split("\n");
            
            // Skip header row
            for (int i = 1; i < lines.length; i++) {
                String[] tokens = lines[i].split(",");
                if (tokens.length >= 10) { // Ensure enough columns
                    try {
                        Poi poi = new Poi();
                        poi.city = tokens[0].trim();
                        poi.state = tokens[1].trim();
                        poi.name = tokens[2].trim();
                        poi.category = tokens[3].trim();
                        poi.latitude = Double.parseDouble(tokens[4].trim());
                        poi.longitude = Double.parseDouble(tokens[5].trim());
                        
                        // Denormalize the scaled values back to actual values
                        double normalizedCost = Double.parseDouble(tokens[6].trim());
                        double normalizedTime = Double.parseDouble(tokens[7].trim());
                        
                        // Convert back to actual values using: actual = (normalized * scale) + mean
                        poi.estimatedCost = (normalizedCost * scaler.get("estimated_visit_cost_inr_scale")) + scaler.get("estimated_visit_cost_inr_mean");
                        poi.timeHours = (normalizedTime * scaler.get("time_hours_scale")) + scaler.get("time_hours_mean");
                        
                        // Ensure positive values
                        poi.estimatedCost = Math.max(0, poi.estimatedCost);
                        poi.timeHours = Math.max(0.1, poi.timeHours);
                        
                        // Set thumbnail based on category
                        poi.thumbnailUrl = generateDrawableName(poi.category);
                        
                        // Add POI to list
                        allPois.add(poi);
                    } catch (NumberFormatException e) {
                        System.out.println("Error parsing POI data at line " + i + ": " + e.getMessage());
                        // Continue to next POI
                    }
                }
            }
            
            System.out.println("Successfully loaded " + allPois.size() + " POIs");
            
        } catch (IOException e) {
            System.out.println("Error loading POIs file: " + e.getMessage());
            e.printStackTrace();
            
            // Create some default POIs as fallback
            createDefaultPois();
        }
    }
    
    private void createDefaultPois() {
        System.out.println("Creating default POIs as fallback");
        
        // Create a few default POIs for major cities
        String[] cities = {"Delhi", "Mumbai", "Bangalore", "Chennai", "Kolkata", "Hyderabad"};
        String[] categories = {"Historical", "Religious", "Museum", "Park", "Monument"};
        
        for (String city : cities) {
            for (int i = 0; i < 3; i++) {
                Poi poi = new Poi();
                poi.city = city;
                poi.state = "Default";
                poi.name = "Popular Attraction " + (i+1) + " in " + city;
                poi.category = categories[i % categories.length];
                poi.estimatedCost = 500 + (i * 200);
                poi.timeHours = 1.5 + (i * 0.5);
                poi.latitude = 0.0;
                poi.longitude = 0.0;
                poi.description = "A popular attraction in " + city;
                poi.costCategory = "MODERATE";
                poi.popularityScore = 0.8;
                poi.thumbnailUrl = generateDrawableName(poi.category);
                
                allPois.add(poi);
            }
        }
        
        // Add some specific landmark POIs with proper thumbnails
        Poi taj = new Poi();
        taj.name = "Taj Mahal";
        taj.category = "Monument";
        taj.city = "Agra";
        taj.state = "Uttar Pradesh";
        taj.estimatedCost = 1100.0;
        taj.timeHours = 3.0;
        taj.latitude = 27.1751;
        taj.longitude = 78.0421;
        taj.thumbnailUrl = "monument";
        taj.popularityScore = 0.95;
        taj.costCategory = "MODERATE";
        
        Poi qutub = new Poi();
        qutub.name = "Qutub Minar";
        qutub.category = "Monument";
        qutub.city = "Delhi";
        qutub.state = "Delhi";
        qutub.estimatedCost = 600.0;
        qutub.timeHours = 2.0;
        qutub.latitude = 28.5245;
        qutub.longitude = 77.1855;
        qutub.thumbnailUrl = "monument";
        qutub.popularityScore = 0.9;
        qutub.costCategory = "BUDGET";
        
        Poi gateway = new Poi();
        gateway.name = "Gateway of India";
        gateway.category = "Monument";
        gateway.city = "Mumbai";
        gateway.state = "Maharashtra";
        gateway.estimatedCost = 0.0;
        gateway.timeHours = 1.0;
        gateway.latitude = 18.9220;
        gateway.longitude = 72.8347;
        gateway.thumbnailUrl = "monument";
        gateway.popularityScore = 0.9;
        gateway.costCategory = "FREE";
        
        allPois.add(taj);
        allPois.add(qutub);
        allPois.add(gateway);
        
        System.out.println("Created " + allPois.size() + " default POIs");
    }
    
    /**
     * Main method to plan itinerary with exact requirements implementation
     */
    public ItineraryResult planItinerary(String location, int days, String budget) {
        // Input validation
        if (location == null || location.trim().isEmpty()) {
            return createErrorResult("Please enter a valid location.");
        }
        
        if (days <= 0) {
            days = 1; // Default to 1 day if invalid
        }
        
        if (budget == null || budget.trim().isEmpty()) {
            budget = "MODERATE"; // Default to moderate budget
        }
        
        ItineraryResult result = new ItineraryResult();
        result.dayPlans = new HashMap<>();
        result.metadata = new ItineraryResult.Metadata();
        result.metadata.location = location;
        result.metadata.days = days;
        result.metadata.budget = budget;
        result.metadata.generatedAt = System.currentTimeMillis();
        
        try {
            // Check if model is loaded
            if (tflite == null) {
                System.out.println("Interpreter is null, attempting to reload model");
                try {
                    // Use the stored context reference instead
                    if (this.context != null) {
                        loadModel(this.context);
                    } else {
                        return createErrorResult("Application context not available.");
                    }
                } catch (Exception e) {
                    System.out.println("Failed to reload model: " + e.getMessage());
                    return createErrorResult("Model failed to load — try reinstalling the app.");
                }
            }
            
            // Check if POIs are loaded
            if (allPois == null || allPois.isEmpty()) {
                System.out.println("No POIs available, creating defaults");
                createDefaultPois();
                if (allPois.isEmpty()) {
                    return createErrorResult("Could not load attractions data.");
                }
            }
            
            // Step 1: Scope selection
            List<Poi> candidates = selectScope(location, days);
            if (candidates.isEmpty()) {
                return createErrorResult("Location not recognized — try city or state name.");
            }
            
            // Step 2: Pre-filtering
            candidates = preFilter(candidates, budget);
            if (candidates.isEmpty()) {
                // If no candidates after filtering, use all from scope with relaxed budget
                candidates = selectScope(location, days);
                System.out.println("No POIs after budget filtering, using all available: " + candidates.size());
            }
            
            // Step 3: Scoring & ranking
            candidates = scoreAndRank(candidates, location, budget);
            
            // Step 4: Daily packing
            result.dayPlans = packDaily(candidates, days, budget);
            
            // Step 5: Calculate totals and generate summary
            result.totals = calculateTotals(result.dayPlans);
            result.summary = result.generateSummary();
            
            // Update metadata with source cities
            Set<String> sourceCities = new HashSet<>();
            for (List<Poi> dayPois : result.dayPlans.values()) {
                for (Poi poi : dayPois) {
                    sourceCities.add(poi.city);
                }
            }
            result.metadata.sourceCities = new ArrayList<>(sourceCities);
            
        } catch (Exception e) {
            System.out.println("Error in planItinerary: " + e.getMessage());
            e.printStackTrace();
            return createErrorResult("Failed to generate itinerary. Please try again with different parameters.");
        }
        
        return result;
    }
    
    private List<Poi> selectScope(String location, int days) {
        String normalizedLocation = location.toLowerCase().trim();
        List<Poi> candidates = new ArrayList<>();
        
        System.out.println("Selecting scope for location: " + location);
        
        // Step 1: Exact city match (highest priority)
        for (Poi poi : allPois) {
            if (poi.city.toLowerCase().equals(normalizedLocation)) {
                candidates.add(poi);
            }
        }
        
        System.out.println("Found " + candidates.size() + " POIs in exact city match");
        
        // Step 2: If insufficient, try state match
        if (candidates.size() < MIN_POIS_PER_DAY * days) {
            for (Poi poi : allPois) {
                if (poi.state.toLowerCase().equals(normalizedLocation) && !candidates.contains(poi)) {
                    candidates.add(poi);
                }
            }
            System.out.println("After state match: " + candidates.size() + " POIs");
        }
        
        // Step 3: If still insufficient, find reference point and expand by coordinates
        if (candidates.size() < MIN_POIS_PER_DAY * days) {
            Poi referencePoi = findReferencePoi(location);
            if (referencePoi != null) {
                System.out.println("Using reference POI: " + referencePoi.name + " at " + 
                                 referencePoi.latitude + ", " + referencePoi.longitude);
                
                // Expand search radius stepwise: 10km, 25km, 50km, 100km
                int[] radii = {10, 25, 50, 100};
                for (int radius : radii) {
                    for (Poi poi : allPois) {
                        if (!candidates.contains(poi)) {
                            double distance = calculateDistance(referencePoi.latitude, referencePoi.longitude,
                                                            poi.latitude, poi.longitude);
                            if (distance <= radius) {
                                candidates.add(poi);
                            }
                        }
                    }
                    System.out.println("After " + radius + "km radius: " + candidates.size() + " POIs");
                    if (candidates.size() >= MIN_POIS_PER_DAY * days) {
                        break;
                    }
                }
            }
        }
        
        // Step 4: If still insufficient, try fuzzy matching as last resort
        if (candidates.size() < MIN_POIS_PER_DAY * days) {
            for (Poi poi : allPois) {
                if (!candidates.contains(poi) && 
                    (poi.city.toLowerCase().contains(normalizedLocation) || 
                     poi.state.toLowerCase().contains(normalizedLocation))) {
                    candidates.add(poi);
                }
            }
            System.out.println("After fuzzy match: " + candidates.size() + " POIs");
        }
        
        return candidates;
    }
    
    private Poi findReferencePoi(String location) {
        String locationLower = location.toLowerCase().trim();
        
        // First try to find a POI in the exact city
        for (Poi poi : allPois) {
            if (poi.city.toLowerCase().equals(locationLower)) {
                return poi;
            }
        }
        
        // Then try to find a POI in the state
        for (Poi poi : allPois) {
            if (poi.state.toLowerCase().equals(locationLower)) {
                return poi;
            }
        }
        
        // If no exact match, try fuzzy matching
        for (Poi poi : allPois) {
            if (poi.city.toLowerCase().contains(locationLower) || 
                poi.state.toLowerCase().contains(locationLower)) {
                return poi;
            }
        }
        
        return null;
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    private List<Poi> preFilter(List<Poi> candidates, String budget) {
        List<Poi> filtered = new ArrayList<>();
        
        for (Poi poi : candidates) {
            // Budget compatibility check
            boolean budgetCompatible = isBudgetCompatible(poi.costCategory, budget);
            
            // Time constraint check
            boolean timeCompatible = poi.timeHours > 0 && poi.timeHours <= MAX_SINGLE_POI_HOURS;
            
            if (budgetCompatible && timeCompatible) {
                filtered.add(poi);
            }
        }
        
        System.out.println("Pre-filtering: " + candidates.size() + " -> " + filtered.size() + " POIs");
        return filtered;
    }
    
    private boolean isBudgetCompatible(String costCategory, String budget) {
        String budgetLower = budget.toLowerCase();
        
        if (budgetLower.contains("low")) {
            return costCategory.equals("low") || costCategory.equals("medium");
        } else if (budgetLower.contains("moderate")) {
            return costCategory.equals("low") || costCategory.equals("medium");
        } else if (budgetLower.contains("high")) {
            return true; // High budget allows all categories
        }
        
        return true; // Default allow all
    }
    
    private List<Poi> scoreAndRank(List<Poi> candidates, String location, String budget) {
        if (tflite == null) {
            // Fallback scoring without ML model
            return fallbackScoring(candidates, location, budget);
        }
        
        // Prepare features for each candidate
        float[][] inputFeatures = new float[candidates.size()][FEATURE_COUNT];
        
        for (int i = 0; i < candidates.size(); i++) {
            Poi poi = candidates.get(i);
            float[] features = prepareFeatureVector(poi, location, budget);
            inputFeatures[i] = features;
        }
        
        // Run model inference
        float[][] output = new float[1][candidates.size()];
        tflite.run(inputFeatures, output);
        
        // Apply model scores and proximity boost
        for (int i = 0; i < candidates.size(); i++) {
            Poi poi = candidates.get(i);
            poi.modelScore = output[0][i];
            
            // Calculate proximity boost
            double distance = calculateDistanceToLocation(poi, location);
            poi.proximityBoost = 1.0 / (1.0 + distance);
            
            // Final score: model_score * 0.9 + proximity_boost * 0.1
            poi.finalScore = poi.modelScore * 0.9 + poi.proximityBoost * 0.1;
        }
        
        // Sort by final score descending
        candidates.sort((a, b) -> Double.compare(b.finalScore, a.finalScore));
        
        System.out.println("Scoring completed for " + candidates.size() + " POIs");
        return candidates;
    }
    
    private float[] prepareFeatureVector(Poi poi, String location, String budget) {
        float[] features = new float[FEATURE_COUNT];
        
        // [0] estimated_visit_cost_inr (scaled)
        features[0] = (float) ((poi.estimatedCost - scaler.get("estimated_visit_cost_inr_mean")) / 
                               scaler.get("estimated_visit_cost_inr_scale"));
        
        // [1] time_hours (scaled)
        features[1] = (float) ((poi.timeHours - scaler.get("time_hours_mean")) / 
                               scaler.get("time_hours_scale"));
        
        // [2] dist_km_to_city_center (scaled) - calculate distance to location center
        double distance = calculateDistanceToLocation(poi, location);
        features[2] = (float) ((distance - scaler.get("dist_km_to_city_center_mean")) / 
                               scaler.get("dist_km_to_city_center_scale"));
        
        // [3] le_city (encoded)
        features[3] = locationEncoder.getOrDefault(poi.city, 0);
        
        // [4] le_budget (encoded)
        features[4] = budgetEncoder.getOrDefault(budget.toLowerCase(), 0);
        
        // [5] le_costcat (encoded)
        features[5] = costCategoryEncoder.getOrDefault(poi.costCategory, 0);
        
        // [6] le_cat (encoded)
        features[6] = categoryEncoder.getOrDefault(poi.category, 0);
        
        // [7] popularity_score (normalized)
        features[7] = (float) poi.popularityScore;
        
        return features;
    }
    
    private double calculateDistanceToLocation(Poi poi, String location) {
        // Find a reference POI for the location
        Poi referencePoi = findReferencePoi(location);
        if (referencePoi != null) {
            return calculateDistance(referencePoi.latitude, referencePoi.longitude, 
                                  poi.latitude, poi.longitude);
        }
        return 0.0;
    }
    
    /**
     * Generate drawable resource name for POI based on category using your POI images
     */
    private String generateDrawableName(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "ic_place_holder";
        }
        
        String lowerCategory = category.toLowerCase().trim();
        
        // Handle specific category mappings to your POI images
        // Note: For now, using a simple approach that will work with your current setup
        if (lowerCategory.contains("temple") || lowerCategory.contains("temples") || 
            lowerCategory.contains("religious") || lowerCategory.contains("religious site")) {
            return "temple";
        } else if (lowerCategory.contains("church")) {
            return "church";
        } else if (lowerCategory.contains("mosque")) {
            return "mosque";
        } else if (lowerCategory.contains("shrine") || lowerCategory.contains("gurudwara")) {
            return "shrine";
        } else if (lowerCategory.contains("fort") || lowerCategory.contains("forts")) {
            return "fort";
        } else if (lowerCategory.contains("palace") || lowerCategory.contains("palaces")) {
            return "palace";
        } else if (lowerCategory.contains("monument") || lowerCategory.contains("monuments")) {
            return "monument";
        } else if (lowerCategory.contains("memorial") || lowerCategory.contains("memorials")) {
            return "memorial";
        } else if (lowerCategory.contains("mausoleum") || lowerCategory.contains("mausoleums") ||
                   lowerCategory.contains("tomb") || lowerCategory.contains("tombs")) {
            return "mausoleum";
        } else if (lowerCategory.contains("beach") || lowerCategory.contains("beaches")) {
            return "beach";
        } else if (lowerCategory.contains("lake") || lowerCategory.contains("lakes")) {
            return "lake";
        } else if (lowerCategory.contains("park") || lowerCategory.contains("parks") ||
                   lowerCategory.contains("garden") || lowerCategory.contains("gardens") ||
                   lowerCategory.contains("botanical")) {
            return "park";
        } else if (lowerCategory.contains("museum") || lowerCategory.contains("museums") ||
                   lowerCategory.contains("gallery") || lowerCategory.contains("galleries")) {
            return "museum";
        } else if (lowerCategory.contains("market") || lowerCategory.contains("markets") ||
                   lowerCategory.contains("shopping") || lowerCategory.contains("bazaar")) {
            return "market";
        } else if (lowerCategory.contains("mall") || lowerCategory.contains("malls")) {
            return "mall";
        } else if (lowerCategory.contains("national park") || lowerCategory.contains("wildlife") ||
                   lowerCategory.contains("sanctuary") || lowerCategory.contains("sanctuaries")) {
            return "wildlife_sanctuary";
        } else if (lowerCategory.contains("zoo") || lowerCategory.contains("zoos")) {
            return "wildlife";
        } else if (lowerCategory.contains("cave") || lowerCategory.contains("caves")) {
            return "cave";
        } else if (lowerCategory.contains("hill") || lowerCategory.contains("hills") ||
                   lowerCategory.contains("mountain") || lowerCategory.contains("mountains")) {
            return "hill";
        } else if (lowerCategory.contains("waterfall") || lowerCategory.contains("waterfalls")) {
            return "waterfall";
        } else if (lowerCategory.contains("aquarium") || lowerCategory.contains("aquariums")) {
            return "aquarium";
        } else if (lowerCategory.contains("science") || lowerCategory.contains("scientific")) {
            return "science";
        } else if (lowerCategory.contains("historical") || lowerCategory.contains("historic")) {
            return "historical";
        } else if (lowerCategory.contains("promenade")) {
            return "promenade";
        } else if (lowerCategory.contains("urban development")) {
            return "urban_development_project";
        } else {
            // Default placeholder
            return "ic_place_holder";
        }
    }

    private List<Poi> fallbackScoring(List<Poi> candidates, String location, String budget) {
        // Simple fallback scoring based on popularity and proximity
        for (Poi poi : candidates) {
            double distance = calculateDistanceToLocation(poi, location);
            poi.proximityBoost = 1.0 / (1.0 + distance);
            poi.finalScore = poi.popularityScore * 0.7 + poi.proximityBoost * 0.3;
        }
        
        candidates.sort((a, b) -> Double.compare(b.finalScore, a.finalScore));
        return candidates;
    }
    
    private Map<Integer, List<Poi>> packDaily(List<Poi> candidates, int days, String budget) {
        Map<Integer, List<Poi>> dayPlans = new HashMap<>();
        List<Poi> remainingPois = new ArrayList<>(candidates);
        
        double dailyBudget = getDailyBudget(budget);
        
        // Calculate target POIs per day for more even distribution
        int totalPois = remainingPois.size();
        int targetPoisPerDay = Math.max(MIN_POIS_PER_DAY, totalPois / days);
        
        System.out.println("Total POIs: " + totalPois + ", Target per day: " + targetPoisPerDay);
        
        for (int day = 1; day <= days; day++) {
            List<Poi> dayPois = new ArrayList<>();
            double remainingTime = MAX_HOURS_PER_DAY;
            double remainingBudget = dailyBudget;
            
            System.out.println("Processing Day " + day + " with " + remainingPois.size() + " remaining POIs");
            
            // Try to get target number of POIs for this day
            int poisAdded = 0;
            Iterator<Poi> iterator = remainingPois.iterator();
            
            while (iterator.hasNext() && remainingTime > 0 && poisAdded < targetPoisPerDay) {
                Poi poi = iterator.next();
                
                // Ensure POI has valid values
                if (poi.timeHours > 0 && poi.estimatedCost >= 0 && 
                    poi.timeHours <= remainingTime && poi.estimatedCost <= remainingBudget) {
                    dayPois.add(poi);
                    remainingTime -= poi.timeHours;
                    remainingBudget -= poi.estimatedCost;
                    iterator.remove();
                    poisAdded++;
                    System.out.println("Added " + poi.name + " to Day " + day);
                }
            }
            
            // If we didn't get enough POIs and there are still some left, try to add more
            if (poisAdded < MIN_POIS_PER_DAY && !remainingPois.isEmpty()) {
                System.out.println("Day " + day + " needs more POIs, trying to add more...");
                iterator = remainingPois.iterator();
                while (iterator.hasNext() && remainingTime > 0) {
                    Poi poi = iterator.next();
                    
                    if (poi.timeHours > 0 && poi.estimatedCost >= 0 &&
                        poi.timeHours <= remainingTime && poi.estimatedCost <= remainingBudget) {
                        dayPois.add(poi);
                        remainingTime -= poi.timeHours;
                        remainingBudget -= poi.estimatedCost;
                        iterator.remove();
                        poisAdded++;
                        System.out.println("Added additional " + poi.name + " to Day " + day);
                    }
                }
            }
            
            // Always put the day in the map, even if empty
            dayPlans.put(day, dayPois);
            System.out.println("Day " + day + ": " + dayPois.size() + " POIs, " + 
                             String.format("%.1f", MAX_HOURS_PER_DAY - remainingTime) + " hours, ₹" + 
                             String.format("%.0f", dailyBudget - remainingBudget));
        }
        
        // If there are still remaining POIs, distribute them to days with capacity
        if (!remainingPois.isEmpty()) {
            System.out.println("Distributing " + remainingPois.size() + " remaining POIs...");
            distributeRemainingPois(remainingPois, dayPlans, dailyBudget);
        }
        
        return dayPlans;
    }
    
    private void distributeRemainingPois(List<Poi> remainingPois, Map<Integer, List<Poi>> dayPlans, double dailyBudget) {
        for (Poi poi : remainingPois) {
            // Find the day with the least POIs that can accommodate this POI
            int bestDay = -1;
            int minPois = Integer.MAX_VALUE;
            
            for (int day = 1; day <= dayPlans.size(); day++) {
                List<Poi> dayPois = dayPlans.get(day);
                if (dayPois == null) continue;
                
                // Check if this day can accommodate the POI
                double dayTime = dayPois.stream().mapToDouble(p -> p.timeHours).sum();
                double dayCost = dayPois.stream().mapToDouble(p -> p.estimatedCost).sum();
                
                if (dayTime + poi.timeHours <= MAX_HOURS_PER_DAY && 
                    dayCost + poi.estimatedCost <= dailyBudget) {
                    
                    if (dayPois.size() < minPois) {
                        minPois = dayPois.size();
                        bestDay = day;
                    }
                }
            }
            
            if (bestDay != -1) {
                dayPlans.get(bestDay).add(poi);
                System.out.println("Added " + poi.name + " to Day " + bestDay);
            }
        }
    }
    
    private double getDailyBudget(String budget) {
        String budgetLower = budget.toLowerCase();
        if (budgetLower.contains("low")) {
            return DAILY_BUDGET_LOW;
        } else if (budgetLower.contains("moderate")) {
            return DAILY_BUDGET_MODERATE;
        } else if (budgetLower.contains("high")) {
            return DAILY_BUDGET_HIGH;
        }
        return DAILY_BUDGET_MODERATE; // Default
    }
    
    private ItineraryResult.Totals calculateTotals(Map<Integer, List<Poi>> dayPlans) {
        double totalCost = 0;
        double totalTime = 0;
        
        for (List<Poi> dayPois : dayPlans.values()) {
            for (Poi poi : dayPois) {
                totalCost += poi.estimatedCost;
                totalTime += poi.timeHours;
            }
        }
        
        return new ItineraryResult.Totals(totalCost, totalTime);
    }
    
    private ItineraryResult createErrorResult(String message) {
        ItineraryResult result = new ItineraryResult();
        result.metadata = new ItineraryResult.Metadata();
        result.metadata.location = "";
        result.metadata.days = 0;
        result.metadata.budget = "";
        result.metadata.generatedAt = System.currentTimeMillis();
        result.dayPlans = new HashMap<>();
        result.totals = new ItineraryResult.Totals(0, 0);
        result.summary = message;
        return result;
    }
    
    public void shutdown() {
        if (tflite != null) {
            tflite.close();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}