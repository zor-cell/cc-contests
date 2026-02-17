package solver.spaceship;

import java.util.List;

public class TotalPath {
    List<Integer> x;
    List<Integer> y;

    public TotalPath(List<Integer> x, List<Integer> y) {
        this.x = x;
        this.y = y;
    }

    public boolean isValid() {
        return isValid(x) && isValid(y);
    }

    private boolean isValid(List<Integer> path) {
        if (path == null || path.isEmpty()) return false;

        // Rule: Path must start and end with 0 [cite: 83]
        if (path.get(0) != 0 || path.get(path.size() - 1) != 0) {
            return false;
        }

        for (int i = 0; i < path.size() - 1; i++) {
            int current = path.get(i);
            int next = path.get(i + 1);

            // Calculate abstract "Speed Levels" to simplify the check
            int currentLevel = getSpeedLevel(current);
            int nextLevel = getSpeedLevel(next);

            // The level can change by at most 1 (step up, step down, or stay same)
            if (Math.abs(currentLevel - nextLevel) > 1) {
                return false;
            }
        }
        return true;
    }

    // Maps the contest "Pace" values to a linear "Speed Level"
    // 1 -> Level 5 (Fastest forward)
    // ...
    // 5 -> Level 1 (Slowest forward)
    // 0 -> Level 0 (Stop)
    // -5 -> Level -1 (Slowest backward)
    // ...
    // -1 -> Level -5 (Fastest backward)
    private int getSpeedLevel(int pace) {
        if (pace == 0) return 0;
        if (pace > 0) return 6 - pace; // Maps 5->1, 4->2, ..., 1->5
        return -(6 - Math.abs(pace));  // Maps -5->-1, -4->-2, ..., -1->-5
    }
}
