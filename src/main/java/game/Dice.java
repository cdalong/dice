package game;

import org.apache.commons.lang3.tuple.ImmutablePair;
import player.Player;
import player.PlayerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    gamePlayers.add(cameron);
    gamePlayers.add(fisk);
    Player currentPlayer;
    diceList = new ArrayList<>();
    activeDice = 6;
    while (!someoneHasWon) {
      int currentPlayerPosition = turnCounter % 2;
      currentPlayer = gamePlayers.get(currentPlayerPosition);
      currentPlayer.incrementTurn();
      int runningScore = 0;
      diceList = currentPlayer.roll(activeDice);
      ImmutablePair<Integer, Integer> gameState = currentPlayer.decideScore(diceList);

      if (gameState.left == 0) {
        // busted
        LOGGER.info(String.format("Player %s has busted with", currentPlayer.name));
        nextTurn();
        turnCounter++;
        continue;
      }
      if (!currentPlayer.isOpen) {
        while (runningScore + gameState.left <= 1000) {
          if (gameState.left == 0)
          {
            // busted
            LOGGER.info(String.format("Player %s has held with %s", currentPlayer.name, runningScore));
            nextTurn();
            turnCounter++;
            break;
          }
          runningScore += gameState.left;
          if (runningScore >= 1000) {

            LOGGER.info(String.format("Player %s has opened with %s", currentPlayer.name, runningScore));
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
        LOGGER.info(String.format("Player %s has held with %s", currentPlayer.name, gameState.left));
        if (currentPlayer.score + gameState.left > 10000)
        {
          LOGGER.info(String.format("Player %s has busted over 10000", currentPlayer.name));
          nextTurn();
          turnCounter++;
        }
        else if (currentPlayer.score + gameState.left == 10000)
        {
          LOGGER.info(String.format("Player %s has won, with average roll score of %s", currentPlayer.name, currentPlayer.calculateAverageTurnScore()));
          someoneHasWon = true;
          nextTurn();
          turnCounter++;
        }
        else if (currentPlayer.score + gameState.left < 10000) {
          currentPlayer.setScore(gameState.left + currentPlayer.getScore());
          LOGGER.info(String.format("Player %s Score: %s", currentPlayer.name, currentPlayer.getScore()));
          nextTurn();
          turnCounter++;
        }
        // TODO: next player continues if above threshold
        continue;
      }
    }
  }
}
