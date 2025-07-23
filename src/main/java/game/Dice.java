package game;

import java.util.ArrayList;
import java.util.List;
import model.GameMetadata;
import org.apache.commons.lang3.tuple.ImmutablePair;
import player.Player;
import player.PlayerType;

public class Dice {

  private final List<Player> players;
  private static int activeDice = 6;
  private static List<Integer> diceList;
  private static final org.apache.log4j.Logger LOGGER =
          org.apache.log4j.Logger.getLogger(Dice.class.getName());

  GameMetadata metadata;
  private int highestTurnScore = 0;

  public Dice(List<Player> players) {
    this.players = players;
  }

  private static void nextTurn() {
    activeDice = 6;
    diceList.clear();
  }

  private Player getOpponent(Player currentPlayer) {
    for (Player player : players) {
      if (!player.equals(currentPlayer)) {
        return player;
      }
    }
    return null; // Should never happen in 2-player game
  }

  public void playGame() {
    boolean someoneHasWon = false;
    int turnCounter = 0;

    Player currentPlayer;
    diceList = new ArrayList<>();
    activeDice = 6;

    while (!someoneHasWon) {
      int currentPlayerPosition = turnCounter % 2;
      currentPlayer = players.get(currentPlayerPosition);
      Player opponent = getOpponent(currentPlayer);
      currentPlayer.incrementTurn();

      int runningScore = 0;
      List<Integer> currentRoll = currentPlayer.roll(activeDice);
      diceList = currentRoll;
      ImmutablePair<Integer, Integer> gameState = currentPlayer.decideScore(diceList);

      if (gameState.left == 0) {
        // Busted immediately
        LOGGER.info(String.format("Player %s has busted", currentPlayer.name));
        currentPlayer.incrementTimesBusted();

        // Record the bust decision (they had no choice)
        currentPlayer.recordDecision(0, activeDice, false, 0,
                opponent.score, currentRoll);

        nextTurn();
        turnCounter++;
        continue;
      }

      if (!currentPlayer.isOpen) {
        // Player must keep rolling until they open (reach 1000+ points)
        while (runningScore + gameState.left < 1000) {
          runningScore += gameState.left;

          // Only record decision if player has a choice (not forced to continue)
          if (gameState.right > 0) {
            currentPlayer.recordDecision(runningScore, gameState.right, false,
                    gameState.left, opponent.score, currentRoll);
          }

          if (gameState.right == 0) {
            // Used all dice, automatically get 6 back (hot dice)
            LOGGER.info(String.format("Player %s used all dice, continuing with 6", currentPlayer.name));
            activeDice = 6;
          } else {
            activeDice = gameState.right;
          }

          currentRoll = currentPlayer.roll(activeDice);
          gameState = currentPlayer.decideScore(currentRoll);

          if (gameState.left == 0) {
            // Busted while trying to open
            LOGGER.info(String.format("Player %s has busted while opening", currentPlayer.name));
            currentPlayer.incrementTimesBusted();

            // Record the bust
            currentPlayer.recordDecision(runningScore, activeDice, false,
                    -runningScore, opponent.score, currentRoll);

            nextTurn();
            turnCounter++;
            break;
          }
        }

        if (runningScore + gameState.left >= 1000) {
          // Successfully opened
          runningScore += gameState.left;
          LOGGER.info(String.format("Player %s has opened with %s",
                  currentPlayer.name, runningScore));
          currentPlayer.isOpen = true;

          // Record the opening decision (forced to hold)
          currentPlayer.recordDecision(runningScore, gameState.right,
                  true, runningScore,
                  opponent.score, currentRoll);

          nextTurn();
          turnCounter++;
        }
      } else {
        // Player is open - now they can make strategic decisions
          boolean continueTurn = true;

        while (continueTurn) {
          runningScore += gameState.left;

          // Decision point: should the player hold or continue?
          boolean shouldHold = currentPlayer.shouldHold(runningScore, gameState.right, opponent.score);

          if (shouldHold) {
            // Player decides to hold
            LOGGER.info(String.format("Player %s has held with %s", currentPlayer.name, runningScore));

            // Check for winning/busting conditions first
            if (currentPlayer.score + runningScore > 10000) {
              LOGGER.info(String.format("Player %s has busted over 10000", currentPlayer.name));

              // Record decision that led to bust
              currentPlayer.recordDecision(runningScore, gameState.right, true,
                      -runningScore, opponent.score, currentRoll);

              nextTurn();
              turnCounter++;
              continueTurn = false;
            } else if (currentPlayer.score + runningScore == 10000) {
              // Winner!
              LOGGER.info(String.format("Player %s has won, with average roll score of %s",
                      currentPlayer.name, currentPlayer.calculateAverageTurnScore()));
              currentPlayer.score = 10000;
              someoneHasWon = true;

              // Record winning decision
              currentPlayer.recordDecision(runningScore, gameState.right, true,
                      runningScore, opponent.score, currentRoll);

              metadata = GameMetadata.builder()
                      .players(players)
                      .winningPlayer(currentPlayer)
                      .totalTurns(turnCounter)
                      .winningPlayerAverageRollScore(currentPlayer.calculateAverageTurnScore())
                      .highestTurnScore(highestTurnScore)
                      .totalStealAttempts(players.stream().mapToInt(p -> p.stealAttempts).sum())
                      .successfulSteals(players.stream().mapToInt(p -> p.successfulSteals).sum())
                      .totalBusts(players.stream().mapToInt(p -> p.timesBusted).sum())
                      .build();
              nextTurn();
              turnCounter++;
              continueTurn = false;
            } else {
              // Normal hold - but first check if opponent wants to steal!

              // **THIS IS WHERE WE ADD THE STEALING LOGIC**
              boolean opponentWantsToSteal = checkOpponentSteal(opponent, currentPlayer, runningScore);

              if (opponentWantsToSteal) {
                LOGGER.info(String.format("Player %s attempts to steal %d points from %s",
                        opponent.name, runningScore, currentPlayer.name));

                int stealResult = attemptSteal(opponent, currentPlayer, runningScore, turnCounter);

                if (stealResult > 0) {
                  // Successful steal!
                  LOGGER.info(String.format("Player %s successfully stole with %d points (beat %d)",
                          opponent.name, stealResult, runningScore));

                  // Give points to the stealing player
                  opponent.setScore(opponent.getScore() + stealResult);
                  LOGGER.info(String.format("Player %s Score after steal: %s", opponent.name, opponent.getScore()));

                  // Original player gets nothing this turn
                  LOGGER.info(String.format("Player %s lost their turn due to steal", currentPlayer.name));

                  // Record the steal attempt success for the stealing player (already done in attemptSteal)
                  // Record the loss for the original player
                  currentPlayer.recordDecision(runningScore, gameState.right, true,
                          0, opponent.score, currentRoll); // 0 because they lost the points to steal

                } else {
                  // Failed steal - original player keeps their points
                  LOGGER.info(String.format("Player %s failed to steal, %s keeps %d points",
                          opponent.name, currentPlayer.name, runningScore));

                  // Original player gets their points as normal
                  currentPlayer.setScore(currentPlayer.getScore() + runningScore);
                  LOGGER.info(String.format("Player %s Score: %s", currentPlayer.name, currentPlayer.getScore()));

                  // Record successful hold decision for original player
                  currentPlayer.recordDecision(runningScore, gameState.right, true,
                          runningScore, opponent.score, currentRoll);

                  // Stealing player's failure is already recorded in attemptSteal method

                }
                  nextTurn();
                  turnCounter++;
                  continueTurn = false;
              } else {
                // No steal attempt - normal hold
                currentPlayer.setScore(currentPlayer.getScore() + runningScore);
                LOGGER.info(String.format("Player %s Score: %s", currentPlayer.name, currentPlayer.getScore()));

                // Record successful hold decision
                currentPlayer.recordDecision(runningScore, gameState.right, true,
                        runningScore, opponent.score, currentRoll);

                nextTurn();
                turnCounter++;
                continueTurn = false;
              }
            }
          }
        }
      }
    }
  }

