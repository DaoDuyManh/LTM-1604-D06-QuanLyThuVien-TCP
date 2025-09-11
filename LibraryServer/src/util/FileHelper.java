package util;

import java.io.*;
import java.util.*;

public class FileHelper {
    public static List<String> readLines(String filename) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line.trim());
            }
        } catch (IOException e) {
            System.out.println("⚠️ Không thể đọc file: " + filename);
        }
        return lines;
    }

    public static void writeLines(String filename, List<String> lines) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            for (String line : lines) {
                pw.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendLine(String filename, String line) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename, true))) {
            pw.println(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
