import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

class PlayerTest {

  @org.junit.jupiter.api.Test
  void TestRoll() {}

  @org.junit.jupiter.api.Test
  void TestCheckStraight() {
    Player player = new Player(PlayerType.PLAYER_TYPE.AGGRESSIVE, 0, 0);
    HashMap<Integer, Integer> straightMap;
    HashMap<Integer, Integer> notStraightMap;
    List<Integer> straight = Arrays.asList(1, 2, 3, 4, 5, 6);
    List<Integer> notStraight = Arrays.asList(1, 2, 2, 3, 4, 5);

    straightMap = player.createHashMap(straight);
    notStraightMap = player.createHashMap(notStraight);
    boolean definitelyTrue = player.checkStraight(straightMap);
    boolean definitelyFalse = player.checkStraight(notStraightMap);

    Assertions.assertTrue(definitelyTrue);
    Assertions.assertFalse(definitelyFalse);
  }

  @org.junit.jupiter.api.Test
  void TestDecideScore() {
    Player player = new Player(PlayerType.PLAYER_TYPE.AGGRESSIVE, 0, 0);
    List<Integer> notStraight = Arrays.asList(1, 2, 2, 3, 4, 5);
    List<Integer> triples = Arrays.asList(1, 2, 3, 3, 3, 4);
    List<Integer> tripleOnes = Arrays.asList(1, 1, 1, 4, 4, 3);
    List<Integer> twoTriples = Arrays.asList(1, 1, 1, 4, 4, 4);
    List<Integer> quads = Arrays.asList(1, 1, 1, 1, 2, 3);
    List<Integer> quints = Arrays.asList(1, 1, 1, 1, 1, 6);
    List<Integer> quads2 = Arrays.asList(6, 6, 6, 6, 2, 2);
    List<Integer> quads3 = Arrays.asList(6, 6, 6, 6, 1, 1);
    ImmutablePair<Integer, Integer> scorePair;

    scorePair = player.decideScore(notStraight);
    Assertions.assertEquals(150, (int) scorePair.left);
    Assertions.assertEquals(4, (int) scorePair.right);
    scorePair = player.decideScore(triples);
    Assertions.assertEquals(400, (int) scorePair.left);
    Assertions.assertEquals(2, (int) scorePair.right);
    scorePair = player.decideScore(tripleOnes);
    Assertions.assertEquals(1000, (int) scorePair.left);
    Assertions.assertEquals(3, (int) scorePair.right);
    scorePair = player.decideScore(twoTriples);
    Assertions.assertEquals(1400, (int) scorePair.left);
    Assertions.assertEquals(6, (int) scorePair.right);
    scorePair = player.decideScore(quads);
    Assertions.assertEquals(2000, (int) scorePair.left);
    Assertions.assertEquals(2, (int) scorePair.right);
    scorePair = player.decideScore(quints);
    Assertions.assertEquals(4000, (int) scorePair.left);
    Assertions.assertEquals(1, (int) scorePair.right);
    scorePair = player.decideScore(quads2);
    Assertions.assertEquals(1200, (int) scorePair.left);
    Assertions.assertEquals(2, (int) scorePair.right);
    scorePair = player.decideScore(quads3);
    Assertions.assertEquals(1400, (int) scorePair.left);
    Assertions.assertEquals(6, (int) scorePair.right);
  }

  @org.junit.jupiter.api.Test
  void TestDecideScoreWithLessDice()
  {
    Player player = new Player(PlayerType.PLAYER_TYPE.AGGRESSIVE, 0, 0);
    List<Integer> fiveDice = Arrays.asList(1,2,3,4,5);
    List<Integer> fourDice = Arrays.asList(1,2,3,4);
    List<Integer> threeDice = Arrays.asList(1,2,3);
    List<Integer> twoDice = Arrays.asList(1,2);
    List<Integer> oneDice = List.of(1);

    ImmutablePair<Integer, Integer> scorePair;
    scorePair = player.decideScore(fiveDice);
    Assertions.assertEquals(scorePair.left, 150);
    Assertions.assertEquals(scorePair.right, 3);
    scorePair = player.decideScore(fourDice);
    Assertions.assertEquals(scorePair.left, 100);
    Assertions.assertEquals(scorePair.right, 3);
    scorePair = player.decideScore(threeDice);
    Assertions.assertEquals(scorePair.left, 100);
    Assertions.assertEquals(scorePair.right, 2);
    scorePair = player.decideScore(twoDice);
    Assertions.assertEquals(scorePair.left, 100);
    Assertions.assertEquals(scorePair.right, 1);
    scorePair = player.decideScore(oneDice);
    Assertions.assertEquals(scorePair.left, 100);
    Assertions.assertEquals(scorePair.right, 6);


  }
}
