package player;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Player {

  public PlayerType.PLAYER_TYPE playerType;
  public boolean isOpen;
  public int rollThreshold;
  public int remainingDiceThreshold;
  public int score;
  public String name;
  private static final Logger LOGGER = Logger.getLogger(Player.class.getName());

  public Player(PlayerType.PLAYER_TYPE playerType, int rollThreshold, int remainingDiceThreshold) {
    this.playerType = playerType;
    this.rollThreshold = rollThreshold;
    this.remainingDiceThreshold = remainingDiceThreshold;
    this.score = 0;
    this.isOpen = false;
  }

  public List<Integer> roll(int activeDice) {
    List<Integer> dice = new ArrayList<>();
    for (int i = 0; i < activeDice; i++ )
    {
      dice.add((int) (Math.random() * 6 + 1));
    }
    return dice;
  }

  public boolean checkStraight(HashMap<Integer, Integer> diceMap) {
    for (int value : diceMap.values()) {
      // everything needs to be one
      if (value > 1 || value == 0) {
        return false;
      }
    }

    LOGGER.info(String.format("Player %s has hit a straight", this.getName()));
    return true;
  }

  public HashMap<Integer, Integer> createHashMap(List<Integer> rolledDice) {
    HashMap<Integer, Integer> map = new HashMap<>();
    for (int dice : rolledDice) {
      if (map.containsKey(dice)) {
        int newValue = map.get(dice);
        newValue++;
        map.put(dice, newValue);
      } else {
        // first count of dice roll
        map.put(dice, 1);
      }
    }
    return map;
  }

  public ImmutablePair<Integer, Integer> decideScore(List<Integer> rolledDice) {

    // based on player type
    // need to decide how many of each number
    // or if it's a straight
    // decide current score and remaining dice
    // if it's not open you have to keep rolling
    // player can bust if no one's, fives or three of a kinds are rolled
    int currentPendingScore = 0;
    int activeDice = rolledDice.size();
    HashMap<Integer, Integer> map;
    map = createHashMap(rolledDice);
    // check for straight first
    if (rolledDice.size() == 6) {
      if (checkStraight(map)) {
        return new ImmutablePair<>(1500, 6);
      }
    }
    for (int diceRoll = 1; diceRoll <= 6; diceRoll++) {
      if (map.containsKey(diceRoll)) {
        // amount of times that dice has rolled
        int curDiceVal = map.get(diceRoll);

        // calculate triples first
        if (curDiceVal >= 3 && diceRoll != 1) {
          if (curDiceVal == 3) {
            currentPendingScore += diceRoll * 100;
          } else {
            currentPendingScore += diceRoll * 100 * (2 * (curDiceVal - 3));
          }
          activeDice -= curDiceVal;
          LOGGER.info(String.format("Player %s rolled a Multiple Of: %s", this.getName(), diceRoll));
        }
        if (curDiceVal >= 3 && diceRoll == 1) {
          if (curDiceVal == 3) {
            currentPendingScore += diceRoll * 1000;
          } else {
            currentPendingScore += diceRoll * 1000 * (2 * (curDiceVal - 3));
          }
          activeDice -= curDiceVal;
          LOGGER.info(String.format("Player %s rolled a Multiple Of: %s", this.getName(), curDiceVal));
        } else if (diceRoll == 1) {
          currentPendingScore += curDiceVal * 100;
          activeDice -= curDiceVal;
        } else if (diceRoll == 5 && curDiceVal <= 2) {
          currentPendingScore += curDiceVal * 50;
          activeDice -= curDiceVal;
        }
        // should prioritize triples first, but requires some extra logic if there's only three twos
        // as usually players don't use 2's TODO
      }
    }

    if (activeDice == 0) {
      LOGGER.info(String.format("Player %s continues turn", this.getName()));
      activeDice = 6;
    }
    return new ImmutablePair<>(currentPendingScore, activeDice);
  }

  public boolean isOpen() {
    return isOpen;
  }

  public void setOpen(boolean open) {
    isOpen = open;
  }

  public int getRollThreshold() {
    return rollThreshold;
  }

  public void setRollThreshold(int rollThreshold) {
    this.rollThreshold = rollThreshold;
  }

  public int getRemainingDiceThreshold() {
    return remainingDiceThreshold;
  }

  public void setRemainingDiceThreshold(int remainingDiceThreshold) {
    this.remainingDiceThreshold = remainingDiceThreshold;
  }

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }
  public PlayerType.PLAYER_TYPE getPlayerType() {
    return playerType;
  }

  public void setPlayerType(PlayerType.PLAYER_TYPE playerType) {
    this.playerType = playerType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
