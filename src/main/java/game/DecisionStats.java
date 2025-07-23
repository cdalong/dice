package game;// Create new file: src/main/java/analytics/DecisionStats.java

public class DecisionStats {
    private int holdDecisions = 0;
    private int continueDecisions = 0;
    private double totalHoldOutcome = 0;
    private double totalContinueOutcome = 0;
    private int holdSuccesses = 0; // positive outcomes
    private int continueSuccesses = 0; // positive outcomes

    public void addDecision(boolean decidedToHold, int outcome) {
        if (decidedToHold) {
            holdDecisions++;
            totalHoldOutcome += outcome;
            if (outcome > 0) {
                holdSuccesses++;
            }
        } else {
            continueDecisions++;
            totalContinueOutcome += outcome;
            if (outcome > 0) {
                continueSuccesses++;
            }
        }
    }

    public double getAverageHoldOutcome() {
        return holdDecisions > 0 ? totalHoldOutcome / holdDecisions : 0;
    }

    public double getAverageContinueOutcome() {
        return continueDecisions > 0 ? totalContinueOutcome / continueDecisions : 0;
    }

    public double getHoldSuccessRate() {
        return holdDecisions > 0 ? (double) holdSuccesses / holdDecisions : 0;
    }

    public double getContinueSuccessRate() {
        return continueDecisions > 0 ? (double) continueSuccesses / continueDecisions : 0;
    }

    public String getOptimalStrategy() {
        if (holdDecisions == 0 && continueDecisions == 0) {
            return "NO_DATA";
        }

        if (holdDecisions == 0) {
            return "CONTINUE (no hold data)";
        }

        if (continueDecisions == 0) {
            return "HOLD (no continue data)";
        }

        double holdExpectedValue = getAverageHoldOutcome();
        double continueExpectedValue = getAverageContinueOutcome();

        if (holdExpectedValue > continueExpectedValue) {
            return String.format("HOLD (%.1f vs %.1f avg points)",
                    holdExpectedValue, continueExpectedValue);
        } else {
            return String.format("CONTINUE (%.1f vs %.1f avg points)",
                    continueExpectedValue, holdExpectedValue);
        }
    }

    public int getTotalDecisions() {
        return holdDecisions + continueDecisions;
    }

    @Override
    public String toString() {
        return String.format(
                "Hold: %d decisions, %.1f avg points, %.1f%% success | " +
                        "Continue: %d decisions, %.1f avg points, %.1f%% success | " +
                        "Optimal: %s",
                holdDecisions, getAverageHoldOutcome(), getHoldSuccessRate() * 100,
                continueDecisions, getAverageContinueOutcome(), getContinueSuccessRate() * 100,
                getOptimalStrategy()
        );
    }
}