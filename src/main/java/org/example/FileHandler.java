package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Клас для роботи з файлами - читання та запис даних.
 */
public class FileHandler {
    private static Logger log = LoggerFactory.getLogger(FileHandler.class);
    // Ім'я файлу для зберігання даних
    private static final String FILE_NAME = "massive_input.txt";

    /**
     * Зчитує дані про бібліотеку з файлу.
     *
     * @return Об'єкт Library або null, якщо дані не знайдені
     * @throws IOException якщо виникають проблеми з читанням файлу
     *                     Формат файлу на початку:
     *                     [Library]
     *                     Назва бібліотеки
     *                     Адрес бібліотеки
     */
    public static Library readLibraryFromFile()
            throws IOException, InvalidDataException, LibraryNameException, LibraryAddressException {
        File file = new File(FILE_NAME);
        if (!file.exists() || file.length() == 0) {
            log.debug("Файл '{}' не існує або порожній: бібліотеку не завантажено.", FILE_NAME);
            return null;
        }

        log.debug("Зчитування даних бібліотеки з файлу '{}'.", FILE_NAME);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equals("[Library]")) {
                    String name = readNonNullLine(reader);
                    String address = readNonNullLine(reader);
                    return new Library(name, address);
                }
            }
        }
        return null;
    }

    /**
     * Зчитує список книг з файлу.
     *
     * @return ArrayList<Book> - список книг, зчитаних з файлу
     * @throws IOException якщо виникають проблеми з читанням файлу
     *                     Формат файлу:
     *                     Кожна книга зберігається у вигляді блоків даних:
     *                     [ТипКниги]
     *                     Назва
     *                     Автор
     *                     Рік
     *                     ISBN
     *                     Сторінки
     *                     Жанр
     *                     [Додаткові поля залежно від типу]
     *                     [Пустий рядок]
     */
    public static ArrayList<Book> readBooksFromFile() throws IOException {
        ArrayList<Book> books = new ArrayList<>();
        File file = new File(FILE_NAME);

        // Якщо файл не існує або порожній, повертаємо пустий список
        if (!file.exists() || file.length() == 0) {
            log.debug("Файл '{}' не існує або порожній: список книг порожній.", FILE_NAME);
            return books;
        }

        // Використовуємо try-with-resources для автоматичного закриття потоку
        log.debug("Зчитування книг з файлу '{}'.", FILE_NAME);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            // Читаємо файл рядок за рядком
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Пропускаємо порожні рядки та інформацію про бібліотеку
                if (line.isEmpty() || line.equals("[Library]")) {
                    continue; // Пропускаємо пусті рядки та блок бібліотеки
                }

                if (line.startsWith("[")) { // Початок блоку книги
                    Book book = readBookData(reader, line);
                    if (book != null) {
                        books.add(book);
                    }
                }
            }
        }
        log.debug("З файлу '{}' зчитано {} книг.", FILE_NAME, books.size());
        return books;
    }

    /**
     * Допоміжний метод для читання даних конкретної книги.
     *
     * @param reader BufferedReader для читання даних
     * @param type   тип книги (визначає формат даних)
     * @return об'єкт Book або null, якщо виникла помилка
     * @throws IOException якщо виникають проблеми з читанням даних
     */
    private static Book readBookData(BufferedReader reader, String type) throws IOException {
        try {
            // Читаємо спільні для всіх книг поля
            String title = readNonNullLine(reader);
            String author = readNonNullLine(reader);
            int year = Integer.parseInt(readNonNullLine(reader));
            String isbn = readNonNullLine(reader);
            int pages = Integer.parseInt(readNonNullLine(reader));
            Genre genre = Genre.valueOf(readNonNullLine(reader));

            // Залежно від типу книги читаємо додаткові поля
            switch (type) {
                case "[EBook]":
                    EBookFormat format = EBookFormat.valueOf(readNonNullLine(reader));
                    double size = Double.parseDouble(readNonNullLine(reader));
                    return new EBook(title, author, year, isbn, pages, genre, format, size);
                case "[PaperBook]":
                    boolean hardcover = Boolean.parseBoolean(readNonNullLine(reader));
                    return new PaperBook(title, author, year, isbn, pages, genre, hardcover);
                case "[Audiobook]":
                    double duration = Double.parseDouble(readNonNullLine(reader));
                    String narrator = readNonNullLine(reader);
                    return new Audiobook(title, author, year, isbn, pages, genre, duration, narrator);
                case "[RareBook]":
                    boolean rareHardcover = Boolean.parseBoolean(readNonNullLine(reader));
                    int value = Integer.parseInt(readNonNullLine(reader));
                    int firstPrintYear = Integer.parseInt(readNonNullLine(reader));
                    return new RareBook(title, author, year, isbn, pages, genre, rareHardcover, value, firstPrintYear);
                default:
                    String errorId = UUID.randomUUID().toString();
                    log.error(
                            "[ErrorID: {}] - Критична помилка при читанні типу книги. Контекст: operation='readBookData', file='{}', type='{}'.",
                            errorId, FILE_NAME, type);
                    return null;
            }
        } catch (Exception e) {
            String errorId = UUID.randomUUID().toString();
            log.error(
                    "[ErrorID: {}] - Критична помилка при парсингу книги. Контекст: operation='readBookData', file='{}', type='{}'.",
                    errorId, FILE_NAME, type, e);
            return null;
        }
    }

    /**
     * Допоміжний метод для читання рядка з перевіркою на null.
     *
     * @param reader BufferedReader для читання даних
     * @return рядок без зайвих пробілів
     * @throws IOException якщо досягнуто кінець файлу
     */
    private static String readNonNullLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Неочікуваний кінець файлу");
        }
        return line.trim();
    }

    /**
     * Зберігає список книг у файл.
     *
     * @param books список книг для збереження
     * @throws IOException якщо виникнуть проблеми з записом у файл
     *                     <p>
     *                     </p>
     *                     <br>
     *                     Формат запису:
     *                     Кожна книга зберігається у вигляді послідовності рядків:
     *                     [ТипКниги]
     *                     Назва
     *                     Автор
     *                     Рік
     *                     ISBN
     *                     Сторінки
     *                     Жанр
     *                     [Додаткові поля залежно від типу]
     *                     [Пустий рядок]
     */
    public static void saveBooksToFile(Library library, ArrayList<Book> books)
            throws IOException, InvalidDataException {
        log.debug("Збереження {} книг у файл '{}'.", books.size(), FILE_NAME);
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            // Спочатку зберігаємо дані бібліотеки
            writer.println("[Library]");
            writer.println(library.getName());
            writer.println(library.getAddress());
            writer.println(); // Пустий рядок для відокремлення

            for (Book book : books) {
                // Визначаємо тип книги та записуємо відповідні дані
                switch (book) {
                    case RareBook rareBook -> {
                        writer.println("[RareBook]");
                        writer.println(rareBook.getTitle());
                        writer.println(rareBook.getAuthor());
                        writer.println(rareBook.getYear());
                        writer.println(rareBook.getIsbn());
                        writer.println(rareBook.getPages());
                        writer.println(rareBook.getGenre());
                        writer.println(rareBook.getHardcover());
                        writer.println(rareBook.getEstimatedValue());
                        writer.println(rareBook.getFirstPrintYear().getValue());
                    }
                    case EBook eBook -> {
                        writer.println("[EBook]");
                        writer.println(eBook.getTitle());
                        writer.println(eBook.getAuthor());
                        writer.println(eBook.getYear());
                        writer.println(eBook.getIsbn());
                        writer.println(eBook.getPages());
                        writer.println(eBook.getGenre());
                        writer.println(eBook.getFileFormat());
                        writer.println(eBook.getFileSize());
                    }
                    case Audiobook audiobook -> {
                        writer.println("[Audiobook]");
                        writer.println(audiobook.getTitle());
                        writer.println(audiobook.getAuthor());
                        writer.println(audiobook.getYear());
                        writer.println(audiobook.getIsbn());
                        writer.println(audiobook.getPages());
                        writer.println(audiobook.getGenre());
                        writer.println(audiobook.getDuration());
                        writer.println(audiobook.getNarrator());
                    }
                    case PaperBook paperBook -> {
                        writer.println("[PaperBook]");
                        writer.println(paperBook.getTitle());
                        writer.println(paperBook.getAuthor());
                        writer.println(paperBook.getYear());
                        writer.println(paperBook.getIsbn());
                        writer.println(paperBook.getPages());
                        writer.println(paperBook.getGenre());
                        writer.println(paperBook.getHardcover());
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + book);
                }

                // Розділяємо книги пустими рядками
                writer.println();
            }
        }
        log.debug("Збереження у файл '{}' завершено успішно.", FILE_NAME);
    }
}
