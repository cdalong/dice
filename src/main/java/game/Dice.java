package game;

import java.util.ArrayList;
import java.util.List;
import model.GameMetadata;
import org.apache.commons.lang3.tuple.ImmutablePair;
import player.Player;

public class Dice {

  private final List<Player> players;
  private static int activeDice = 6;
  private static List<Integer> diceList;
  private static final org.apache.log4j.Logger LOGGER =
          org.apache.log4j.Logger.getLogger(Dice.class.getName());

  GameMetadata metadata;

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
          assert opponent != null;
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

          // Record decision to continue (forced)
          currentPlayer.recordDecision(runningScore, gameState.right, false,
                  gameState.left, opponent.score, currentRoll);

          if (gameState.right == 0) {
            // Used all dice, get 6 back
            gameState = new ImmutablePair<>(gameState.left, 6);
          }

          currentRoll = currentPlayer.roll(gameState.right);
          gameState = currentPlayer.decideScore(currentRoll);

          if (gameState.left == 0) {
            // Busted while trying to open
            LOGGER.info(String.format("Player %s has busted while opening", currentPlayer.name));
            currentPlayer.incrementTimesBusted();

            // Record the bust
            currentPlayer.recordDecision(runningScore, gameState.right, false,
                    -runningScore, opponent.score, currentRoll);

            nextTurn();
            turnCounter++;
            break;
          }
        }

        if (runningScore + gameState.left >= 1000) {
          // Successfully opened
          LOGGER.info(String.format("Player %s has opened with %s",
                  currentPlayer.name, runningScore + gameState.left));
          currentPlayer.isOpen = true;

          // Record the opening decision (forced to hold)
          currentPlayer.recordDecision(runningScore + gameState.left, gameState.right,
                  true, runningScore + gameState.left,
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
            // Record the decision to continue
            currentPlayer.recordDecision(runningScore, gameState.right, false,
                    gameState.left, opponent.score, currentRoll);

            if (gameState.right == 0) {
              // Used all dice, get 6 back
              gameState = new ImmutablePair<>(0, 6);
            }

            currentRoll = currentPlayer.roll(gameState.right);
            gameState = currentPlayer.decideScore(currentRoll);

            if (gameState.left == 0) {
              // Busted while continuing
              LOGGER.info(String.format("Player %s has busted", currentPlayer.name));
              currentPlayer.incrementTimesBusted();

              // Record the bust decision
              currentPlayer.recordDecision(runningScore, gameState.right, false,
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
}