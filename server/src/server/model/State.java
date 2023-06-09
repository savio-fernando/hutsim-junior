package server.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import server.Allocator;
import server.model.hazard.Hazard;
import server.model.target.Target;
import server.model.task.Task;
import tool.GsonUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class State {

    public static final int GAME_TYPE_SANDBOX = 0;
    public static final int GAME_TYPE_SCENARIO = 1;
    private final static transient Logger LOGGER = Logger.getLogger(Allocator.class.getName());
    private boolean inProgress;

    private String gameId;
    private String gameDescription;
    private int gameType;
    private String allocationMethod = "maxsum";
    private Boolean flockingEnabled = false;
    private double time;
    private boolean editMode;

    private String prov_doc;

    private final Collection<Agent> agents;
    private final Collection<Task> tasks;
    private final Collection<Task> completedTasks;
    private final Collection<Hazard> hazards;

    //State information for scenarios
    private Coordinate gameCentre;
    private final Collection<Target> targets;

    //Updated on server but only used on client.
    @SuppressWarnings("unused")
    private boolean allocationUndoAvailable;
    @SuppressWarnings("unused")
    private boolean allocationRedoAvailable;

    private Map<String, String> allocation;
    //Allocation that is WIP (i.e. not confirmed by user).
    private Map<String, String> tempAllocation;
    //Allocation created from dropped out agents.
    private Map<String, String> droppedAllocation;

    private HazardHitCollection hazardHits;

    public State() {
        agents = new ArrayList<>();
        tasks = new ArrayList<>();
        completedTasks = new ArrayList<>();
        targets = new ArrayList<>();
        hazards = new ArrayList<>();
        allocation = new ConcurrentHashMap<>();
        tempAllocation = new ConcurrentHashMap<>();
        droppedAllocation = new ConcurrentHashMap<>();
        hazardHits = new HazardHitCollection();

        allocationUndoAvailable = false;
        allocationRedoAvailable = false;

        reset();
    }

    public synchronized void reset() {
        time = 0;
        editMode = false;
        inProgress = false;

        agents.clear();
        tasks.clear();
        completedTasks.clear();
        targets.clear();
        hazards.clear();
        allocation.clear();
        tempAllocation.clear();
        hazardHits.clear();

        hazardHits.init();
    }

    @Override
    public synchronized String toString() {
        return GsonUtils.toJson(this);
    }

    public Target getTarget(String targetId) {
        return getById(targets, targetId);
    }

    public Task getTask(String taskId) {
        return getById(tasks, taskId);
    }

    public Agent getAgent(String agentId) {
        return getById(agents, agentId);
    }

    public Hazard getHazard(String hazardId) {
        return getById(hazards, hazardId);
    }

    public void add(IdObject item) {
        if(item instanceof Target)
            add(targets, (Target) item);
        else if(item instanceof  Task)
            add(tasks, (Task) item);
        else if(item instanceof Agent)
            add(agents, (Agent) item);
        else if(item instanceof Hazard)
            add(hazards, (Hazard) item);
        else
            throw new RuntimeException("Cannot add item to state, unrecognised class - " + item.getClass().getSimpleName());
    }

    public void remove(IdObject item) {
        if(item instanceof Target)
            remove(targets, (Target) item);
        else if(item instanceof  Task)
            remove(tasks, (Task) item);
        else if(item instanceof  Agent)
            remove(agents, (Agent) item);
        else
            throw new RuntimeException("Cannot remove item from state, unrecognised class - " + item.getClass().getSimpleName());
    }

    private <T extends IdObject> void add(Collection<T> items, T item) {
        if(getById(items, item.getId()) != null)
            throw new RuntimeException("Cannot add item to list - list already contains item with given id.");
        items.add(item);
    }

    private <T extends IdObject> boolean remove(Collection<T> items, T item) {
        if((item = getById(items, item.getId())) == null)
            return false;
        items.remove(item);
        return true;
    }

    private <T extends IdObject> T getById(Collection<T> items, String id) {
        List<T> matching = items.stream().filter(o -> o.getId().equals(id)).collect(Collectors.toList());
        if(matching.size() == 0)
            return null;
        if(matching.size() > 1)
            throw new RuntimeException("Two objects found with same id!");
        return matching.get(0);
    }

    //Getters and setters below
    public synchronized double getTime() {
        return time;
    }

    public synchronized void setTime(double time) {
        this.time = time;
    }

    public synchronized void incrementTime(double increment) {
        setTime(this.time + increment);
    }

    public synchronized boolean isEditMode() {
        return editMode;
    }

    public synchronized void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public synchronized String getGameId() {
        return gameId;
    }

    public synchronized void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public synchronized int getGameType() {
        return gameType;
    }

    public String getGameDescription() {
        return gameDescription;
    }

    public void setGameDescription(String gameDescription) {
        this.gameDescription = gameDescription;
    }

    public synchronized void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public synchronized void setGameCentre(Coordinate gameCentre) {
        this.gameCentre = gameCentre;
    }

    public synchronized void setAllocationMethod(String allocationMethod) {
        this.allocationMethod = allocationMethod;
    }

    public synchronized String getAllocationMethod() {
        return this.allocationMethod;
    }

    public synchronized void setFlockingEnabled(Boolean flockingEnabled) {
        this.flockingEnabled = flockingEnabled;
    }

    public synchronized Boolean isFlockingEnabled() {
        return this.flockingEnabled;
    }

    public Collection<Target> getTargets() {
        return targets;
    }

    public Collection<Task> getTasks() {
        return tasks;
    }

    public Collection<Agent> getAgents() {
        return agents;
    }

    public Map<String, String> getAllocation() {
        return allocation;
    }

    public Collection<Hazard> getHazards() {
        return hazards;
    }

    public void setAllocation(Map<String, String> allocation) {
        this.allocation = allocation;
    }

    public Map<String, String> getTempAllocation() {
        return tempAllocation;
    }

    public void setTempAllocation(Map<String, String> tempAllocation) {
        if(tempAllocation == null)
            this.tempAllocation.clear();
        else
            this.tempAllocation = tempAllocation;
    }

    public Map<String, String> getDroppedAllocation() {
        return droppedAllocation;
    }

    public synchronized void setProvDoc(String prov_doc) {
        this.prov_doc = prov_doc;
    }

    public void setAllocationUndoAvailable(boolean allocationUndoAvailable) {
        this.allocationUndoAvailable = allocationUndoAvailable;
    }

    public void setAllocationRedoAvailable(boolean allocationRedoAvailable) {
        this.allocationRedoAvailable = allocationRedoAvailable;
    }

    public void addCompletedTask(Task task) {
        this.completedTasks.add(task);
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }

    public synchronized void addHazardHit(int type, Coordinate location) {
        hazardHits.add(type, location);
    }

    public void decayHazardHits() {
        hazardHits.decayAll();
    }

    private class HazardHit {
        private Coordinate location;
        private double weight;
        private transient double decayRate;

        private HazardHit(Coordinate location, double decayRate) {
            this.location = location;
            this.weight = 1;
            this.decayRate = decayRate;
        }

        private boolean decay() {
            this.weight -= decayRate;
            return this.weight < 0;
        }
    }

    public class HazardHitCollection {
        private transient Map<Integer, Map<Coordinate, HazardHit>> hazardHits;

        private HazardHitCollection() {
            this.hazardHits = new ConcurrentHashMap<>();
        }

        private void init() {
            hazardHits.put(Hazard.NONE, new ConcurrentHashMap<>());
            hazardHits.put(Hazard.FIRE, new ConcurrentHashMap<>());
            hazardHits.put(Hazard.DEBRIS, new ConcurrentHashMap<>());
        }

        private void add(int type, Coordinate location) {
            if(this.hazardHits.containsKey(type)) {
                /* Hits should only be registered if they are far enough from all
                 * other hits. This is done by storing a rounded coordinate to provide
                 * a quick way to see if a hit is far enough away from all the other hits.
                 *
                 * The actual coordinate is kept, and this is the one that should be rendered
                 * so the heatmap does not appear 'blocky'.
                 */
                double lat = location.latitude;
                double lng = location.longitude;
                double roundedLat = Math.round(lat * 10000D) / 10000D;
                double roundedLng = Math.round(lng * 10000D) / 10000D;
                Coordinate roundedCoord = new Coordinate(roundedLat, roundedLng);
                Map<Coordinate, HazardHit> coordMap = this.hazardHits.get(type);
                HazardHit hit;
                if(type == -1)
                    hit = new HazardHit(location, 0.001);
                else
                    hit = new HazardHit(location, 0);
                coordMap.put(roundedCoord, hit);
            }
            else {
                LOGGER.severe("Could not register hazard hit - not list for hazard type " + type);
            }
        }

        private void decayAll() {
            for(Map<Coordinate, HazardHit> m : hazardHits.values()) {
                for(Map.Entry<Coordinate, HazardHit> e : m.entrySet())
                    if(e.getValue().decay())
                        m.remove(e.getKey());
            }
        }

        private void clear() {
            hazardHits.clear();
        }
    }

    public static JsonSerializer hazardHitsSerializer = new JsonSerializer<HazardHitCollection>() {
        @Override
        public JsonElement serialize(HazardHitCollection hazardHitCollection, Type type, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("-1", context.serialize(hazardHitCollection.hazardHits.get(-1).values()));
            jsonObject.add("0", context.serialize(hazardHitCollection.hazardHits.get(0).values()));
            jsonObject.add("1", context.serialize(hazardHitCollection.hazardHits.get(1).values()));
            return jsonObject;
        }
    };
}
