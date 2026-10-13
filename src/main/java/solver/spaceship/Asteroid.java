package solver.spaceship;

public class Asteroid {
    public int x;
    public int y;
    public int xPace;
    public int yPace;

    /**
     * @param a "x,y" for a stationary asteroid or "x,y,xPace,yPace" for a moving one
     */
    public Asteroid(String a) {
        String[] split = a.split(",");
        x = Integer.parseInt(split[0]);
        y = Integer.parseInt(split[1]);

        if(split.length == 4) {
            xPace = Integer.parseInt(split[2]);
            yPace = Integer.parseInt(split[3]);
        }
    }

    public boolean isMoving() {
        return xPace != 0 || yPace != 0;
    }

    //asteroids keep their pace from tick 0 on: one cell every |pace| ticks
    public int xAt(int time) {
        return xPace == 0 ? x : x + time / xPace;
    }

    public int yAt(int time) {
        return yPace == 0 ? y : y + time / yPace;
    }
}
