package solver.spaceship;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Level6 {
    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        //same task as level 5 with a shorter time limit, the path finder always stays within the limit
        Level5.solve(reader, writer);
    }
}
