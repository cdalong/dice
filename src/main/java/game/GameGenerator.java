package game;

import java.util.ArrayList;
import java.util.List;
import player.Player;
import player.PlayerType;

public class GameGenerator {

  private static final org.apache.log4j.Logger LOGGER =
      org.apache.log4j.Logger.getLogger(GameGenerator.class.getName());

  public static void main(String[] args) {

    List<Player> gamePlayers = new ArrayList<>();
    Player cameron = new Player(PlayerType.PLAYER_TYPE.AGGRESSIVE, 500, 3);
    cameron.setName("Cameron");
    Player fisk = new Player(PlayerType.PLAYER_TYPE.AGGRESSIVE, 200, 1);
    fisk.setName("Fisk");
    gamePlayers.add(cameron);
    gamePlayers.add(fisk);
  }
}
