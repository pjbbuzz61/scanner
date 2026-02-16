package collector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RemoveRtns {

	public static void main(String[] args) {
		
        Path inputFilePath = Paths.get("/home/pat/wagers/2026/espn/files/from_chrome_1.json"); // Replace with your input file path
        Path outputFilePath = Paths.get("/home/pat/wagers/2026/espn/files/chrome_file_modified.json"); // Replace with your desired output file path

        try {
            // 1. Read the entire file content into a String
            String content = new String(Files.readAllBytes(inputFilePath));

            // 2. Remove all carriage returns and line feeds using a regex
            // The regex "[\\r\\n]" matches any single carriage return or line feed character.
            String modifiedContent = content.replaceAll("[\\r\\n]", "")
            		.replaceAll(" ", " ").replaceAll(" ", " ").replaceAll(" ", " ").replaceAll(" ", " ");

            // You can also replace them with a space instead of removing them entirely:
            // String modifiedContent = content.replaceAll("[\\r\\n]", " ");

            // 3. Write the modified content back to the output file
            Files.writeString(outputFilePath, modifiedContent);

            System.out.println("Carriage returns and line feeds removed successfully!");

        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }

	}

}
