import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.swing.*;
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
    while (!someoneHasWon) {
      int currentPlayerPosition = turnCounter % 2;
      currentPlayer = gamePlayers.get(currentPlayerPosition);
      int runningScore = 0;
      ImmutablePair<Integer, Integer> gameState = currentPlayer.decideScore(diceList);

      if (gameState.left == 0) {
        // busted
        nextTurn();
        turnCounter++;
        continue;
      }

      if (!currentPlayer.isOpen) {
        while (runningScore + gameState.left <= 1000) {
          runningScore += gameState.left;
          if (runningScore >= 1000) {
            currentPlayer.isOpen = true;
            nextTurn();
            turnCounter++;
            continue;
            // stay
          }
          gameState = currentPlayer.decideScore(currentPlayer.roll(gameState.right));
        }
      }

      else if (gameState.right <= currentPlayer.remainingDiceThreshold) {
        // player holds
        currentPlayer.setScore(gameState.left + currentPlayer.getScore());
        nextTurn();
        // TODO: next player continues if above threshold
        continue;
      }
    }
  }
}
