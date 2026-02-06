package solver.spaceship;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level3 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine();

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            int pos = reader.nextInt();
            int limit = reader.nextInt();
            reader.nextLine();

            int absPos = Math.abs(pos);
            int acc = absPos / 2;

            var paces = new ArrayList<Integer>();

            paces.add(0);
            if(absPos == 2) {
                paces.add((int) Math.signum(pos) * 5);
                paces.add((int) Math.signum(pos) * 5);
            } else {
                int pace = 0;
                int breakPoint = 0;
                for (int i = 0; i < absPos; i++) {
                    pace = accelerate(pace, (int) Math.signum(pos));
                    paces.add(pace);

                    breakPoint = absPos - (5 - Math.abs(pace));
                    if (i >= breakPoint - 1) {
                        break;
                    }
                }

                for (int i = breakPoint; i < absPos; i++) {
                    pace = decelerate(pace, (int) Math.signum(pos));
                    paces.add(pace);
                }
            }
            paces.add(0);

            for(var a : paces) {
                writer.write(a + " ");
            }
            writer.write("\n");
        }
    }

    private static int accelerate(int prevPace, int sign) {
        if(prevPace == 0) {
            return sign * 5;
        } else if(Math.abs(prevPace) == 1) {
            return sign * 1;
        }

        return prevPace + (sign * -1);
    }

    private static int decelerate(int prevPace, int sign) {
        int paceAbs = Math.abs(prevPace);

        if(prevPace == 0) {
            return 0;
        } else if(Math.abs(prevPace) == 5) {
            return 0;
        }

        return prevPace + (sign);
    }

    private static int paceToSpace(int pace) {
        return (int) Math.signum(pace);
    }

    private static int paceToTime(int pace) {
        if(pace == 0) return 1;

        return Math.abs(pace);
    }
}
