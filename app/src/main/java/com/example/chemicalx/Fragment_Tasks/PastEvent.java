package com.example.chemicalx.Fragment_Tasks;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PastEvent implements Comparable<PastEvent> {
    private int eventCategory;
    private long eventDuration;
    private double normalisedEventDuration;
    private long dtend;

    public PastEvent(int eventCategory, long eventDuration, long dtend) {
        this.eventCategory = eventCategory;
        this.eventDuration = eventDuration;
        this.normalisedEventDuration = Math.tanh(Math.log(((double) this.eventDuration) / 1000 / 60 / 60 / 6));
        this.dtend = dtend;
    }

    public static PastEvent constructFromJSONObject(JSONObject jsonObject) throws JSONException {
        int eventCat = 0;
        long eventDur = 0;
        long endOfEvent = 0L;
        eventCat = jsonObject.getInt("eventCategory");
        eventDur = jsonObject.getLong("eventDuration");
        endOfEvent = jsonObject.getLong("endOfEvent");
        return new PastEvent(eventCat, eventDur, endOfEvent);
    }

    public Map<String, Object> getPastEventMap(long startOfNextFreeTime) {
        Map<String, Object> pastEventMap = new HashMap<>();

        long timeSinceEvent = startOfNextFreeTime - this.dtend;
        double normalisedTimeSinceEvent = 2.0 * Math.atan(((double) timeSinceEvent) / 1000 / 60 / 60 / 24) / Math.PI;

        pastEventMap.put("eventCategory", this.eventCategory);
        pastEventMap.put("eventDuration", this.normalisedEventDuration);
        pastEventMap.put("timeSinceEvent", normalisedTimeSinceEvent);

        return pastEventMap;
    }

    public JSONObject getJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("eventCategory", this.eventCategory);
        jsonObject.put("eventDuration", this.eventDuration);
        jsonObject.put("endOfEvent", this.dtend);

        return jsonObject;
    }

    public long getEndTime() {
        return dtend;
    }

    @Override
    public int compareTo(PastEvent o) {
        return (int) (this.dtend - o.dtend);
    }
}