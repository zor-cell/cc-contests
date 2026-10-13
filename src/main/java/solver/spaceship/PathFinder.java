package solver.spaceship;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Weighted A* search for the 2D spaceship, simulated tick by tick like the visualizer.
 * A node is (x, y, x-axis state, y-axis state), an axis state is (pace, remaining ticks of that pace).
 * Every edge is one tick. The heuristic is the maximum of the exact remaining time of each axis without asteroids
 * and the number of cells to the station around the asteroids (an axis moves at most one cell per tick).
 * Nodes that cannot reach the station within the time limit are dropped, so every path found respects the limit.
 */
public class PathFinder {
    private static final int MIN_PACE = 5;
    private static final int COLLISION_AREA = 2;
    //how far the ship may leave the bounding box of start, station and asteroids
    private static final int MARGIN = 10;
    //weight of the heuristic: 1 finds the fastest path, higher values find a slower path much quicker
    private static final int WEIGHT = 2;
    private static final int INF = Integer.MAX_VALUE / 2;

    private static final int STATE_COUNT = 31;
    private static final int[] PACE = new int[STATE_COUNT];
    private static final int[] REMAINING = new int[STATE_COUNT];
    //per axis state: the possible states one tick later and the position change of that tick
    private static final int[][] NEXT = new int[STATE_COUNT][];
    private static final int[][] MOVE = new int[STATE_COUNT][];
    private static final int STOPPED;

    //minTime[state][distance + range]: ticks until the axis stands still at the station
    private static int[][] minTime;
    private static int range = -1;

    static {
        int[][] index = new int[2 * MIN_PACE + 1][MIN_PACE];
        int count = 0;
        for(int pace = -MIN_PACE;pace <= MIN_PACE;pace++) {
            for(int remaining = 0;remaining < Math.max(1, Math.abs(pace));remaining++) {
                PACE[count] = pace;
                REMAINING[count] = remaining;
                index[pace + MIN_PACE][remaining] = count++;
            }
        }
        STOPPED = index[MIN_PACE][0];

        for(int state = 0;state < STATE_COUNT;state++) {
            //a running pace has to be finished, only afterwards a new one can be chosen
            int[] paces = REMAINING[state] > 0 ? new int[]{PACE[state]} : validPaces(PACE[state]);
            NEXT[state] = new int[paces.length];
            MOVE[state] = new int[paces.length];

            for(int i = 0;i < paces.length;i++) {
                int remaining = (REMAINING[state] > 0 ? REMAINING[state] : Math.max(1, Math.abs(paces[i]))) - 1;

                NEXT[state][i] = index[paces[i] + MIN_PACE][remaining];
                MOVE[state][i] = remaining == 0 ? Integer.signum(paces[i]) : 0;
            }
        }
    }

    private final Point goal;
    private final int limit;
    private final int minX;
    private final int minY;
    private final int width;
    private final int height;
    private final boolean[] blocked;
    //cells to the station around the blocked cells, moving in all 8 directions
    private final int[] cellDistance;

    //nodes
    private long[] nodeKey = new long[1 << 12];
    private int[] nodeParent = new int[1 << 12];
    private int[] nodeTime = new int[1 << 12];
    private int nodeCount = 0;

    //open addressing hash map: key -> node with the lowest time
    private long[] mapKeys = new long[1 << 13];
    private int[] mapNodes = new int[1 << 13];

    //open list: one stack of nodes per f value
    private final int[][] buckets;
    private final int[] bucketSizes;
    private int lowestBucket = 0;

    private PathFinder(Point goal, int limit, List<Point> asteroids) {
        this.goal = goal;
        this.limit = limit;

        int[] box = boundingBox(goal, asteroids);
        minX = box[0] - MARGIN;
        minY = box[1] - MARGIN;
        width = box[2] - box[0] + 2 * MARGIN + 1;
        height = box[3] - box[1] + 2 * MARGIN + 1;

        blocked = new boolean[width * height];
        for(var asteroid : asteroids) {
            for(int x = asteroid.x - COLLISION_AREA;x <= asteroid.x + COLLISION_AREA;x++) {
                for(int y = asteroid.y - COLLISION_AREA;y <= asteroid.y + COLLISION_AREA;y++) {
                    if(inside(x, y)) {
                        blocked[cell(x, y)] = true;
                    }
                }
            }
        }

        cellDistance = cellDistances();

        buckets = new int[WEIGHT * limit + 1][16];
        bucketSizes = new int[WEIGHT * limit + 1];
        Arrays.fill(mapKeys, -1);
    }

