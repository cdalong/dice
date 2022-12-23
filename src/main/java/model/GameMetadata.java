package model;

import lombok.Builder;
import lombok.Data;
import player.Player;

@Data
@Builder
public class GameMetadata {
    int totalTurns;
    Player player;
}
