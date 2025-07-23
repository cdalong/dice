package game;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.GameMetadata;
import model.DecisionPoint;
import player.Player;

public class MLDataCollector {
    private static final org.apache.log4j.Logger LOGGER =
            org.apache.log4j.Logger.getLogger(MLDataCollector.class.getName());

    public static void exportTrainingData(List<GameMetadata> games, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // CSV header
            writer.println("currentScore,opponentScore,pendingScore,remainingDice," +
                    "turnNumber,isOpen,scoreGap,pointsNeededToWin," +
                    "bustProbability,expectedValue,decidedToHold,outcome,won");

            int totalDecisions = 0;
            for (GameMetadata game : games) {
                for (Player player : game.getPlayers()) {
                    boolean playerWon = player.equals(game.getWinningPlayer());

                    for (DecisionPoint decision : player.decisionHistory) {
                        writer.printf("%d,%d,%d,%d,%d,%b,%d,%d,%.3f,%.1f,%b,%d,%b%n",
                                decision.currentScore,
                                decision.opponentScore,
                                decision.pendingScore,
                                decision.remainingDice,
                                decision.turnNumber,
                                decision.isPlayerOpen,
                                decision.scoreGap,
                                decision.pointsNeededToWin,
                                decision.bustProbability,
                                decision.expectedValue,
                                decision.decidedToHold,
                                decision.actualOutcome,
                                playerWon
                        );
                        totalDecisions++;
                    }
                }
            }
            LOGGER.info("Training data exported to " + filename + " with " + totalDecisions + " decisions");
        } catch (Exception e) {
            LOGGER.error("Failed to export training data", e);
        }
    }

    public static void analyzeOptimalDecisions(List<GameMetadata> games) {
        Map<String, DecisionStats> stats = new HashMap<>();
        int totalDecisions = 0;

        for (GameMetadata game : games) {
            for (Player player : game.getPlayers()) {
                for (DecisionPoint decision : player.decisionHistory) {
                    totalDecisions++;
                    // Create context key for similar game situations
                    String context = createContextKey(decision);

                    stats.computeIfAbsent(context, k -> new DecisionStats())
                            .addDecision(decision.decidedToHold, decision.actualOutcome);
                }
            }
        }

        LOGGER.info("=== OPTIMAL DECISION ANALYSIS ===");
        LOGGER.info("Total decisions analyzed: " + totalDecisions);

        // Log optimal strategies for different contexts
        stats.entrySet().stream()
                .filter(entry -> entry.getValue().getTotalDecisions() >= 20) // Only contexts with enough data
                .sorted((a, b) -> Integer.compare(b.getValue().getTotalDecisions(),
                        a.getValue().getTotalDecisions()))
                .limit(20) // Show top 20 most common situations
                .forEach(entry -> {
                    LOGGER.info(String.format("Context: %s | %s",
                            entry.getKey(), entry.getValue().toString()));
                });
    }

    private static String createContextKey(DecisionPoint decision) {
        // Group similar game situations together
        String gamePhase;
        if (!decision.isPlayerOpen) {
            gamePhase = "opening";
        } else if (decision.currentScore < 3000) {
            gamePhase = "early";
        } else if (decision.currentScore < 7000) {
            gamePhase = "mid";
        } else {
            gamePhase = "late";
        }

        String scoreGapCategory;
        if (Math.abs(decision.scoreGap) < 1000) {
            scoreGapCategory = "close";
        } else if (decision.scoreGap > 1000) {
            scoreGapCategory = "ahead";
        } else {
            scoreGapCategory = "behind";
        }

        String pendingCategory;
        if (decision.pendingScore < 300) {
            pendingCategory = "low";
        } else if (decision.pendingScore < 600) {
            pendingCategory = "med";
        } else {
            pendingCategory = "high";
        }

        return String.format("%s_%s_pending%s_dice%d",
                gamePhase, scoreGapCategory, pendingCategory, decision.remainingDice);
    }

    public static void generateOptimalStrategyReport(List<GameMetadata> games) {
        LOGGER.info("=== STRATEGY INSIGHTS ===");

        // Analyze by remaining dice
        for (int dice = 1; dice <= 6; dice++) {
            analyzeByDiceCount(games, dice);
        }

        // Analyze by game phase
        analyzeByGamePhase(games);
    }

    private static void analyzeByDiceCount(List<GameMetadata> games, int diceCount) {
        DecisionStats stats = new DecisionStats();

        for (GameMetadata game : games) {
            for (Player player : game.getPlayers()) {
                for (DecisionPoint decision : player.decisionHistory) {
                    if (decision.remainingDice == diceCount) {
                        stats.addDecision(decision.decidedToHold, decision.actualOutcome);
                    }
                }
            }
        }

        if (stats.getTotalDecisions() > 10) {
            LOGGER.info(String.format("With %d dice remaining: %s", diceCount, stats.toString()));
        }
    }

    private static void analyzeByGamePhase(List<GameMetadata> games) {
        Map<String, DecisionStats> phaseStats = new HashMap<>();

        for (GameMetadata game : games) {
            for (Player player : game.getPlayers()) {
                for (DecisionPoint decision : player.decisionHistory) {
                    String phase;
                    if (!decision.isPlayerOpen) {
                        phase = "Opening";
                    } else if (decision.pointsNeededToWin > 5000) {
                        phase = "Early Game";
                    } else if (decision.pointsNeededToWin > 2000) {
                        phase = "Mid Game";
                    } else {
                        phase = "End Game";
                    }

                    phaseStats.computeIfAbsent(phase, k -> new DecisionStats())
                            .addDecision(decision.decidedToHold, decision.actualOutcome);
                }
            }
        }

        phaseStats.forEach((phase, stats) -> {
            if (stats.getTotalDecisions() > 10) {
                LOGGER.info(String.format("%s: %s", phase, stats.toString()));
            }
        });
    }
}