package solver.nanofabricator;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level4 {
    private static final Logger log = CustomLogger.getLogger();

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = Integer.parseInt(nextNonEmpty(reader));

        for(int run = 0;run < maxRuns;run++) {
            int radius = Integer.parseInt(nextNonEmpty(reader));
            int lines = Integer.parseInt(nextNonEmpty(reader));

            String[][] program = new String[lines][];
            for(int l = 0;l < lines;l++) {
                program[l] = nextNonEmpty(reader).split("\\s+");
            }

            Board board = simulate(radius, program);
            writer.write(board.toString());
            writer.write("\n");
        }
    }

    /**
     * Runs a program of instruction lines (6 instructions F/B/T/- each) on an empty board of the given radius.
     */
    public static Board simulate(int radius, String[][] program) {
        Machine machine = new Machine(radius);
        for(String[] line : program) {
            machine.tick(line);
        }
        return machine.board;
    }

    static String nextNonEmpty(Scanner reader) {
        String line = reader.nextLine().trim();
        while(line.isEmpty()) {
            line = reader.nextLine().trim();
        }
        return line;
    }
}
