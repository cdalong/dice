package game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.DecisionPoint;
import model.GameMetadata;
import player.Player;
import player.PlayerType;


public class GameGenerator {

  private static final org.apache.log4j.Logger LOGGER =
          org.apache.log4j.Logger.getLogger(GameGenerator.class.getName());

  private static final List<GameMetadata> listOfGamesPlayed = new ArrayList<>();

  private static final int gamesPlayed = 10000; // Increased for ML training

  public static void main(String[] args) {
    LOGGER.info("Starting Farkle simulation with " + gamesPlayed + " games");

    for (int i = 0; i < gamesPlayed; i++) {
      if (i % 1000 == 0) {
        LOGGER.info("Completed " + i + " games");
      }

      List<Player> gamePlayers = new ArrayList<>();

      // Create players with different strategies for comparison
      Player player1 = new Player(PlayerType.PLAYER_TYPE.AGGRESSIVE, 400, 2);
      player1.setName("Aggressive_Player");

      Player player2 = new Player(PlayerType.PLAYER_TYPE.SAFE, 600, 3);
      player2.setName("Safe_Player");

      gamePlayers.add(player1);
      gamePlayers.add(player2);

      Dice diceGame = new Dice(gamePlayers);
      diceGame.playGame();

      GameMetadata metadata = diceGame.metadata;
      if (metadata != null) {
        listOfGamesPlayed.add(metadata);
      }
    }

    LOGGER.info("Games finished. Analyzing results...");

    // Traditional analysis
    calculateWinsByPlayer(listOfGamesPlayed);
    calculateAveragePointsByGame(listOfGamesPlayed);
    calculateAdvancedStatistics(listOfGamesPlayed);

    // ML analysis and data export
    MLDataCollector.exportTrainingData(listOfGamesPlayed, "farkle_training_data.csv");
    MLDataCollector.analyzeOptimalDecisions(listOfGamesPlayed);
    MLDataCollector.generateOptimalStrategyReport(listOfGamesPlayed);

    // Generate strategy comparison report
    generateStrategyComparisonReport(listOfGamesPlayed);
  }

  public static void calculateAveragePointsByGame(List<GameMetadata> gameMetadata) {
    HashMap<String, Integer> pointsByPlayer = HashMap.newHashMap(2);

    for (GameMetadata individualGame : gameMetadata) {
      for (Player player : individualGame.getPlayers()) {
        if (pointsByPlayer.containsKey(player.name)) {
          int totalPoints = pointsByPlayer.get(player.name);
          totalPoints = totalPoints + player.getScore();
          pointsByPlayer.put(player.name, totalPoints);
        } else {
          pointsByPlayer.put(player.name, player.getScore());
        }
      }
    }

    LOGGER.info("=== AVERAGE POINTS PER GAME ===");
    for (String playerName : pointsByPlayer.keySet()) {
      int averagePoints = pointsByPlayer.get(playerName);
      averagePoints = averagePoints / gamesPlayed;
      pointsByPlayer.put(playerName, averagePoints);
      LOGGER.info(String.format("%s: %d average points", playerName, averagePoints));
    }
  }

  public static void calculateWinsByPlayer(List<GameMetadata> gameMetadata) {
    HashMap<String, Integer> winsByPlayer = HashMap.newHashMap(2);

    for (GameMetadata individualGame : gameMetadata) {
      if (winsByPlayer.containsKey(individualGame.getWinningPlayer().name)) {
        int wins = winsByPlayer.get(individualGame.getWinningPlayer().name);
        wins++;
        winsByPlayer.put(individualGame.getWinningPlayer().name, wins);
      } else {
        winsByPlayer.put(individualGame.getWinningPlayer().name, 1);
      }
    }

    LOGGER.info("=== WIN STATISTICS ===");
    for (Map.Entry<String, Integer> entry : winsByPlayer.entrySet()) {
      double winPercentage = (double) entry.getValue() / gamesPlayed * 100;
      LOGGER.info(String.format("%s: %d wins (%.1f%%)",
              entry.getKey(), entry.getValue(), winPercentage));
    }
  }