    /**
     * @return {minX, minY, maxX, maxY} of start, station and asteroids
     */
    private static int[] boundingBox(Point goal, List<Point> asteroids) {
        int[] box = {Math.min(0, goal.x), Math.min(0, goal.y), Math.max(0, goal.x), Math.max(0, goal.y)};
        for(var asteroid : asteroids) {
            box[0] = Math.min(box[0], asteroid.x);
            box[1] = Math.min(box[1], asteroid.y);
            box[2] = Math.max(box[2], asteroid.x);
            box[3] = Math.max(box[3], asteroid.y);
        }

        return box;
    }

    private int[] cellDistances() {
        int[] distances = new int[width * height];
        Arrays.fill(distances, INF);
        if(blocked[cell(goal.x, goal.y)]) {
            return distances;
        }

        int[] queue = new int[width * height];
        int head = 0;
        int tail = 0;
        queue[tail++] = cell(goal.x, goal.y);
        distances[queue[0]] = 0;

        while(head < tail) {
            int cell = queue[head++];
            int x = cell / height + minX;
            int y = cell % height + minY;

            for(int dx = -1;dx <= 1;dx++) {
                for(int dy = -1;dy <= 1;dy++) {
                    if(!inside(x + dx, y + dy)) {
                        continue;
                    }

                    int next = cell(x + dx, y + dy);
                    if(!blocked[next] && distances[next] == INF) {
                        distances[next] = distances[cell] + 1;
                        queue[tail++] = next;
                    }
                }
            }
        }

        return distances;
    }

    /**
     * @return a collision free path to {@code goal} within {@code limit}, or null if there is none
     */
    public static TotalPath findPath(Point goal, int limit, List<Point> asteroids) {
        int[] box = boundingBox(goal, asteroids);
        ensureMinTime(Math.max(box[2] - box[0], box[3] - box[1]) + 2 * MARGIN);

        return new PathFinder(goal, limit, asteroids).search();
    }

    private TotalPath search() {
        if(blocked[cell(0, 0)]) {
            return null;
        }

        //both sequences have to start with pace 0, which takes the first tick
        push(key(0, 0, STOPPED, STOPPED), -1, 1);

        while(lowestBucket < buckets.length) {
            if(bucketSizes[lowestBucket] == 0) {
                lowestBucket++;
                continue;
            }

            int node = buckets[lowestBucket][--bucketSizes[lowestBucket]];
            long key = nodeKey[node];
            if(mapNodes[find(key)] != node) {
                continue; //a faster way to this state was found later
            }

            int yState = (int) (key % STATE_COUNT);
            int xState = (int) (key / STATE_COUNT % STATE_COUNT);
            int cell = (int) (key / (STATE_COUNT * STATE_COUNT));
            int x = cell / height + minX;
            int y = cell % height + minY;

            if(x == goal.x && y == goal.y && xState == STOPPED && yState == STOPPED) {
                return buildPath(node);
            }

            for(int i = 0;i < NEXT[xState].length;i++) {
                int nextX = x + MOVE[xState][i];

                for(int j = 0;j < NEXT[yState].length;j++) {
                    int nextY = y + MOVE[yState][j];

                    if(inside(nextX, nextY) && !blocked[cell(nextX, nextY)]) {
                        push(key(nextX, nextY, NEXT[xState][i], NEXT[yState][j]), node, nodeTime[node] + 1);
                    }
                }
            }
        }

        return null;
    }

    private void push(long key, int parent, int time) {
        int yState = (int) (key % STATE_COUNT);
        int xState = (int) (key / STATE_COUNT % STATE_COUNT);
        int cell = (int) (key / (STATE_COUNT * STATE_COUNT));

        int xTime = minTime[xState][goal.x - (cell / height + minX) + range];
        int yTime = minTime[yState][goal.y - (cell % height + minY) + range];
        int remaining = Math.max(Math.max(xTime, yTime), cellDistance[cell]);
        if(time + remaining > limit) {
            return;
        }
        int f = time + WEIGHT * remaining;

        int slot = find(key);
        if(mapKeys[slot] == key && nodeTime[mapNodes[slot]] <= time) {
            return;
        }

        if(nodeCount == nodeKey.length) {
            nodeKey = Arrays.copyOf(nodeKey, nodeCount * 2);
            nodeParent = Arrays.copyOf(nodeParent, nodeCount * 2);
            nodeTime = Arrays.copyOf(nodeTime, nodeCount * 2);
        }
        nodeKey[nodeCount] = key;
        nodeParent[nodeCount] = parent;
        nodeTime[nodeCount] = time;

        mapKeys[slot] = key;
        mapNodes[slot] = nodeCount;

        if(bucketSizes[f] == buckets[f].length) {
            buckets[f] = Arrays.copyOf(buckets[f], bucketSizes[f] * 2);
        }
        buckets[f][bucketSizes[f]++] = nodeCount;
        lowestBucket = Math.min(lowestBucket, f);

        nodeCount++;
        if(nodeCount * 2 > mapKeys.length) {
            growMap();
        }
    }

