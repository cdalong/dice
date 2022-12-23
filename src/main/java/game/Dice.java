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

  public void playGame() {
    boolean someoneHasWon = false;
    int turnCounter = 0;

    Player currentPlayer;
    diceList = new ArrayList<>();
    activeDice = 6;
    while (!someoneHasWon) {
      int currentPlayerPosition = turnCounter % 2;
      currentPlayer = players.get(currentPlayerPosition);
      currentPlayer.incrementTurn();
      int runningScore = 0;
      diceList = currentPlayer.roll(activeDice);
      ImmutablePair<Integer, Integer> gameState = currentPlayer.decideScore(diceList);

      if (gameState.left == 0) {
        // busted
        LOGGER.info(String.format("Player %s has busted with", currentPlayer.name));
        currentPlayer.incrementTimesBusted();
        nextTurn();
        turnCounter++;
        continue;
      }
      if (!currentPlayer.isOpen) {
        while (runningScore + gameState.left <= 1000) {
          if (gameState.left == 0) {
            // busted
            LOGGER.info(
                String.format("Player %s has held with %s", currentPlayer.name, runningScore));
            nextTurn();
            turnCounter++;
            break;
          }
          runningScore += gameState.left;
          if (runningScore >= 1000) {

            LOGGER.info(
                String.format("Player %s has opened with %s", currentPlayer.name, runningScore));
            currentPlayer.isOpen = true;
            nextTurn();
            turnCounter++;
            break;
            // stay
          }
          gameState = currentPlayer.decideScore(currentPlayer.roll(gameState.right));
        }
      } else if (gameState.right <= currentPlayer.remainingDiceThreshold
          || currentPlayer.score > 8500) {
        // player holds
        LOGGER.info(
            String.format("Player %s has held with %s", currentPlayer.name, gameState.left));
        if (currentPlayer.score + gameState.left > 10000) {
          LOGGER.info(String.format("Player %s has busted over 10000", currentPlayer.name));
          nextTurn();
          turnCounter++;
        } else if (currentPlayer.score + gameState.left == 10000) {
          LOGGER.info(
              String.format(
                  "Player %s has won, with average roll score of %s",
                  currentPlayer.name, currentPlayer.calculateAverageTurnScore()));
          someoneHasWon = true;
          metadata =
              GameMetadata.builder()
                  .players(players)
                  .winningPlayer(currentPlayer)
                  .totalTurns(turnCounter)
                  .winningPlayerAverageRollScore(currentPlayer.calculateAverageTurnScore())
                  .build();
          nextTurn();
          turnCounter++;
        } else if (currentPlayer.score + gameState.left < 10000) {
          currentPlayer.setScore(gameState.left + currentPlayer.getScore());
          LOGGER.info(
              String.format("Player %s Score: %s", currentPlayer.name, currentPlayer.getScore()));
          nextTurn();
          turnCounter++;
        }
      }
    }
  }
}
