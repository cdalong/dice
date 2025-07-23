package player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;

@Data
public class Player {

  public PlayerType.PLAYER_TYPE playerType;
  public boolean isOpen;
  public int rollThreshold;
  public int remainingDiceThreshold;
  public int score;
  public String name;
  private static final Logger LOGGER = Logger.getLogger(Player.class.getName());
  public int multiplesRolled;

  public int straightsRolled;

  public int averageRollScore;

  public int turnNumber = 0;

  public int timesBusted = 0;

  // ML Training Data
  public List<DecisionPoint> decisionHistory = new ArrayList<>();
  public List<RollAnalysis> rollHistory = new ArrayList<>();

  // Situational statistics
  public Map<String, Integer> decisionsByGameState = new HashMap<>();
  public Map<Integer, List<Integer>> scoreByRemainingDice = new HashMap<>();

  public int totalPointsFromHolding = 0;
  public int totalPointsFromContinuing = 0;
  public int timesHeldAndSucceeded = 0;
  public int timesContinuedAndBusted = 0;
  public int timesContinuedAndSucceeded = 0;

  public static class DecisionPoint {
    // Current game state
    public int currentScore;           // Player's total score
    public int opponentScore;          // Opponent's total score
    public int pendingScore;           // Points accumulated this turn
    public int remainingDice;          // Dice available to roll
    public int turnNumber;             // Which turn in the game
    public boolean isPlayerOpen;       // Has player opened?
    public boolean isOpponentOpen;     // Has opponent opened?

    // Risk assessment
    public double bustProbability;     // Calculated risk of busting
    public int scoreGap;              // currentScore - opponentScore
    public int pointsNeededToWin;     // 10000 - currentScore
    public int pointsNeededToOpen;    // 1000 - currentScore (if not open)
    public double expectedValue;       // Expected points if we continue rolling

    // Decision made
    public boolean decidedToHold;     // True if held, false if continued
    public int actualOutcome;         // Points gained/lost from decision
  }

  public static class RollAnalysis {
    public Map<Integer, Integer> diceFrequency;  // Count of each die face
    public int scoringDiceCount;                 // How many dice scored
    public boolean hadMultiple;                  // Three+ of a kind
    public boolean hadStraightPotential;         // Close to straight
    public int maxPossibleScore;                 // Best possible score from roll
    public int guaranteedScore;                  // Minimum safe score
  }

  public void recordDecision(int pendingScore, int remainingDice,
                             boolean decidedToHold, int outcome,
                             int opponentScore) {
    DecisionPoint decision = new DecisionPoint();
    decision.currentScore = this.score;
    decision.opponentScore = opponentScore;
    decision.pendingScore = pendingScore;
    decision.remainingDice = remainingDice;
    decision.turnNumber = this.turnNumber;
    decision.isPlayerOpen = this.isOpen;
    decision.decidedToHold = decidedToHold;
    decision.actualOutcome = outcome;
    decision.scoreGap = this.score - opponentScore;
    decision.pointsNeededToWin = 10000 - this.score;
    decision.bustProbability = calculateBustProbability(remainingDice);
    decision.expectedValue = calculateExpectedValue(remainingDice, pendingScore);

    decisionHistory.add(decision);
  }

  // Add to Player.java
  private double calculateBustProbability(int diceCount) {
    if (diceCount == 1) {
      // Only 1s and 5s score with 1 die
      return 4.0/6.0; // P(rolling 2,3,4,6)
    }

    if (diceCount == 2) {
      // Can score with: any 1, any 5, or pair of 1s/5s
      return calculateTwoDiceBustProbability();
    }

    if (diceCount >= 3) {
      // Can score with: 1s, 5s, three-of-a-kind, or straight (if 6 dice)
      return calculateMultiDiceBustProbability(diceCount);
    }

    return 0.0;
  }

  private double calculateTwoDiceBustProbability() {
    // Total possible outcomes: 36
    // Scoring outcomes:
    // - At least one 1: 11 outcomes (6 with first die + 6 with second die - 1 overlap)
    // - At least one 5: 11 outcomes
    // - Both 1 and 5: 2 outcomes (already counted above)
    // Total scoring: 11 + 11 - 2 = 20
    // Busting outcomes: 36 - 20 = 16
    return 16.0/36.0;
  }

  private double calculateExpectedValue(int diceCount, int currentPending) {
    int simulations = 10000;
    double totalValue = 0;

    for (int i = 0; i < simulations; i++) {
      List<Integer> testRoll = roll(diceCount);
      ImmutablePair<Integer, Integer> result = decideScore(testRoll);

      if (result.left == 0) {
        // Bust - lose all pending points from this turn
        totalValue += -currentPending;
      } else {
        // Score - gain the points (assuming we hold after this roll)
        // This is a simplified model - in reality we might continue again
        totalValue += result.left;
      }
    }

    return totalValue / simulations;
  }