  public static void calculateAdvancedStatistics(List<GameMetadata> gameMetadata) {
    LOGGER.info("=== ADVANCED GAME STATISTICS ===");

    // Game length statistics
    int totalTurns = 0;
    int shortestGame = Integer.MAX_VALUE;
    int longestGame = 0;
    int highestGameScore = 0;
    int totalStealAttempts = 0;
    int successfulSteals = 0;

    // Player performance metrics
    Map<String, PlayerStats> playerStats = new HashMap<>();

    for (GameMetadata game : gameMetadata) {
      totalTurns += game.getTotalTurns();
      shortestGame = Math.min(shortestGame, game.getTotalTurns());
      longestGame = Math.max(longestGame, game.getTotalTurns());
      highestGameScore = Math.max(highestGameScore, game.getHighestTurnScore());
      totalStealAttempts += game.getTotalStealAttempts();
      successfulSteals += game.getSuccessfulSteals();

      for (Player player : game.getPlayers()) {
        PlayerStats stats = playerStats.computeIfAbsent(player.name, k -> new PlayerStats());
        stats.addGame(player, game.getWinningPlayer().equals(player));
      }
    }

    double averageTurns = (double) totalTurns / gameMetadata.size();
    double stealSuccessRate = totalStealAttempts > 0 ? (double) successfulSteals / totalStealAttempts * 100 : 0;

    LOGGER.info(String.format("Average game length: %.1f turns", averageTurns));
    LOGGER.info(String.format("Shortest game: %d turns", shortestGame));
    LOGGER.info(String.format("Longest game: %d turns", longestGame));
    LOGGER.info(String.format("Highest single turn score: %d points", highestGameScore));
    LOGGER.info(String.format("Steal attempts: %d (%.1f%% success rate)", totalStealAttempts, stealSuccessRate));

    // Player statistics
    LOGGER.info("=== PLAYER PERFORMANCE ===");
    for (Map.Entry<String, PlayerStats> entry : playerStats.entrySet()) {
      LOGGER.info(String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
    }
  }

  public static void generateStrategyComparisonReport(List<GameMetadata> gameMetadata) {
    LOGGER.info("=== STRATEGY COMPARISON REPORT ===");

    Map<PlayerType.PLAYER_TYPE, StrategyStats> strategyStats = new HashMap<>();

    for (GameMetadata game : gameMetadata) {
      for (Player player : game.getPlayers()) {
        PlayerType.PLAYER_TYPE type = player.getPlayerType();
        StrategyStats stats = strategyStats.computeIfAbsent(type, k -> new StrategyStats());

        stats.addGame(player, game.getWinningPlayer().equals(player));
      }
    }

    for (Map.Entry<PlayerType.PLAYER_TYPE, StrategyStats> entry : strategyStats.entrySet()) {
      LOGGER.info(String.format("%s Strategy: %s",
              entry.getKey(), entry.getValue().toString()));
    }
  }

  // Helper classes for statistics
  private static class PlayerStats {
    private int gamesPlayed = 0;
    private int wins = 0;
    private int totalScore = 0;
    private int totalTurns = 0;
    private int totalBusts = 0;
    private int totalMultiples = 0;
    private int totalStraights = 0;
    private int totalStealAttempts = 0;
    private int successfulSteals = 0;
    private int highestTurnScore = 0;

    public void addGame(Player player, boolean won) {
      gamesPlayed++;
      if (won) wins++;
      totalScore += player.getScore();
      totalTurns += player.turnNumber;
      totalBusts += player.timesBusted;
      totalMultiples += player.multiplesRolled;
      totalStraights += player.straightsRolled;
      totalStealAttempts += player.stealAttempts;
      successfulSteals += player.successfulSteals;
      highestTurnScore = Math.max(highestTurnScore, player.highestSingleTurn);
    }

    @Override
    public String toString() {
      double winRate = (double) wins / gamesPlayed * 100;
      double avgScore = (double) totalScore / gamesPlayed;
      double avgTurns = (double) totalTurns / gamesPlayed;
      double bustRate = (double) totalBusts / totalTurns * 100;
      double avgMultiples = (double) totalMultiples / gamesPlayed;
      double avgStraights = (double) totalStraights / gamesPlayed;
      double stealSuccessRate = totalStealAttempts > 0 ? (double) successfulSteals / totalStealAttempts * 100 : 0;

      return String.format("%.1f%% wins, %.0f avg score, %.1f avg turns, " +
                      "%.1f%% bust rate, %.2f multiples/game, %.2f straights/game, " +
                      "%d steal attempts (%.1f%% success), highest turn: %d",
              winRate, avgScore, avgTurns, bustRate, avgMultiples, avgStraights,
              totalStealAttempts, stealSuccessRate, highestTurnScore);
    }
  }

  private static class StrategyStats {
    private int gamesPlayed = 0;
    private int wins = 0;
    private double totalExpectedValue = 0;
    private int totalDecisions = 0;
    private int holdDecisions = 0;
    private int continueDecisions = 0;

    public void addGame(Player player, boolean won) {
      gamesPlayed++;
      if (won) wins++;

      // Analyze decision patterns
      for (DecisionPoint decision : player.decisionHistory) {
        totalDecisions++;
        totalExpectedValue += decision.expectedValue;

        if (decision.decidedToHold) {
          holdDecisions++;
        } else {
          continueDecisions++;
        }
      }
    }

    @Override
    public String toString() {
      double winRate = (double) wins / gamesPlayed * 100;
      double avgExpectedValue = totalDecisions > 0 ? totalExpectedValue / totalDecisions : 0;
      double holdRate = totalDecisions > 0 ? (double) holdDecisions / totalDecisions * 100 : 0;

      return String.format("%.1f%% wins, %.1f avg expected value, %.1f%% hold rate",
              winRate, avgExpectedValue, holdRate);
    }
  }

  // Additional utility methods for testing different player configurations
  public static void runStrategyExperiment(String experimentName,
                                           List<Player> players,
                                           int numGames) {
    LOGGER.info("=== STRATEGY EXPERIMENT: " + experimentName + " ===");

    List<GameMetadata> experimentGames = new ArrayList<>();

    for (int i = 0; i < numGames; i++) {
      // Reset players for each game
      for (Player player : players) {
        player.score = 0;
        player.isOpen = false;
        player.turnNumber = 0;
        player.timesBusted = 0;
        player.multiplesRolled = 0;
        player.straightsRolled = 0;
        player.decisionHistory.clear();
      }

      Dice diceGame = new Dice(new ArrayList<>(players));
      diceGame.playGame();

      if (diceGame.metadata != null) {
        experimentGames.add(diceGame.metadata);
      }
    }

    // Analyze experiment results
    calculateWinsByPlayer(experimentGames);
    MLDataCollector.analyzeOptimalDecisions(experimentGames);
  }

  // Method to test various strategy combinations
  public static void runComprehensiveStrategyAnalysis() {
    LOGGER.info("=== COMPREHENSIVE STRATEGY ANALYSIS ===");

    // Test different risk thresholds
    int[] rollThresholds = {300, 400, 500, 600, 700};
    int[] diceThresholds = {1, 2, 3, 4};

    for (int rollThreshold : rollThresholds) {
      for (int diceThreshold : diceThresholds) {
        List<Player> testPlayers = new ArrayList<>();

        Player testPlayer = new Player(PlayerType.PLAYER_TYPE.AGGRESSIVE, rollThreshold, diceThreshold);
        testPlayer.setName(String.format("Test_R%d_D%d", rollThreshold, diceThreshold));

        Player baseline = new Player(PlayerType.PLAYER_TYPE.SAFE, 500, 3);
        baseline.setName("Baseline");

        testPlayers.add(testPlayer);
        testPlayers.add(baseline);

        String experimentName = String.format("RollThreshold_%d_DiceThreshold_%d",
                rollThreshold, diceThreshold);
        runStrategyExperiment(experimentName, testPlayers, 1000);
      }
    }
  }
}