  // Check if opponent wants to attempt to steal the turn
  private boolean checkOpponentSteal(Player opponent, Player currentPlayer, int heldScore) {
    LOGGER.info(String.format("Checking if %s wants to steal %d points from %s",
            opponent.name, heldScore, currentPlayer.name));

    // Simple AI decision: steal if held score is high enough and we're behind
    if (!opponent.isOpen) {
      LOGGER.info(String.format("%s not open, cannot steal", opponent.name));
      return false; // Can't steal if not opened
    }

    int scoreGap = currentPlayer.score + heldScore - opponent.score; // Gap after the hold

    LOGGER.info(String.format("Score gap after hold would be: %d (current player would have %d, opponent has %d)",
            scoreGap, currentPlayer.score + heldScore, opponent.score));

    // More likely to steal if:
    // 1. The held score is substantial (>= 250 points)
    // 2. We're significantly behind or the game is close

    if (heldScore < 250) {
      LOGGER.info(String.format("Held score %d too low to steal", heldScore));
      return false; // Not worth the risk for small scores
    }

    // Aggressive players more likely to steal
      boolean shouldSteal;
      if (opponent.playerType == PlayerType.PLAYER_TYPE.AGGRESSIVE) {
      // Steal if held score >= 300 or if we're behind by 500+
          shouldSteal = heldScore >= 300 || scoreGap >= 500;
      LOGGER.info(String.format("AGGRESSIVE player decision: heldScore=%d (>=300?), scoreGap=%d (>=500?) -> %b",
              heldScore, scoreGap, shouldSteal));
      } else {
      // Safe players only steal for big scores or when far behind
          shouldSteal = heldScore >= 500 || scoreGap >= 1500;
      LOGGER.info(String.format("SAFE player decision: heldScore=%d (>=500?), scoreGap=%d (>=1500?) -> %b",
              heldScore, scoreGap, shouldSteal));
      }
      return shouldSteal;
  }

