package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import model.GameMetadata;
import player.Player;
import player.PlayerType;

public class GameGenerator {

  private static final org.apache.log4j.Logger LOGGER =
      org.apache.log4j.Logger.getLogger(GameGenerator.class.getName());

  private static final List<GameMetadata> listOfGamesPlayed = new ArrayList<>();

  private static final int gamesPlayed = 1000;

  public static void main(String[] args) {

    for (int i = 0; i < gamesPlayed; i++) {

      List<Player> gamePlayers = new ArrayList<>();
      Player cameron = new Player(PlayerType.PLAYER_TYPE.AGGRESSIVE, 500, 3);
      cameron.setName("Cameron");
      Player fisk = new Player(PlayerType.PLAYER_TYPE.AGGRESSIVE, 200, 1);
      fisk.setName("Fisk");
      gamePlayers.add(cameron);
      gamePlayers.add(fisk);

      Dice diceGame = new Dice(gamePlayers);
      diceGame.playGame();

      GameMetadata metadata = diceGame.metadata;
      listOfGamesPlayed.add(metadata);
    }
    LOGGER.info("Games finished");
    calculateWinsByPlayer(listOfGamesPlayed);
    calculateAverageWinningPointsPerTurnByGame(listOfGamesPlayed);
  }

  public static void calculateAverageWinningPointsPerTurnByGame(List<GameMetadata> gameMetadata) {
    HashMap<String, Integer> pointsByPlayer = HashMap.newHashMap(2);

    for (GameMetadata individualGame : gameMetadata) {

      for (Player player : individualGame.getPlayers()) {
        if (pointsByPlayer.containsKey(player.name)) {

          // update wins
          int averagePoints = pointsByPlayer.get(player.name);
          averagePoints = averagePoints + player.calculateAverageTurnScore();
          pointsByPlayer.put(player.name, averagePoints);
        } else {
          // first win
          pointsByPlayer.put(player.name, player.calculateAverageTurnScore());
        }
      }
    }

    for (String playerName : pointsByPlayer.keySet()) {

      int averagePoints = pointsByPlayer.get(playerName);
      averagePoints = averagePoints / gamesPlayed;
      pointsByPlayer.put(playerName, averagePoints);
    }

    LOGGER.info(Arrays.toString(pointsByPlayer.entrySet().toArray()));
  }

  public static void calculateWinsByPlayer(List<GameMetadata> gameMetadata) {

    HashMap<String, Integer> winsByPlayer = HashMap.newHashMap(2);

    for (GameMetadata individualGame : gameMetadata) {

      if (winsByPlayer.containsKey(individualGame.getWinningPlayer().name)) {

        // update wins
        int wins = winsByPlayer.get(individualGame.getWinningPlayer().name);
        wins++;
        winsByPlayer.put(individualGame.getWinningPlayer().name, wins);
      } else {
        // first win
        winsByPlayer.put(individualGame.getWinningPlayer().name, 1);
      }
    }
    LOGGER.info(Arrays.toString(winsByPlayer.entrySet().toArray()));
  }
}
