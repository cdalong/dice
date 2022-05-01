import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class Dice {

  private List<Player> Players;
  private Player winner;
  private int winningScore;
  private int timesRolledStraight;
  private HashMap scoreHistogram;
  private static int activeDice = 6;
  private static List<Integer> diceList;
  private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(Dice.class.getName());


  private void CreateGame(Player[] Players) {}

  private static void nextTurn() {
    activeDice = 6;
    diceList.clear();
  }

  private boolean checkWinningScore(Player player) {
    return player.score == winningScore;
  }

  private int takeTurn(Player player) {
    return 0;
  }

  private boolean willPlayerContinue() {
    return false;
  }
  private void updateScore(Player player)
  {

  }

  public static void main(String[] args) {
    boolean someoneHasWon = false;
    int turnCounter = 0;
    List<Player> gamePlayers = new ArrayList<>();
    Player cameron = new Player(PlayerType.PLAYER_TYPE.AGGRESSIVE, 500, 3);
    cameron.setName("Cameron");
    Player fisk = new Player(PlayerType.PLAYER_TYPE.AGGRESSIVE, 200, 1);
    fisk.setName("Fisk");
    fisk.setRemainingDiceThreshold(3);
    cameron.setRemainingDiceThreshold(6);
    gamePlayers.add(cameron);
    gamePlayers.add(fisk);
    Player currentPlayer;
    diceList = new ArrayList<>();
    activeDice = 6;
    while (!someoneHasWon) {
      int currentPlayerPosition = turnCounter % 2;
      currentPlayer = gamePlayers.get(currentPlayerPosition);
      int runningScore = 0;
      diceList = currentPlayer.roll(activeDice);
      ImmutablePair<Integer, Integer> gameState = currentPlayer.decideScore(diceList);

      if (gameState.left == 0) {
        // busted
        LOGGER.info("Player has busted");
        nextTurn();
        turnCounter++;
        continue;
      }
      if (!currentPlayer.isOpen) {
        while (runningScore + gameState.left <= 1000) {
          if (gameState.left == 0)
          {
            // busted
            LOGGER.info("Player has failed to open");
            nextTurn();
            turnCounter++;
            break;
          }
          runningScore += gameState.left;
          if (runningScore >= 1000) {

            LOGGER.info("Player has opened");
            currentPlayer.isOpen = true;
            nextTurn();
            turnCounter++;
            break;
            // stay
          }
          gameState = currentPlayer.decideScore(currentPlayer.roll(gameState.right));
        }
      }

      else if (gameState.right <= currentPlayer.remainingDiceThreshold || currentPlayer.score > 8500) {
        // player holds
        LOGGER.info("Player has held with:");
        LOGGER.info(gameState.left);
        if (currentPlayer.score + gameState.left > 10000)
        {
          LOGGER.info("Player has busted over 10000");
          LOGGER.info(currentPlayer.name);
          LOGGER.info(currentPlayer.score);
          nextTurn();
          turnCounter++;
        }
        else if (currentPlayer.score + gameState.left == 10000)
        {
          LOGGER.info("Player has won");
          LOGGER.info(currentPlayer.name);
          someoneHasWon = true;
          nextTurn();
          turnCounter++;
        }
        else if (currentPlayer.score + gameState.left < 10000) {
          currentPlayer.setScore(gameState.left + currentPlayer.getScore());
          LOGGER.info("Player Score:");
          LOGGER.info(currentPlayer.getScore());
          nextTurn();
          turnCounter++;
        }
        // TODO: next player continues if above threshold
        continue;
      }
    }
  }
}