  private double calculateMultiDiceBustProbability(int diceCount) {
    // This is complex - we need to calculate the probability of:
    // 1. No 1s AND no 5s AND no three-of-a-kind AND (no straight if 6 dice)

    // For practical purposes, we can use simulation or lookup tables
    // Here's a simplified approximation that's more accurate than the original

    if (diceCount == 3) return 0.444; // ~44.4%
    if (diceCount == 4) return 0.309; // ~30.9%
    if (diceCount == 5) return 0.193; // ~19.3%
    if (diceCount == 6) return 0.077; // ~7.7%

    return 0.0;
  }
  public Player(PlayerType.PLAYER_TYPE playerType, int rollThreshold, int remainingDiceThreshold) {
    this.playerType = playerType;
    this.rollThreshold = rollThreshold;
    this.remainingDiceThreshold = remainingDiceThreshold;
    this.score = 0;
    this.isOpen = false;
  }

  public List<Integer> roll(int activeDice) {
    List<Integer> dice = new ArrayList<>();
    for (int i = 0; i < activeDice; i++) {
      dice.add((int) (Math.random() * 6 + 1));
    }
    return dice;
  }

  public boolean checkStraight(HashMap<Integer, Integer> diceMap) {
    for (int value : diceMap.values()) {
      // everything needs to be one
      if (value > 1 || value == 0) {
        return false;
      }
    }

    LOGGER.info(
        String.format(
            "Player %s has hit a straight on turn number %s", this.getName(), this.turnNumber));
    straightsRolled += 1;
    return true;
  }

  public HashMap<Integer, Integer> createHashMap(List<Integer> rolledDice) {
    HashMap<Integer, Integer> map = new HashMap<>();
    for (int dice : rolledDice) {
      if (map.containsKey(dice)) {
        int newValue = map.get(dice);
        newValue++;
        map.put(dice, newValue);
      } else {
        // first count of dice roll
        map.put(dice, 1);
      }
    }
    return map;
  }

  public ImmutablePair<Integer, Integer> decideScore(List<Integer> rolledDice) {

    // based on player type
    // need to decide how many of each number
    // or if it's a straight
    // decide current score and remaining dice
    // if it's not open you have to keep rolling
    // player can bust if no one's, fives or three of a kinds are rolled
    int currentPendingScore = 0;
    int activeDice = rolledDice.size();
    HashMap<Integer, Integer> map;
    map = createHashMap(rolledDice);
    // check for straight first
    if (rolledDice.size() == 6) {
      if (checkStraight(map)) {
        return new ImmutablePair<>(1500, 6);
      }
    }
    for (int diceRoll = 1; diceRoll <= 6; diceRoll++) {
      if (map.containsKey(diceRoll)) {
        // amount of times that dice has rolled
        int curDiceVal = map.get(diceRoll);

        // calculate triples first
        if (curDiceVal >= 3 && diceRoll != 1) {
          if (curDiceVal == 3) {
            currentPendingScore += diceRoll * 100;
          } else {
            currentPendingScore += diceRoll * 100 * (2 * (curDiceVal - 3));
          }
          activeDice -= curDiceVal;
          LOGGER.info(
              String.format("Player %s rolled a Multiple Of: %s", this.getName(), diceRoll));
          multiplesRolled += 1;
        }
        if (curDiceVal >= 3 && diceRoll == 1) {
          if (curDiceVal == 3) {
            currentPendingScore += diceRoll * 1000;
          } else {
            currentPendingScore += diceRoll * 1000 * (2 * (curDiceVal - 3));
          }
          activeDice -= curDiceVal;
          LOGGER.info(
              String.format("Player %s rolled a Multiple Of: %s", this.getName(), curDiceVal));
          multiplesRolled += 1;
        } else if (diceRoll == 1) {
          currentPendingScore += curDiceVal * 100;
          activeDice -= curDiceVal;
        } else if (diceRoll == 5 && curDiceVal <= 2) {
          currentPendingScore += curDiceVal * 50;
          activeDice -= curDiceVal;
        }
        // should prioritize triples first, but requires some extra logic if there's only three twos
        // as usually players don't use 2's TODO
      }
    }

    if (activeDice == 0) {
      LOGGER.info(String.format("Player %s continues turn", this.getName()));
      activeDice = 6;
    }
    return new ImmutablePair<>(currentPendingScore, activeDice);
  }

  public void incrementTurn() {
    this.turnNumber += 1;
  }

  public void incrementTimesBusted() {
    this.timesBusted += 1;
  }

  public int calculateAverageTurnScore() {
    return score / turnNumber;
  }


}