  // Attempt to steal the opponent's held turn
  private int attemptSteal(Player stealingPlayer, Player originalPlayer, int targetScore, int turnCounter) {
    LOGGER.info(String.format("Player %s attempts to steal %d points from %s",
            stealingPlayer.name, targetScore, originalPlayer.name));

    stealingPlayer.stealAttempts++; // Track steal attempts

    int stealRunningScore = 0;
    int stealActiveDice = 6;
    boolean stealContinue = true;

    while (stealContinue && stealRunningScore <= targetScore) {
      List<Integer> stealRoll = stealingPlayer.roll(stealActiveDice);
      ImmutablePair<Integer, Integer> stealResult = stealingPlayer.decideScore(stealRoll);

      if (stealResult.left == 0) {
        // Busted the steal attempt
        LOGGER.info(String.format("Player %s busted steal attempt", stealingPlayer.name));
        stealingPlayer.incrementTimesBusted();

        // Record the failed steal
        stealingPlayer.recordDecision(stealRunningScore, stealActiveDice, false,
                -stealRunningScore, originalPlayer.score, stealRoll);
        return 0; // Failed steal
      }

      stealRunningScore += stealResult.left;

      if (stealRunningScore > targetScore) {
        // Successfully beat the target score
        LOGGER.info(String.format("Player %s successfully stole with %d points (target was %d)",
                stealingPlayer.name, stealRunningScore, targetScore));

        stealingPlayer.successfulSteals++; // Track successful steals

        // Track highest turn score
        if (stealRunningScore > highestTurnScore) {
          highestTurnScore = stealRunningScore;
          LOGGER.info(String.format("New highest turn score: %d by %s (steal attempt)",
                  highestTurnScore, stealingPlayer.name));
        }

        // Record successful steal decision
        stealingPlayer.recordDecision(stealRunningScore, stealResult.right, true,
                stealRunningScore, originalPlayer.score, stealRoll);
        return stealRunningScore;
      }

      // Decide whether to continue the steal attempt
      // For steals, be more aggressive since we need to beat the target
      boolean shouldContinueSteal = false;
      if (stealResult.right <= 2) {
        // Too risky with 1-2 dice
        shouldContinueSteal = true; // Stop stealing
      } else if (stealRunningScore < targetScore / 2) {
        // Not even halfway to target, keep going
        shouldContinueSteal = false; // Continue stealing
      } else {
        // Use normal strategy
        shouldContinueSteal = stealingPlayer.shouldHold(stealRunningScore, stealResult.right, originalPlayer.score);
      }

      if (shouldContinueSteal) {
        // Give up on steal - not enough points
        LOGGER.info(String.format("Player %s gave up steal attempt with %d points (needed %d)",
                stealingPlayer.name, stealRunningScore, targetScore + 1));

        // Record decision to stop stealing
        stealingPlayer.recordDecision(stealRunningScore, stealResult.right, true,
                0, originalPlayer.score, stealRoll); // 0 because steal failed
        return 0; // Failed to beat target
      }

      // Continue stealing
      stealingPlayer.recordDecision(stealRunningScore, stealResult.right, false,
              stealResult.left, originalPlayer.score, stealRoll);

      if (stealResult.right == 0) {
        // Hot dice in steal attempt
        LOGGER.info(String.format("Player %s got hot dice during steal, continuing with %d points",
                stealingPlayer.name, stealRunningScore));
        stealActiveDice = 6;
      } else {
        stealActiveDice = stealResult.right;
      }
    }

    return 0; // Should not reach here
  }
}