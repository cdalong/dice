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

  int highestTurnScore;

  // Additional statistics for analysis
  @Builder.Default
  int totalStealAttempts = 0;

  @Builder.Default
  int successfulSteals = 0;

  @Builder.Default
  int totalBusts = 0;

  @Builder.Default
  int totalHotDiceRolls = 0;
}