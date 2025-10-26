package com.example.travelitineraryplanner.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.travelitineraryplanner.R;
import com.example.travelitineraryplanner.ml.Poi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Adapter that shows day cards (each with an inner RecyclerView of POIs).
 * Expects item_day_plan.xml and item_poi.xml (IDs used in these layouts).
 */
public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.DayViewHolder> {

    private final Map<Integer, List<Poi>> dayPlans;
    private final OnPoiClickListener poiClickListener;

    public interface OnPoiClickListener {
        void onPoiClick(Poi poi);
    }

    public ItineraryAdapter(Map<Integer, List<Poi>> dayPlans, OnPoiClickListener listener) {
        this.dayPlans = dayPlans;
        this.poiClickListener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_plan, parent, false);
        return new DayViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        List<Integer> days = new ArrayList<>(dayPlans.keySet());
        Collections.sort(days);
        int dayNumber = days.get(position);
        List<Poi> pois = dayPlans.get(dayNumber);
        holder.bind(dayNumber, pois);
    }

    @Override
    public int getItemCount() {
        return dayPlans == null ? 0 : dayPlans.size();
    }

    class DayViewHolder extends RecyclerView.ViewHolder {
        private final TextView dayNumberText;
        private final TextView daySummaryText;
        private final RecyclerView poisRecyclerView;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNumberText = itemView.findViewById(R.id.dayNumberText);
            daySummaryText = itemView.findViewById(R.id.daySummaryText);
            poisRecyclerView = itemView.findViewById(R.id.poisRecyclerView);

            poisRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            poisRecyclerView.setNestedScrollingEnabled(false);
        }

        void bind(int dayNumber, List<Poi> pois) {
            dayNumberText.setText("Day " + dayNumber);

            if (pois == null || pois.isEmpty()) {
                daySummaryText.setText("No places planned");
                poisRecyclerView.setAdapter(null);
                return;
            }

            // Calculate totals for the day
            double totalTime = 0d;
            double totalCost = 0d;
            for (Poi p : pois) {
                if (p != null) {
                    totalTime += p.timeHours;
                    totalCost += p.estimatedCost;
                }
            }

            String summary = String.format("• %d places • %.1fh • ₹%.0f", pois.size(), totalTime, totalCost);
            daySummaryText.setText(summary);

            PoiAdapter adapter = new PoiAdapter(pois, poiClickListener, itemView.getContext());
            poisRecyclerView.setAdapter(adapter);
        }
    }

    /**
     * Inner adapter for POI items
     */
    private static class PoiAdapter extends RecyclerView.Adapter<PoiAdapter.PoiViewHolder> {
        private final List<Poi> pois;
        private final OnPoiClickListener listener;
        private final Context ctx;

        PoiAdapter(List<Poi> pois, OnPoiClickListener listener, Context ctx) {
            this.pois = pois;
            this.listener = listener;
            this.ctx = ctx;
        }

        @NonNull
        @Override
        public PoiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poi, parent, false);
            return new PoiViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PoiViewHolder holder, int position) {
            holder.bind(pois.get(position));
        }

        @Override
        public int getItemCount() {
            return pois == null ? 0 : pois.size();
        }

        class PoiViewHolder extends RecyclerView.ViewHolder {
            private final TextView poiName;
            private final TextView poiCategory;
            private final TextView poiDescription;
            private final TextView poiTime;
            private final TextView poiCost;
            private final ImageView poiThumbnail;

            PoiViewHolder(@NonNull View itemView) {
                super(itemView);
                poiName = itemView.findViewById(R.id.poi_name);
                poiCategory = itemView.findViewById(R.id.poi_category);
                poiDescription = itemView.findViewById(R.id.poi_description);
                poiTime = itemView.findViewById(R.id.poi_time);
                poiCost = itemView.findViewById(R.id.poi_cost);
                poiThumbnail = itemView.findViewById(R.id.poi_thumbnail);

                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && listener != null && pos < pois.size()) {
                        listener.onPoiClick(pois.get(pos));
                    }
                });
            }

            void bind(Poi poi) {
                if (poi == null) return;

                poiName.setText(poi.name != null ? poi.name : "Unknown");
                poiCategory.setText(poi.category != null ? poi.category : "");
                String desc = poi.description != null && !poi.description.isEmpty()
                        ? poi.description
                        : (poi.city != null ? poi.city + (poi.state != null ? ", " + poi.state : "") : "");
                poiDescription.setText(desc != null ? desc : "");

                double safeTime = Math.max(0.1, poi.timeHours);
                double safeCost = Math.max(0.0, poi.estimatedCost);
                poiTime.setText(String.format("%.1fh", safeTime));
                poiCost.setText(String.format("₹%.0f", safeCost));

                // load thumbnail: prefer drawable resource name, then URL, else placeholder
                if (poi.thumbnailUrl != null && !poi.thumbnailUrl.isEmpty()) {
                    int resId = getDrawableResourceId(ctx, poi.thumbnailUrl);
                    if (resId != 0) {
                        Glide.with(ctx)
                                .load(resId)
                                .apply(new RequestOptions().placeholder(R.drawable.ic_place_holder).error(R.drawable.ic_place_holder))
                                .into(poiThumbnail);
                    } else if (poi.thumbnailUrl.startsWith("http")) {
                        Glide.with(ctx)
                                .load(poi.thumbnailUrl)
                                .apply(new RequestOptions().placeholder(R.drawable.ic_place_holder).error(R.drawable.ic_place_holder))
                                .into(poiThumbnail);
                    } else {
                        poiThumbnail.setImageResource(R.drawable.ic_place_holder);
                    }
                } else {
                    poiThumbnail.setImageResource(R.drawable.ic_place_holder);
                }
            }

            private int getDrawableResourceId(Context c, String name) {
                try {
                    return c.getResources().getIdentifier(name, "drawable", c.getPackageName());
                } catch (Exception e) {
                    return 0;
                }
            }
        }
    }
}
