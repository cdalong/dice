package player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;
import model.DecisionPoint;
import model.RollContext;

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

  // Enhanced performance metrics for ML
  public int totalPointsFromHolding = 0;
  public int totalPointsFromContinuing = 0;
  public int timesHeldAndSucceeded = 0;
  public int timesContinuedAndBusted = 0;
  public int timesContinuedAndSucceeded = 0;
  public int stealAttempts = 0;
  public int successfulSteals = 0;
  public int highestSingleTurn = 0;

  // Situational statistics
  public Map<String, Integer> decisionsByGameState = new HashMap<>();
  public Map<Integer, List<Integer>> scoreByRemainingDice = new HashMap<>();

  // Cache for bust probability calculations
  private static final Map<Integer, Double> BUST_PROBABILITY_CACHE = new HashMap<>();

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
          multiplesRolled += 1; // Only increment once per multiple type
        }
        else if (curDiceVal >= 3) {
          if (curDiceVal == 3) {
            currentPendingScore += diceRoll * 1000;
          } else {
            currentPendingScore += diceRoll * 1000 * (2 * (curDiceVal - 3));
          }
          activeDice -= curDiceVal;
          LOGGER.info(
                  String.format("Player %s rolled a Multiple Of %d ones", this.getName(), curDiceVal));
          multiplesRolled += 1; // Only increment once per multiple type
        } else if (diceRoll == 1) {
          currentPendingScore += curDiceVal * 100;
          activeDice -= curDiceVal;
        } else if (diceRoll == 5) {
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

  // ML Decision Recording Methods
  public void recordDecision(int pendingScore, int remainingDice,
                             boolean decidedToHold, int outcome,
                             int opponentScore, List<Integer> lastRoll) {
    DecisionPoint decision = new DecisionPoint();

    // Basic state
    decision.currentScore = this.score;
    decision.opponentScore = opponentScore;
    decision.pendingScore = pendingScore;
    decision.remainingDice = remainingDice;
    decision.turnNumber = this.turnNumber;
    decision.isPlayerOpen = this.isOpen;
    decision.decidedToHold = decidedToHold;
    decision.actualOutcome = outcome;

    // Calculate probabilities and values
    decision.bustProbability = calculateBustProbability(remainingDice);
    decision.expectedValue = calculateExpectedValue(remainingDice, pendingScore);
    decision.holdValue = pendingScore;

    // Derived metrics
    decision.scoreGap = this.score - opponentScore;
    decision.pointsNeededToWin = 10000 - this.score;
    decision.pointsNeededToOpen = this.isOpen ? 0 : Math.max(0, 1000 - this.score);

    decisionHistory.add(decision);

    // Update performance metrics
    updatePerformanceMetrics(decidedToHold, outcome);
  }

  private void updatePerformanceMetrics(boolean decidedToHold, int outcome) {
    if (decidedToHold) {
      totalPointsFromHolding += outcome;
      if (outcome > 0) {
        timesHeldAndSucceeded++;
        // Track highest single turn
        if (outcome > highestSingleTurn) {
          highestSingleTurn = outcome;
        }
      }
    } else {
      totalPointsFromContinuing += outcome;
      if (outcome > 0) {
        timesContinuedAndSucceeded++;
      } else {
        timesContinuedAndBusted++;
      }
    }
  }

  private double calculateBustProbability(int diceCount) {
    return BUST_PROBABILITY_CACHE.computeIfAbsent(diceCount,
            this::calculateBustProbabilityBySimulation);
  }

  private double calculateBustProbabilityBySimulation(int diceCount) {
    if (diceCount <= 0) return 1.0;

    int simulations = 10000;
    int bustCount = 0;

    for (int i = 0; i < simulations; i++) {
      List<Integer> testRoll = roll(diceCount);
      ImmutablePair<Integer, Integer> result = decideScore(testRoll);
      if (result.left == 0) {
        bustCount++;
      }
    }

    return (double) bustCount / simulations;
  }

  private double calculateExpectedValue(int diceCount, int currentPending) {
    if (diceCount <= 0) return 0.0;

    int simulations = 5000; // Reduced for performance
    double totalValue = 0;

    for (int i = 0; i < simulations; i++) {
      List<Integer> testRoll = roll(diceCount);
      ImmutablePair<Integer, Integer> result = decideScore(testRoll);

      if (result.left == 0) {
        // Bust - lose all pending points from this turn
        totalValue += -currentPending;
      } else {
        // Score - gain the points (simplified: assume we hold after this roll)
        totalValue += result.left;
      }
    }

    return totalValue / simulations;
  }

  // Method to determine if player should hold based on their strategy
  public boolean shouldHold(int pendingScore, int remainingDice, int opponentScore) {
    // This is where the current AI strategy is implemented
    if (!this.isOpen) {
      // Must keep rolling until open (this shouldn't be called for unopened players)
      return false;
    }

    // Never "hold" with 0 dice remaining - that's automatic continuation
    if (remainingDice == 0) {
      return false;
    }

    if (remainingDice <= this.remainingDiceThreshold) {
      // Too risky to continue
      return true;
    }

    if (this.score > 8500) {
      // Close to winning, be more conservative
      return true;
    }

    if (pendingScore >= this.rollThreshold) {
      // Have enough points to be satisfied
      return true;
    }

    // Additional logic for aggressive vs safe players
    if (this.playerType == PlayerType.PLAYER_TYPE.AGGRESSIVE) {
      // Aggressive players take more risks
      double expectedValue = calculateExpectedValue(remainingDice, pendingScore);
      return expectedValue <= pendingScore * 0.5; // More willing to risk
    } else {
      // Safe players are more conservative
      double expectedValue = calculateExpectedValue(remainingDice, pendingScore);
      return expectedValue <= pendingScore * 0.8; // Less willing to risk
    }
  }

  public void incrementTurn() {
    this.turnNumber += 1;
  }

  public void incrementTimesBusted() {
    this.timesBusted += 1;
  }

  public boolean isOpen() {
    return isOpen;
  }

  public void setOpen(boolean open) {
    isOpen = open;
  }

  public int getRollThreshold() {
    return rollThreshold;
  }

  public void setRollThreshold(int rollThreshold) {
    this.rollThreshold = rollThreshold;
  }

  public int getRemainingDiceThreshold() {
    return remainingDiceThreshold;
  }

  public void setRemainingDiceThreshold(int remainingDiceThreshold) {
    this.remainingDiceThreshold = remainingDiceThreshold;
  }

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }

  public PlayerType.PLAYER_TYPE getPlayerType() {
    return playerType;
  }

  public void setPlayerType(PlayerType.PLAYER_TYPE playerType) {
    this.playerType = playerType;
  }

  public int calculateAverageTurnScore() {
    return turnNumber > 0 ? score / turnNumber : 0;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}