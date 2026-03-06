package solver;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class SokoBot {
  /**
   * All goal positions
   */
  private static final Set<Integer> goalPos = new HashSet<>();

  /**
   * Global width (total # columns)
   */
  private static int width;

  private static class Node implements Comparable<Node> {
    /**
     * Manhattan heuristic
     * Hashed position of player
     */
    private final int h, playerPos;

    /**
     * Stores previous push directions
     */
    private final String moveHistory;

    /**
     * Hashed positions of all boxes
     */
    private final Set<Integer> boxPos;

    public Node(int playerPos, Set<Integer> boxPos, String moveHistory) {
      this.playerPos = playerPos;
      this.boxPos = boxPos;
      this.moveHistory = moveHistory;
      h = calculateHeuristic();
    }

    public String getMoveHistory() {
      return moveHistory;
    }

    private int calculateHeuristic() {
      int total = 0;

      // manhattan distance
      for (int b : boxPos) {
        int minDist = Integer.MAX_VALUE;

        for (int g : goalPos)
          minDist = Math.min(Math.abs(b / width - g / width) + Math.abs(b % width - g %
              width), minDist);

        total += minDist;
      }
      return total;
    }

    /**
     * Checks if all box positions matches goal positions
     *
     * @return boolean value
     */
    public boolean isGoal() {
      for (int b : boxPos)
        if (!goalPos.contains(b))
          return false;

      return true;
    }

    /**
     * Override to make it not compare references, but player and box position for
     * use in {@link HashSet}
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj instanceof Node node)
        return playerPos == node.playerPos && boxPos.equals(node.boxPos);

      return false;
    }

    /**
     * Override to make it not compare references, but player and box position for
     * use in {@link HashSet}
     */
    @Override
    public int hashCode() {
      return Objects.hash(playerPos, boxPos);
    }

    @Override
    public int compareTo(Node arg0) {
      return Integer.compare(h, arg0.h);
    }
  }

  // NOTE: row = hash / width, col = hash % width (unhashing)

  /**
   * Returns hashed position/s of all instances of the given key in the map
   * 
   * @param height Height
   * @param map    Map
   * @param key    Key
   * @return Hashed position/s
   */
  private static Set<Integer> hash(int height, char[][] map, char key) {
    Set<Integer> result = new HashSet<>();

    for (int i = 0; i < height; i++)
      for (int j = 0; j < width; j++)
        if (map[i][j] == key)
          result.add(i * width + j);

    return result;
  }

  /**
   * Returns path from {@link #startPos} towards the {@link #targetPos} to push
   * 
   * @param startPos  Start position
   * @param targetPos Target position
   * @param boxPos    Box position
   * @param wallPos   Wall position
   * @param width     Width
   * @param height    Height
   * @return Path
   */
  private static String getPushPath(int startPos, int targetPos, Set<Integer> boxPos, boolean[] wallPos,
      int height) {

    Queue<Integer> queue = new ArrayDeque<>();
    boolean[] visited = new boolean[width * height];
    Map<Integer, Integer> parentMap = new HashMap<>();

    queue.add(startPos);
    visited[startPos] = true;

    while (!queue.isEmpty()) {
      int pos = queue.poll();

      if (pos == targetPos) {
        StringBuilder result = new StringBuilder();
        int current = pos;

        while (parentMap.containsKey(current)) {
          int cRow = current / width, cCol = current % width, pRow = parentMap.get(current) / width,
              pCol = parentMap.get(current) % width; // unhash

          if (cRow == pRow - 1)
            result.append("u");
          else if (cRow == pRow + 1)
            result.append("d");
          else if (cCol == pCol - 1)
            result.append("l");
          else
            result.append("r");

          current = parentMap.get(current);
        }

        String path = result.reverse().toString();

        return path;
      }

      // for easy looping and position calculation
      int[] dRow = { -1, 1, 0, 0 }, dCol = { 0, 0, -1, 1 };

      for (int dir = 0; dir < 4; dir++) {
        int newRow = pos / width + dRow[dir], newCol = pos % width + dCol[dir], newPos = newRow * width + newCol;

        // check bounds, walls, box, and if it was visited already
        if (newRow < 0 || newRow >= height || newCol < 0 || newCol >= width || wallPos[newPos]
            || boxPos.contains(newPos) || visited[newPos])
          continue;

        // track parent
        parentMap.put(newPos, pos);
        queue.add(newPos);
        visited[newPos] = true;
      }
    }

    return null;
  }

  public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
    /*
     * YOU NEED TO REWRITE THE IMPLEMENTATION OF THIS METHOD TO MAKE THE BOT SMARTER
     */
    /*
     * Default stupid behavior: Think (sleep) for 3 seconds, and then return a
     * sequence
     * that just moves left and right repeatedly.
     */

    // hashed wall positions
    // hashed deadlock positions
    boolean[] wallPos = new boolean[width * height], deadlockPos = new boolean[width * height];

    // set global width
    SokoBot.width = width;

    for (int i : hash(height, mapData, '#'))
      wallPos[i] = true;
    // iterate through the mapData and track all (global) goal positions
    goalPos.addAll(hash(height, mapData, '.'));

    // iterate through mapData and track all deadlock positions
    for (int i = 0; i < height; i++)
      for (int j = 0; j < width; j++) {
        int pos = i * width + j; // hash

        if (wallPos[pos] || goalPos.contains(pos))
          continue;

        // corner deadlock
        boolean up = i > 0 && wallPos[(i - 1) * width + j], down = i < height - 1 && wallPos[(i + 1) * width + j],
            left = j > 0 && wallPos[i * width + (j - 1)], right = j < width - 1 && wallPos[i * width + (j + 1)];

        if ((up && left) || (up && right) || (down && left) || (down && right))
          deadlockPos[pos] = true;
      }

    // priority queue for searching states
    Queue<Node> queue = new PriorityQueue<>();
    // visited nodes
    Set<Node> visited = new HashSet<>();

    queue.add(new Node(hash(height, itemsData, '@').iterator().next(), hash(height, itemsData, '$'), ""));

    // used in node generation for easier looping and position calculation
    int[] dRow = { -1, 1, 0, 0 }, dCol = { 0, 0, -1, 1 };
    char[] dirChar = { 'u', 'd', 'l', 'r' };

    // search and node generation
    while (!queue.isEmpty()) {
      Node current = queue.poll();

      if (!visited.add(current))
        continue;

      if (current.isGoal())
        return current.getMoveHistory();

      // generate successors
      // for every box position in the current, it generates states where each box
      // moves in each direction, for all boxes and in all directions
      for (int b : current.boxPos) {
        int boxRow = b / width, boxCol = b % width; // unhash

        for (int dir = 0; dir < 4; dir++) {
          // target = tile of the box after push
          int targetRow = boxRow + dRow[dir], targetCol = boxCol + dCol[dir], targetPos = targetRow * width + targetCol; // hash

          // check bounds wall, deadlock or box
          if (targetRow < 0 || targetRow >= height || targetCol < 0 || targetCol >= width || wallPos[targetPos]
              || current.boxPos.contains(targetPos) || deadlockPos[targetPos] && !goalPos.contains(targetPos))
            continue;

          // calculate player position after push (where player is supposed to stand
          // during the push)
          int playerRow = boxRow - dRow[dir], playerCol = boxCol - dCol[dir], playerPos = playerRow * width + playerCol; // hash

          // check bounds wall, deadlock or box
          if (playerRow < 0 || playerRow >= height || playerCol < 0 || playerCol >= width || wallPos[playerPos]
              || current.boxPos.contains(playerPos))
            continue;

          // check if player can reach push position

          String path = getPushPath(current.playerPos, playerPos, current.boxPos, wallPos, height);

          if (path == null)
            continue;

          Set<Integer> newBoxPos = new HashSet<>(current.boxPos); // copy old arraylist

          newBoxPos.remove(b);
          newBoxPos.add(targetPos); // modifies 1 box

          Node next = new Node(b, newBoxPos, current.getMoveHistory() + path + dirChar[dir]);

          if (!visited.contains(next))
            queue.add(next);
        }
      }
    }

    // no sol
    return "";
  }

}
