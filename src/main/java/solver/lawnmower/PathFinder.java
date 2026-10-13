package solver.lawnmower;

import java.util.Random;

/**
 * Finds a path that visits every free cell of a lawn exactly once (Hamiltonian path).
 * The path is grown from a random cell: an end is extended onto a free neighbor whenever possible, otherwise a
 * "backbite" move rewires the path at that end, which moves the end somewhere else without losing any cell.
 */
public class PathFinder {
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DY = {-1, 0, 1, 0};
    private static final char[] DIRECTIONS = {'W', 'D', 'S', 'A'};

    private final boolean[][] blocked;
    private final int width;
    private final int height;
    private final int freeCells;

    private int[] path; //cell ids (y * width + x) in visiting order
    private int[] index; //position of a cell in path, -1 if not visited
    private int length;

    public PathFinder(boolean[][] blocked, int width, int height) {
        this.blocked = blocked;
        this.width = width;
        this.height = height;

        int free = 0;
        for(int r = 0;r < height;r++) {
            for(int c = 0;c < width;c++) {
                if(!blocked[r][c]) free++;
            }
        }
        this.freeCells = free;
    }

    /**
     * Returns the path as a string of WDSA characters, or null if none was found within the given attempts.
     * If {@code closed} is set, the end cell of the path is a neighbor of its start cell.
     */
    public String find(long seed, int maxAttempts, boolean closed) {
        Random random = new Random(seed);
        long maxSteps = 2000L * freeCells + 1_000_000L;

        for(int attempt = 0;attempt < maxAttempts;attempt++) {
            if(grow(random, maxSteps, closed)) return toDirections();
        }

        return null;
    }

    private boolean grow(Random random, long maxSteps, boolean closed) {
        path = new int[freeCells];
        index = new int[width * height];
        java.util.Arrays.fill(index, -1);
        length = 0;

        int start;
        do {
            start = random.nextInt(width * height);
        } while(blocked[start / width][start % width]);
        path[length] = start;
        index[start] = length++;

        int[] candidates = new int[4];
        for(long step = 0;step < maxSteps && !isDone(closed);step++) {
            //work on the tail, so flip the path to work on the head
            if(random.nextBoolean()) reverse(0, length - 1);

            int end = path[length - 1];
            int x = end % width, y = end / width;

            int unvisited = 0, visited = 0;
            for(int d = 0;d < 4;d++) {
                int nx = x + DX[d], ny = y + DY[d];
                if(nx < 0 || nx >= width || ny < 0 || ny >= height || blocked[ny][nx]) continue;

                int cell = ny * width + nx;
                if(index[cell] == -1) {
                    //keep unvisited neighbors at the front
                    candidates[unvisited + visited] = candidates[unvisited];
                    candidates[unvisited++] = cell;
                } else if(index[cell] != length - 2) {
                    candidates[unvisited + visited++] = cell;
                }
            }

            if(unvisited > 0) {
                int cell = candidates[random.nextInt(unvisited)];
                path[length] = cell;
                index[cell] = length++;
            } else if(visited > 0) {
                //backbite: connect the end to a visited neighbor and cut the edge behind that neighbor
                int cell = candidates[unvisited + random.nextInt(visited)];
                reverse(index[cell] + 1, length - 1);
            }
        }

        return isDone(closed);
    }

    private boolean isDone(boolean closed) {
        if(length < freeCells) return false;
        if(!closed) return true;

        //a complete path keeps being rewired until both ends are next to each other
        int start = path[0], end = path[length - 1];
        return Math.abs(start % width - end % width) + Math.abs(start / width - end / width) == 1;
    }

    private void reverse(int from, int to) {
        while(from < to) {
            int tmp = path[from];
            path[from] = path[to];
            path[to] = tmp;
            index[path[from]] = from;
            index[path[to]] = to;
            from++;
            to--;
        }
    }

    private String toDirections() {
        StringBuilder sb = new StringBuilder(length);
        for(int i = 1;i < length;i++) {
            int dx = path[i] % width - path[i - 1] % width;
            int dy = path[i] / width - path[i - 1] / width;

            for(int d = 0;d < 4;d++) {
                if(DX[d] == dx && DY[d] == dy) sb.append(DIRECTIONS[d]);
            }
        }

        return sb.toString();
    }
}
