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
        runningScore = 0;
        boolean continueTurn = true;

        while (continueTurn) {
          runningScore += gameState.left;

          // Decision point: should the player hold or continue?
          boolean shouldHold = currentPlayer.shouldHold(runningScore, gameState.right, opponent.score);

          if (shouldHold) {
            // Player decides to hold
            LOGGER.info(String.format("Player %s has held with %s", currentPlayer.name, runningScore));

            // Check for winning/busting conditions
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
                      .build();
              nextTurn();
              turnCounter++;
              continueTurn = false;
            } else {
              // Normal hold - add to score
              currentPlayer.setScore(currentPlayer.getScore() + runningScore);
              LOGGER.info(String.format("Player %s Score: %s", currentPlayer.name, currentPlayer.getScore()));

              // Record successful hold decision
              currentPlayer.recordDecision(runningScore, gameState.right, true,
                      runningScore, opponent.score, currentRoll);

              nextTurn();
              turnCounter++;
              continueTurn = false;
            }
          } else {
            // Player decides to continue rolling
            // Record the decision to continue (only if they have remaining dice)
            if (gameState.right > 0) {
              currentPlayer.recordDecision(runningScore, gameState.right, false,
                      gameState.left, opponent.score, currentRoll);
            }

            if (gameState.right == 0) {
              // Used all dice, automatically get 6 back (hot dice)
              // KEEP the running score - don't reset it!
              LOGGER.info(String.format("Player %s used all dice (hot dice), continuing with 6 dice and %d points",
                      currentPlayer.name, runningScore));
              activeDice = 6;
              // Don't record a decision here - it's automatic
              // runningScore stays the same - points accumulate!
            } else {
              activeDice = gameState.right;
            }

            currentRoll = currentPlayer.roll(activeDice);
            gameState = currentPlayer.decideScore(currentRoll);

            if (gameState.left == 0) {
              // Busted while continuing
              LOGGER.info(String.format("Player %s has busted", currentPlayer.name));
              currentPlayer.incrementTimesBusted();

              // Record the bust decision
              currentPlayer.recordDecision(runningScore, activeDice, false,
                      -runningScore, opponent.score, currentRoll);

              nextTurn();
              turnCounter++;
              continueTurn = false;
            }
          }
        }
      }
    }
  }

  // Check if opponent wants to attempt to steal the turn
  private boolean checkOpponentSteal(Player opponent, Player currentPlayer, int heldScore) {
    // Simple AI decision: steal if held score is high enough and we're behind
    if (!opponent.isOpen) {
      return false; // Can't steal if not opened
    }

    int scoreGap = currentPlayer.score - opponent.score;

    // More likely to steal if:
    // 1. We're behind
    // 2. The held score is substantial (>= 300 points)
    // 3. Opponent's strategy (aggressive players more likely to steal)

    if (heldScore < 300) {
      return false; // Not worth the risk
    }

    if (scoreGap <= 0) {
      return false; // We're not behind
    }

    // Aggressive players more likely to steal
    if (opponent.playerType == PlayerType.PLAYER_TYPE.AGGRESSIVE) {
      return heldScore >= 400 && scoreGap > 1000;
    } else {
      return heldScore >= 600 && scoreGap > 2000;
    }
  }

  // Attempt to steal the opponent's held turn
  private int attemptSteal(Player stealingPlayer, Player originalPlayer, int targetScore, int turnCounter) {
    LOGGER.info(String.format("Player %s attempts to steal %d points from %s",
            stealingPlayer.name, targetScore, originalPlayer.name));

    int stealRunningScore = 0;
    int stealActiveDice = 6;
    boolean stealContinue = true;

    while (stealContinue && stealRunningScore < targetScore) {
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
      boolean shouldContinueSteal = stealingPlayer.shouldHold(stealRunningScore, stealResult.right, originalPlayer.score);

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