package solver.nanofabricator;

import java.util.ArrayList;
import java.util.List;

/**
 * The nanofabricator: a board and 6 emitters on the border rail, emitter d starting inactive in its corner.
 */
public class Machine {
    public final Board board;
    public final int[][] ring;

    //ring index and state per emitter 1-6 (index 0 unused)
    public final int[] pos = new int[7];
    public final boolean[] active = new boolean[7];

    public Machine(int radius) {
        board = Board.empty(radius);
        ring = board.ring();
        for(int d = 1;d <= 6;d++) {
            pos[d] = (d - 1) * radius;
        }
    }

    /**
     * Executes one instruction line (6 instructions F/B/T/-) and creates/destroys matter.
     */
    public void tick(String[] line) {
        int n = ring.length;
        for(int d = 1;d <= 6;d++) {
            switch(line[d - 1]) {
                case "F" -> pos[d] = (pos[d] + 1) % n;
                case "B" -> pos[d] = (pos[d] - 1 + n) % n;
                case "T" -> active[d] = !active[d];
                default -> {}
            }
        }

        board.fireHalfPower(activeEmitters());
    }

    public List<int[]> activeEmitters() {
        List<int[]> emitters = new ArrayList<>();
        for(int d = 1;d <= 6;d++) {
            if(active[d]) emitters.add(new int[]{ring[pos[d]][0], ring[pos[d]][1], d});
        }
        return emitters;
    }
}
