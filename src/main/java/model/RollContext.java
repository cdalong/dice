package model;

import java.util.List;
import java.util.Map;

public class RollContext {
    public Map<Integer, Integer> currentDiceCount;
    public int scoringDiceUsed;
    public boolean rolledMultiple;
    public boolean rolledStraight;
    public List<Integer> availableStrategies;

    public RollContext() {
        // Default constructor
    }
}
