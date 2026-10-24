package solver.nanofabricator;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level5 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = Integer.parseInt(Level4.nextNonEmpty(reader));

        for(int run = 0;run < maxRuns;run++) {
            Board pattern = Board.read(reader);
            int lines = Integer.parseInt(Level4.nextNonEmpty(reader));

            String[][] program = new String[lines][];
            for(int l = 0;l < lines;l++) {
                program[l] = Level4.nextNonEmpty(reader).split("\\s+");
            }

            writer.write(countSubmissions(pattern, program) + "\n");
        }
    }

    /**
     * Runs the program on an empty board of the pattern's radius and counts how often the pattern is submitted.
     */
    public static int countSubmissions(Board pattern, String[][] program) {
        List<int[]> cells = pattern.matter();
        Machine machine = new Machine(pattern.radius);

        int count = 0;
        for(String[] line : program) {
            machine.tick(line);
            if(machine.board.containsIsolated(cells)) {
                machine.board.clear(cells);
                count++;
            }
        }
        return count;
    }
}
