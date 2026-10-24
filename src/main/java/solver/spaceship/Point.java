package solver.spaceship;

public class Point {
    public int x;
    public int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(String p) {
        String[] splitPos = p.split(",");
        x = Integer.parseInt(splitPos[0]);
        y = Integer.parseInt(splitPos[1]);
    }

    public double norm() {
        return Math.sqrt(x * x + y * y);
    }

    public Point minus(Point p) {
        return new Point(x - p.x, y - p.y);
    }

    public double dist(Point other) {
        return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
    }
}
