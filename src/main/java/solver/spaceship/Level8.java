package solver.spaceship;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Level8 {
    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        //same format as level 7, the asteroids just have a pace now
        Level7.solve(reader, writer);
    }
}
