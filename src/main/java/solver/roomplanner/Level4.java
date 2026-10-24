package solver.roomplanner;

import log.CustomLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

public class Level4 {
    private static final Logger log = CustomLogger.getLogger();

    private static final int DESK_LENGTH = 3;
    //a desk plus the free cells it needs to its right and below covers a 2x4 block, so desks are laid out on a grid
    //of 2x2 slots where every desk takes two neighbouring slots
    private static final int SLOT_SIZE = 2;

    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        int maxRuns = reader.nextInt();
        reader.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            log.fine("Run " + run);

            int x = reader.nextInt();
            int y = reader.nextInt();
            int desks = reader.nextInt();

            boolean[][] room = new boolean[y][x];
            int placed = placeDesks(room, x, y);

            if(placed != desks) {
                log.severe("Run " + run + ": placed " + placed + " desks in " + x + "x" + y + ", but " + desks + " are required");
            }

            //write result to file
            writer.write(format(room) + "\n");
        }
    }

    /**
     * Places the maximum number of non-touching desks in the room and returns how many were placed.
     */
    private static int placeDesks(boolean[][] room, int x, int y) {
        //the last slot of an odd side is only one cell wide, which is enough since walls may be touched
        int slotsX = (x + 1) / SLOT_SIZE;
        int slotsY = (y + 1) / SLOT_SIZE;

        int placed = 0;

        //vertical desks, each covering two slot rows
        int pairedSlotsY = slotsY - slotsY % 2;
        for(int slotY = 0;slotY < pairedSlotsY;slotY += 2) {
            for(int slotX = 0;slotX < slotsX;slotX++) {
                for(int i = 0;i < DESK_LENGTH;i++) {
                    room[slotY * SLOT_SIZE + i][slotX * SLOT_SIZE] = true;
                }
                placed++;
            }
        }

        //a leftover slot row gets horizontal desks, each covering two slot columns
        if(pairedSlotsY < slotsY) {
            for(int slotX = 0;slotX + 2 <= slotsX;slotX += 2) {
                for(int i = 0;i < DESK_LENGTH;i++) {
                    room[pairedSlotsY * SLOT_SIZE][slotX * SLOT_SIZE + i] = true;
                }
                placed++;
            }
        }

        return placed;
    }

    private static String format(boolean[][] room) {
        StringBuilder sb = new StringBuilder();
        for(boolean[] row : room) {
            for(boolean desk : row) {
                sb.append(desk ? 'X' : '.');
            }
            sb.append('\n');
        }

        return sb.toString();
    }
}
