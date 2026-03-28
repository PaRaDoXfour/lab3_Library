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
                    String name = reader.readLine();
                    if (name == null) throw new IOException("Unexpected end of file");
                    name = name.trim();

                    String address = reader.readLine();
                    if (address == null) throw new IOException("Unexpected end of file");
                    address = address.trim();

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
                if (line.isEmpty() || line.startsWith("[Library]") || !line.startsWith("[")) {
                    continue;
                }

                try {
                    Book book = readBookData(line);
                    if (book != null) {
                        // Validate ISBN format
                        if (!book.getIsbn().matches("978-\\d{2}-\\d{6}-\\d")) {
                            log.warn("Пропущено запис з недійсним форматом ISBN: {}", book.getIsbn());
                            continue;
                        }
                        books.add(book);
                    }
                } catch (Exception e) {
                    log.warn("Помилка парсингу рядка книги (пропущено): {}", line, e);
                }
            }
        }
        log.debug("З файлу '{}' зчитано {} книг.", FILE_NAME, books.size());
        return books;
    }

    /**
     * Допоміжний метод для читання даних конкретної книги з одного рядка.
     * Використовує StringTokenizer для оптимізації без регулярних виразів.
     *
     * @param rowData рядок з файлу
     * @return об'єкт Book
     * @throws Exception якщо виникають проблеми з читанням даних
     */
    private static Book readBookData(String rowData) throws Exception {
        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(rowData, "|");
        if (!tokenizer.hasMoreTokens()) return null;

        String type = tokenizer.nextToken().trim().intern();
        String title = tokenizer.nextToken().trim(); // Titles are generally unique, avoids bloating String pool
        String author = tokenizer.nextToken().trim().intern();
        int year = Integer.parseInt(tokenizer.nextToken().trim());
        String isbn = tokenizer.nextToken().trim();
        int pages = Integer.parseInt(tokenizer.nextToken().trim());
        Genre genre = Genre.valueOf(tokenizer.nextToken().trim());

        switch (type) {
            case "[EBook]":
                EBookFormat format = EBookFormat.valueOf(tokenizer.nextToken().trim());
                double size = Double.parseDouble(tokenizer.nextToken().trim());
                return new EBook(title, author, year, isbn, pages, genre, format, size);
            case "[PaperBook]":
                boolean hardcover = Boolean.parseBoolean(tokenizer.nextToken().trim());
                return new PaperBook(title, author, year, isbn, pages, genre, hardcover);
            case "[Audiobook]":
                double duration = Double.parseDouble(tokenizer.nextToken().trim());
                String narrator = tokenizer.nextToken().trim().intern();
                return new Audiobook(title, author, year, isbn, pages, genre, duration, narrator);
            case "[RareBook]":
                boolean rareHardcover = Boolean.parseBoolean(tokenizer.nextToken().trim());
                int value = Integer.parseInt(tokenizer.nextToken().trim());
                int firstPrintYear = Integer.parseInt(tokenizer.nextToken().trim());
                return new RareBook(title, author, year, isbn, pages, genre, rareHardcover, value, firstPrintYear);
            default:
                String errorId = UUID.randomUUID().toString();
                log.error(
                        "[ErrorID: {}] - Критична помилка при читанні типу книги. Контекст: operation='readBookData', file='{}', type='{}'.",
                        errorId, FILE_NAME, type);
                return null;
        }
    }

    /**
     * Зберігає список книг у файл.
     *
     * @param books список книг для збереження
     * @throws IOException якщо виникнуть проблеми з записом у файл
     *                     Формат запису:
     *                     Кожна книга зберігається у вигляді одного рядка розділеного пайпами '|'
     */
    public static void saveBooksToFile(Library library, ArrayList<Book> books)
            throws IOException, InvalidDataException {
        log.debug("Збереження {} книг у файл '{}'.", books.size(), FILE_NAME);
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(FILE_NAME)))) {
            // Спочатку зберігаємо дані бібліотеки
            writer.println("[Library]");
            writer.println(library.getName());
            writer.println(library.getAddress());
            writer.println(); // Пустий рядок для відокремлення

            for (Book book : books) {
                StringBuilder sb = new StringBuilder();
                if (book instanceof RareBook) {
                    RareBook rareBook = (RareBook) book;
                    sb.append("[RareBook]|").append(rareBook.getTitle()).append("|")
                      .append(rareBook.getAuthor()).append("|").append(rareBook.getYear()).append("|")
                      .append(rareBook.getIsbn()).append("|").append(rareBook.getPages()).append("|")
                      .append(rareBook.getGenre()).append("|").append(rareBook.getHardcover()).append("|")
                      .append(rareBook.getEstimatedValue()).append("|").append(rareBook.getFirstPrintYear().getValue());
                } else if (book instanceof EBook) {
                    EBook eBook = (EBook) book;
                    sb.append("[EBook]|").append(eBook.getTitle()).append("|")
                      .append(eBook.getAuthor()).append("|").append(eBook.getYear()).append("|")
                      .append(eBook.getIsbn()).append("|").append(eBook.getPages()).append("|")
                      .append(eBook.getGenre()).append("|").append(eBook.getFileFormat()).append("|")
                      .append(eBook.getFileSize());
                } else if (book instanceof Audiobook) {
                    Audiobook audiobook = (Audiobook) book;
                    sb.append("[Audiobook]|").append(audiobook.getTitle()).append("|")
                      .append(audiobook.getAuthor()).append("|").append(audiobook.getYear()).append("|")
                      .append(audiobook.getIsbn()).append("|").append(audiobook.getPages()).append("|")
                      .append(audiobook.getGenre()).append("|").append(audiobook.getDuration()).append("|")
                      .append(audiobook.getNarrator());
                } else if (book instanceof PaperBook) {
                    PaperBook paperBook = (PaperBook) book;
                    sb.append("[PaperBook]|").append(paperBook.getTitle()).append("|")
                      .append(paperBook.getAuthor()).append("|").append(paperBook.getYear()).append("|")
                      .append(paperBook.getIsbn()).append("|").append(paperBook.getPages()).append("|")
                      .append(paperBook.getGenre()).append("|").append(paperBook.getHardcover());
                } else {
                    throw new IllegalStateException("Unexpected value: " + book);
                }
                writer.println(sb.toString());
            }
        }
        log.debug("Збереження у файл '{}' завершено успішно.", FILE_NAME);
    }
}
