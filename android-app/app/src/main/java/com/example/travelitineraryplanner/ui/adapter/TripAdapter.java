package com.example.travelitineraryplanner.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.travelitineraryplanner.R;
import com.example.travelitineraryplanner.data.local.TripRequest;
import com.example.travelitineraryplanner.data.local.ItineraryItem;
import com.example.travelitineraryplanner.data.repository.TripRepository;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.HashSet;

/**
 * Generic TripAdapter that tolerates slightly different item layout ids.
 * Ensure your item_trip.xml has at least one of the destination/date/duration TextView ids:
 *  - trip_destination OR tripDestination OR destinationText
 *  - trip_date OR tripDate OR dateTextView
 *  - trip_duration OR tripDuration OR durationText
 */
public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<TripRequest> trips;
    private OnTripClickListener listener;
    private OnTripEditListener editListener;
    private OnTripDeleteListener deleteListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public interface OnTripClickListener {
        void onOpen(TripRequest trip);
    }

    public interface OnTripEditListener {
        void onEdit(TripRequest trip);
    }

    public interface OnTripDeleteListener {
        void onDelete(TripRequest trip);
    }

    public TripAdapter(List<TripRequest> trips) {
        this.trips = trips != null ? trips : new ArrayList<>();
    }

    public void setOnTripClickListener(OnTripClickListener l) {
        this.listener = l;
    }

    public void setOnTripEditListener(OnTripEditListener l) {
        this.editListener = l;
    }

    public void setOnTripDeleteListener(OnTripDeleteListener l) {
        this.deleteListener = l;
    }

    public void setTrips(List<TripRequest> trips) {
        this.trips = trips != null ? trips : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        TripRequest t = trips.get(position);
        holder.bind(t);
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    class TripViewHolder extends RecyclerView.ViewHolder {
        TextView destinationText;
        TextView dateText;
        TextView durationText;
        TextView btnView;
        TextView btnEdit;
        TextView btnDelete;
        TextView poiSummaryText;
        View root;

        TripViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView;
            // try several common ids so adapter is resilient to slightly different layouts
            destinationText = findTextView(itemView,
                    "trip_destination", "tripDestination", "destinationText", "tvDestination");
            dateText = findTextView(itemView,
                    "trip_date", "tripDate", "dateTextView", "tvDate");
            durationText = findTextView(itemView,
                    "trip_duration", "tripDuration", "durationText", "tvDuration");
            poiSummaryText = itemView.findViewById(R.id.trip_poi_summary);

            // Get the action buttons (View button is removed from layout)
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);

            // Make the entire card clickable
//            itemView.setOnClickListener(v -> {
//                int pos = getAdapterPosition();
//                if (pos >= 0 && pos < trips.size() && listener != null) {
//                    listener.onOpen(trips.get(pos));
//                }
//            });

            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    // Show POI summary when Edit button is clicked
                    if (poiSummaryText != null) {
                        poiSummaryText.setVisibility(View.VISIBLE);
                    }
                    
                    int pos = getAdapterPosition();
                    if (pos >= 0 && pos < trips.size() && editListener != null) {
                        editListener.onEdit(trips.get(pos));
                    }
                });
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos >= 0 && pos < trips.size() && deleteListener != null) {
                        deleteListener.onDelete(trips.get(pos));
                    }
                });
            }
        }

        void bind(TripRequest t) {
            if (destinationText != null) {
                destinationText.setText(t.destination != null ? t.destination : "Unknown");
            }
            if (dateText != null) {
                if (t.createdAt != null) {
                    dateText.setText(dateFormat.format(t.createdAt));
                } else {
                    dateText.setText("Recent");
                }
            }
            if (durationText != null) {
                durationText.setText(t.duration + " days • " + t.budget);
            }

            // Display POI summary with description of places
            if (poiSummaryText != null) {
                // Get trip ID to load itinerary items
                int tripId = t.id;

                // Set initial loading text
                poiSummaryText.setText("Places to visit: Loading...");

                // Create repository to get itinerary items
                TripRepository repository = new TripRepository((android.app.Application) root.getContext().getApplicationContext());

                // Get itinerary items for this trip
                repository.getItineraryItemsByTripId(String.valueOf(tripId), items -> {
                    // Build a description of places
                    StringBuilder placesDescription = new StringBuilder("Places to visit: ");

                    if (items != null && !items.isEmpty()) {
                        // Create a set to avoid duplicate place names
                        Set<String> uniquePlaces = new HashSet<>();

                        // Add up to 3 unique place names
                        int count = 0;
                        for (ItineraryItem item : items) {
                            if (item.name != null && !item.name.isEmpty() && uniquePlaces.add(item.name)) {
                                if (count > 0) {
                                    placesDescription.append(", ");
                                }
                                placesDescription.append(item.name);
                                count++;

                                // Limit to 3 places
                                if (count >= 3) {
                                    break;
                                }
                            }
                        }

                        // Add "and more" if there are more places
                        if (items.size() > 3) {
                            placesDescription.append(" and more");
                        }
                    } else {
                        placesDescription.append("Tap to add places to your itinerary");
                    }

                    // Update UI on main thread
                    final String finalDescription = placesDescription.toString();
                    if (poiSummaryText != null) {
                        poiSummaryText.post(() -> poiSummaryText.setText(finalDescription));
                    }
                });
            }

            // Hide the Edit button as it's not needed
            if (btnEdit != null) {
                btnEdit.setVisibility(View.GONE);
            }

            // Make the entire item more visible
            root.setBackgroundResource(R.color.white);
        }

        private TextView findTextView(View root, String... possibleIds) {
            for (String idName : possibleIds) {
                int id = root.getResources().getIdentifier(idName, "id", root.getContext().getPackageName());
                if (id != 0) {
                    View v = root.findViewById(id);
                    if (v instanceof TextView) return (TextView) v;
                }
            }
            // No fallback needed, just return null if no matching ID found
            return null;
        }
    }
}