    /**
     * @return the slot of {@code key} or the free slot where it belongs
     */
    private int find(long key) {
        int mask = mapKeys.length - 1;
        int slot = (int) ((key * 0x9E3779B97F4A7C15L) >>> 32) & mask;
        while(mapKeys[slot] != -1 && mapKeys[slot] != key) {
            slot = (slot + 1) & mask;
        }

        return slot;
    }

    private void growMap() {
        long[] oldKeys = mapKeys;
        int[] oldNodes = mapNodes;

        mapKeys = new long[oldKeys.length * 2];
        mapNodes = new int[oldKeys.length * 2];
        Arrays.fill(mapKeys, -1);

        for(int i = 0;i < oldKeys.length;i++) {
            if(oldKeys[i] != -1) {
                int slot = find(oldKeys[i]);
                mapKeys[slot] = oldKeys[i];
                mapNodes[slot] = oldNodes[i];
            }
        }
    }

    private TotalPath buildPath(int node) {
        //axis states per tick, from the first tick to the last
        var xStates = new ArrayList<Integer>();
        var yStates = new ArrayList<Integer>();
        for(int n = node;n != -1;n = nodeParent[n]) {
            xStates.add((int) (nodeKey[n] / STATE_COUNT % STATE_COUNT));
            yStates.add((int) (nodeKey[n] % STATE_COUNT));
        }
        Collections.reverse(xStates);
        Collections.reverse(yStates);

        return new TotalPath(toPaces(xStates), toPaces(yStates));
    }

    private static List<Integer> toPaces(List<Integer> states) {
        var paces = new ArrayList<Integer>();

        paces.add(0);
        for(int i = 1;i < states.size();i++) {
            //a new pace starts in the tick after the previous one was finished
            if(REMAINING[states.get(i - 1)] == 0) {
                paces.add(PACE[states.get(i)]);
            }
        }

        //waiting at the station is not needed, one final 0 is enough
        while(paces.size() > 1 && paces.getLast() == 0 && paces.get(paces.size() - 2) == 0) {
            paces.removeLast();
        }

        return paces;
    }

    private long key(int x, int y, int xState, int yState) {
        return ((long) cell(x, y) * STATE_COUNT + xState) * STATE_COUNT + yState;
    }

    private int cell(int x, int y) {
        return (x - minX) * height + (y - minY);
    }

    private boolean inside(int x, int y) {
        return x >= minX && x < minX + width && y >= minY && y < minY + height;
    }

    private static int[] validPaces(int pace) {
        if(pace == 0) {
            return new int[]{-MIN_PACE, 0, MIN_PACE};
        }

        int dir = Integer.signum(pace);
        int abs = Math.abs(pace);
        int slower = abs == MIN_PACE ? 0 : dir * (abs + 1);
        if(abs == 1) {
            return new int[]{pace, slower};
        }

        return new int[]{dir * (abs - 1), pace, slower};
    }

    /**
     * Calculates the 1D lower bounds for all distances in [-newRange, newRange].
     */
    private static void ensureMinTime(int newRange) {
        if(newRange <= range) {
            return;
        }

        range = newRange;
        minTime = new int[STATE_COUNT][2 * range + 1];
        for(var row : minTime) {
            Arrays.fill(row, INF);
        }
        minTime[STOPPED][range] = 0;

        boolean changed = true;
        while(changed) {
            changed = false;

            for(int state = 0;state < STATE_COUNT;state++) {
                for(int dist = -range;dist <= range;dist++) {
                    for(int i = 0;i < NEXT[state].length;i++) {
                        int nextDist = dist - MOVE[state][i];
                        if(Math.abs(nextDist) > range) {
                            continue;
                        }

                        int time = minTime[NEXT[state][i]][nextDist + range] + 1;
                        if(time < minTime[state][dist + range]) {
                            minTime[state][dist + range] = time;
                            changed = true;
                        }
                    }
                }
            }
        }
    }
}
