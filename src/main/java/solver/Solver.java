package solver;

import config.Config;
import config.ConfigLoader;
import io.IOManager;
import io.IOUtils;
import log.CustomLogger;
import org.slf4j.event.Level;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Solver {
    private static Logger log;
    private static Config config;

    /**
     * Do not change this method. Method Loads the config and initializes the IOManager.
     */
    public static void main(String[] args) {
        config = ConfigLoader.loadConfig("config.xml");

        CustomLogger.createLogger(config);
        log = CustomLogger.getLogger();

        IOManager ioManager = new IOManager(config);
        ioManager.initialize();
        ioManager.execute();
    }

    /**
     * The verification method to read content from a given {@code input} to verify whether a solution from a given {@code output} was correct.
     * This method is only applied if a visualizer is provided, which can verify correctness.
     * DO NOT CHANGE PARAMETERS OR RETURN TYPE
     */
    public static void verify(Scanner input, Scanner output, SolutionVerifier verifier) throws IOException {
        //use the level's own verify method if it has one
        String className = parseClassName(config.inputPath());
        try {
            Method method = Class.forName(className).getMethod("verify", Scanner.class, Scanner.class, SolutionVerifier.class);
            method.invoke(null, input, output, verifier);
            return;
        } catch(NoSuchMethodException | ClassNotFoundException e) {
            //fall through to the default below
        } catch(Exception e) {
            Throwable cause = e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null ? e.getCause() : e;
            log.log(java.util.logging.Level.SEVERE, "Could not verify with class: " + className, cause);
            return;
        }

        int maxRuns = input.nextInt();
        input.nextLine(); //skip linebreak

        for(int run = 0;run < maxRuns;run++) {
            String inputLine1 = input.nextLine();
            String inputLine2 = input.nextLine();

            String xPath = output.nextLine();
            String yPath = output.nextLine();
            output.nextLine();

            String inputLine = inputLine1 + "\n" + inputLine2;
            String paths = xPath + "\n" + yPath;

            verifier.checkValidity(run, inputLine, paths);
        }
    }


    /**
     * The solving method to read from an input file via {@code reader} and write the solution to an output file via {@code writer}.
     * This method is applied to all input files.
     * DO NOT CHANGE PARAMETERS OR RETURN TYPE
     */
    public static void solve(Scanner reader, FileWriter writer) throws IOException {
        String className = parseClassName(config.inputPath());

        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod("solve", Scanner.class, FileWriter.class);
            method.invoke(null, reader, writer);
        } catch(Exception e) {
            //unwrap the exception thrown inside the level class
            Throwable cause = e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null ? e.getCause() : e;
            log.log(java.util.logging.Level.SEVERE, "Could not execute class: " + className, cause);
        }
    }

    private static String parseClassName(String path) {
        String cleaned = path.replace("\\", "/");

        if (cleaned.startsWith("./")) {
            cleaned = cleaned.substring(2);
        }

        if (cleaned.startsWith("input/")) {
            cleaned = cleaned.substring("input/".length());
        }

        String packagePart = "";
        String classPart = cleaned;

        //separate package and class part
        int lastSlashIndex = cleaned.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            packagePart = cleaned.substring(0, lastSlashIndex).replace("/", ".");
            classPart = cleaned.substring(lastSlashIndex + 1);
        }

        //capitalize class name
        if (!classPart.isEmpty()) {
            classPart = Character.toUpperCase(classPart.charAt(0)) + classPart.substring(1);
        }

        if (!packagePart.isEmpty()) {
            return "solver." + packagePart + "." + classPart;
        } else {
            return "solver." + classPart;
        }
    }
}
