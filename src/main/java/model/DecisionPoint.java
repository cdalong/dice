package model;

public class DecisionPoint {
    // Basic game state
    public int currentScore;
    public int opponentScore;
    public int pendingScore;
    public int remainingDice;
    public int turnNumber;
    public boolean isPlayerOpen;
    public boolean isOpponentOpen;

    // Decision and outcome
    public boolean decidedToHold;
    public int actualOutcome;

    // Calculated probabilities and values
    public double bustProbability;
    public double expectedValue;
    public int holdValue;

    // Derived metrics
    public int scoreGap;
    public int pointsNeededToWin;
    public int pointsNeededToOpen;

    // Risk assessment
    public double winProbabilityIfHold;
    public double winProbabilityIfContinue;

    public DecisionPoint() {
        // Default constructor
    }

    public DecisionPoint(int currentScore, int opponentScore, int pendingScore,
                         int remainingDice, int turnNumber, boolean isPlayerOpen,
                         boolean decidedToHold, int actualOutcome) {
        this.currentScore = currentScore;
        this.opponentScore = opponentScore;
        this.pendingScore = pendingScore;
        this.remainingDice = remainingDice;
        this.turnNumber = turnNumber;
        this.isPlayerOpen = isPlayerOpen;
        this.decidedToHold = decidedToHold;
        this.actualOutcome = actualOutcome;

        // Calculate derived metrics
        this.scoreGap = currentScore - opponentScore;
        this.pointsNeededToWin = 10000 - currentScore;
        this.pointsNeededToOpen = isPlayerOpen ? 0 : Math.max(0, 1000 - currentScore);
        this.holdValue = pendingScore;
    }
}
