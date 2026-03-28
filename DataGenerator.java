import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * A utility class to generate a massive input file for performance testing.
 * It will create 100,000 unique books in the format expected by the application.
 */
public class DataGenerator {

    private static final String[] GENRES = {
        "КОМПЮТЕРНА_ЛІТЕРАТУРА", "РОМАН", "ДЕТЕКТИВ", "ІСТОРИЧНИЙ", "ФЕНТЕЗІ", "ПОЕМА"
    };

    public static void main(String[] args) {
        String fileName = "massive_input.txt";
        int bookCount = 100000;
        Random random = new Random();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Write the library header
            writer.write("[Library]\n");
            writer.write("Massive Test Library\n");
            writer.write("123 Performance Ave.\n\n");

            // Generate 100,000 books
            for (int i = 1; i <= bookCount; i++) {
                writer.write("[PaperBook]\n");
                writer.write("Test Book Title " + i + "\n");
                writer.write("Test Author " + i + "\n");
                
                int year = 1900 + random.nextInt(124); // Year 1900-2023
                writer.write(year + "\n");
                
                // Unique ID (ISBN format)
                writer.write(String.format("978-%02d-%06d-%d\n", random.nextInt(100), i, random.nextInt(10))); 
                
                int pages = 50 + random.nextInt(950); // 50-999 pages
                writer.write(pages + "\n");
                
                String genre = GENRES[random.nextInt(GENRES.length)];
                writer.write(genre + "\n");
                
                // Random availability (true/false)
                writer.write(random.nextBoolean() + "\n"); 
                
                // Add an empty line between entries except for the last one
                if (i < bookCount) {
                    writer.write("\n");
                }

                // Progress update
                if (i % 10000 == 0) {
                    System.out.println("Generated " + i + " books...");
                }
            }

            System.out.println("Success! Finished generating " + bookCount + " books into: " + fileName);

        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
