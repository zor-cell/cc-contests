package solver.spaceship;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level4 {
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

            var paces = new ArrayList<Integer>();

            paces.add(0);
            int pace = 0;
            while(absPos > 0) {
                int brakePath = pace == 0 ? 0 : 5 - pace + 1;

                if(brakePath == absPos) {
                    //nothing
                } else if(brakePath > absPos) {
                    pace = decelerate(pace);
                } else {
                    pace = accelerate(pace);
                }
                paces.add(pace);

                absPos--;
            }
            paces.add(0);

            for(var a : paces) {
                if(pos < 0) {
                    a *= -1;
                }

                writer.write(a + " ");
            }
            writer.write("\n");
        }
    }

    private static int accelerate(int prevPace) {
        if(prevPace == 0) {
            return 5;
        } else if(prevPace == 1) {
            return 1;
        }

        return prevPace - 1;
    }

    private static int decelerate(int prevPace) {
        if(prevPace == 0) {
            return 0;
        } else if(prevPace == 5) {
            return 0;
        }

        return prevPace + 1;
    }
}
