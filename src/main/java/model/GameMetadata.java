package model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import player.Player;
import player.PlayerType;

@Data
@Builder
public class GameMetadata {
  int totalTurns;

  List<Player> players;

  Player winningPlayer;

  PlayerType winningPlayerType;

  int winningPlayerAverageRollScore;
}
