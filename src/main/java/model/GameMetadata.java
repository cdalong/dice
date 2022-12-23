package model;

import lombok.Builder;
import lombok.Data;
import player.Player;
import player.PlayerType;

import java.util.List;

@Data
@Builder
public class GameMetadata {
    int totalTurns;

    List<Player> players;

    Player winningPlayer;

    PlayerType winningPlayerType;

    int winningPlayerAverageRollScore;

}
