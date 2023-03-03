package net.polar.database;


import java.util.HashMap;
import java.util.Map;

/**
 * Pretend this is a schema for the database
 */
public class PlayerData {

    private final Map<Integer, Long> parkourCheckPoints = new HashMap<>();

    private long bestParkourTime = 0;

    private final Map<Integer, Long> elytraParkourCheckPoints = new HashMap<>();
    private long bestElytraParkourTime = 0;

    public PlayerData(Map<Integer, Long> parkourCheckPoints, Map<Integer, Long> elytraParkourCheckPoints, long bestParkourTime, long bestElytraParkourTime) {
        this.parkourCheckPoints.putAll(parkourCheckPoints);
        this.elytraParkourCheckPoints.putAll(elytraParkourCheckPoints);
    }

    public Map<Integer, Long> getParkourCheckPoints() {
        return parkourCheckPoints;
    }

    public Map<Integer, Long> getElytraParkourCheckPoints() {
        return elytraParkourCheckPoints;
    }

    public void setParkourCheckPoint(int checkpoint, long time) {
        parkourCheckPoints.put(checkpoint, time);
    }

    public void setElytraParkourCheckPoint(int checkpoint, long time) {
        elytraParkourCheckPoints.put(checkpoint, time);
    }

    public Long getParkourCheckPoint(int checkpoint) {
        return parkourCheckPoints.get(checkpoint) == null ? -1L : parkourCheckPoints.get(checkpoint);
    }

    public Long getElytraParkourCheckPoint(int checkpoint) {
        return elytraParkourCheckPoints.get(checkpoint) == null ? -1L : elytraParkourCheckPoints.get(checkpoint);
    }

    public void removeParkourCheckPoint(int checkpoint) {
        parkourCheckPoints.remove(checkpoint);
    }

    public void removeElytraParkourCheckPoint(int checkpoint) {
        elytraParkourCheckPoints.remove(checkpoint);
    }

    public boolean hasParkourCheckPoint(int checkpoint) {
        return parkourCheckPoints.containsKey(checkpoint);
    }

    public boolean hasElytraParkourCheckPoint(int checkpoint) {
        return elytraParkourCheckPoints.containsKey(checkpoint);
    }

    public Long getBestParkourTime() {
        return bestParkourTime;
    }

    public void setBestParkourTime(long bestParkourTime) {
        this.bestParkourTime = bestParkourTime;
    }

    public Long getBestElytraParkourTime() {
        return bestElytraParkourTime;
    }

    public void setBestElytraParkourTime(long bestElytraParkourTime) {
        this.bestElytraParkourTime = bestElytraParkourTime;
    }
}
