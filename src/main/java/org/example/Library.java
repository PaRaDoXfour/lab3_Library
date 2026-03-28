package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Клас Library представляє бібліотеку книг.
 * Містить колекцію книг з інформацією про їх кількість.
 */
public class Library {
    private static Logger log = LoggerFactory.getLogger(Library.class);
    private final Map<Book, Integer> bookInventory;
    private final Map<UUID, Book> bookByIdIndex;
    private final List<LoanRecord> loanRecords;
    private UUID id;
    private String name;
    private String address;

    /**
     * Конструктор для створення бібліотеки з назвою та адресою.
     *
     * @param name    Назва бібліотеки
     * @param address Адреса бібліотеки
     */
    public Library(String name, String address) throws LibraryNameException, LibraryAddressException {
        this.id = UUID.randomUUID();
        setName(name);
        setAddress(address);
        this.bookInventory = new HashMap<>();
        this.bookByIdIndex = new HashMap<>();
        this.loanRecords = new ArrayList<>();
    }

    /**
     * Видає книгу читачеві.
     *
     * @param book         книга для видачі
     * @param borrowerName ім'я позичальника
     * @param loanDate     дата видачі
     * @param returnDate   очікувана дата повернення
     * @return true, якщо книга успішно видана
     * @throws BookNotFoundException якщо книги немає в наявності
     */
    public boolean loanBook(Book book, String borrowerName, LocalDate loanDate, LocalDate returnDate)
            throws BookNotFoundException {
        // Перевіряємо, чи книга існує в бібліотеці
        Book foundBook = bookInventory.keySet().stream()
                .filter(b -> b.equals(book))
                .findFirst()
                .orElseThrow(() -> new BookNotFoundException("Книга не знайдена в бібліотеці"));

        // Перевіряємо наявність примірників
        if (bookInventory.get(foundBook) <= 0) {
            throw new BookNotFoundException("Немає доступних примірників цієї книги");
        }

        // Зменшуємо кількість доступних книг
        bookInventory.put(foundBook, bookInventory.get(foundBook) - 1);

        // Створюємо копію книги для запису
        Book bookCopy = createBookCopy(foundBook);
        if (bookCopy == null) {
            throw new BookNotFoundException("Помилка при створенні копії книги");
        }

        // Додаємо запис про видачу
        LoanRecord record = new LoanRecord(bookCopy, borrowerName, loanDate, returnDate);
        loanRecords.add(record);

        return true;
    }

    /**
     * Повертає книгу до бібліотеки.
     *
     * @param recordId         ID запису про видачу
     * @param actualReturnDate фактична дата повернення
     * @return true, якщо книга успішно повернена
     * @throws BookNotFoundException якщо запис про видачу не знайдено
     */
    public boolean returnBook(UUID recordId, LocalDate actualReturnDate) throws BookNotFoundException {
        // Знаходимо запис про видачу
        LoanRecord record = loanRecords.stream()
                .filter(r -> r.getRecordId().equals(recordId))
                .findFirst()
                .orElseThrow(() -> new BookNotFoundException("Запис про видачу не знайдено"));

        // Перевіряємо, чи книга вже повернена
        if (record.getReturnDate() != null) {
            throw new BookNotFoundException("Ця книга вже повернена");
        }

        // Оновлюємо запис про видачу
        record.setReturnDate(actualReturnDate);

        // Знаходимо оригінальну книгу в бібліотеці
        Book originalBook = bookInventory.keySet().stream()
                .filter(b -> b.equals(record.getBook()))
                .findFirst()
                .orElseThrow(() -> new BookNotFoundException("Книга не знайдена в бібліотеці"));

        // Збільшуємо кількість доступних книг
        bookInventory.put(originalBook, bookInventory.getOrDefault(originalBook, 0) + 1);

        return true;
    }

    /**
     * Отримує список активних видач (книги, які ще не повернуто).
     *
     * @return список активних видач
     */
    public List<LoanRecord> getActiveLoans() {
        return loanRecords.stream()
                .filter(record -> record.getReturnDate() == null)
                .collect(Collectors.toList());
    }

    /**
     * Пошук книги за UUID.
     *
     * @param uuid UUID книги для пошуку
     * @return Знайдена книга або null, якщо не знайдено
     */
    public Book searchByUUID(UUID uuid) {
        return bookByIdIndex.get(uuid);
    }

    /**
     * Отримує унікальний ідентифікатор бібліотеки.
     *
     * @return UUID бібліотеки
     */
    public UUID getId() {
        return id;
    }

    /**
     * Встановлює унікальний ідентифікатор.
     *
     * @param id унікальний ідентифікатор
     */
    public void setId(UUID id) throws InvalidDataException {
        if (id == null) {
            throw new InvalidDataException("ID не може бути null");
        }
        this.id = id;
    }

    /**
     * Видаляє книгу з бібліотеки.
     *
     * @param bookToDelete Книга для видалення
     * @return true, якщо книга була успішно видалена, false якщо ні
     */
    public boolean delete(Book bookToDelete) {
        if (bookInventory.keySet().remove(bookToDelete)) {
            bookByIdIndex.remove(bookToDelete.getId());
            return true;
        }
        return false;
    }

    /**
     * Оновлює існуючу книгу новими даними.
     *
     * @param existingObject Існуюча книга для оновлення
     * @param newObject      Нова версія книги
     * @return true, якщо оновлення пройшло успішно, false якщо ні
     */
    public boolean update(Book existingObject, Book newObject) {
        if (existingObject == null || newObject == null) {
            return false;
        }

        // Перевіряємо, чи книга існує в бібліотеці
        if (!bookInventory.containsKey(existingObject)) {
            return false;
        }

        // Отримуємо кількість копій існуючої книги
        int quantity = bookInventory.get(existingObject);

        // Видаляємо стару книгу
        bookInventory.remove(existingObject);
        if (existingObject.getId() != null) {
            bookByIdIndex.remove(existingObject.getId());
        }

        // Додаємо оновлену книгу з тією ж кількістю
        Book newCopy = createBookCopy(newObject);
        bookInventory.put(newCopy, quantity);
        bookByIdIndex.put(newCopy.getId(), newCopy);

        return true;
    }

    /**
     * Додає нову книгу до бібліотеки або збільшує кількість книг що існують.
     *
     * @param book     Книга для додавання
     * @param quantity Кількість книг
     * @return true, якщо книга успішно додана, false якщо ні
     */
    public boolean addNewBook(Book book, int quantity) {
        if (book == null || quantity <= 0) {
            return false;
        }

        // Якщо книга не знайдена, додаємо нову копію
        Book bookCopy = createBookCopy(book);
        if (bookCopy == null) return false;

        // Шукаємо книгу з такими ж полями (крім UUID)
        for (Map.Entry<Book, Integer> entry : bookInventory.entrySet()) {
            Book existingBook = entry.getKey();
            if (booksAreEqualIgnoringId(existingBook, bookCopy)) {
                bookInventory.put(existingBook, entry.getValue() + quantity);
                return true;
            }
        }


        bookInventory.put(bookCopy, quantity);
        bookByIdIndex.put(bookCopy.getId(), bookCopy);
        return true;
    }

    /**
     * Метод для перевірки чи однакові книги не порівнюючи ID.
     *
     * @param book1 Книга з бібліотеки
     * @param book2 Книга яку створюємо/додаємо
     * @return true true якщо однакові, false якщо різні
     */
    private boolean booksAreEqualIgnoringId(Book book1, Book book2) {
        if (book1 == book2) return true;
        if (book1 == null || book2 == null) return false;
        if (book1.getClass() != book2.getClass()) return false;

        // Порівнюємо всі поля крім ID
        return Objects.equals(book1.getTitle(), book2.getTitle()) &&
                Objects.equals(book1.getAuthor(), book2.getAuthor()) &&
                book1.getYear() == book2.getYear() &&
                Objects.equals(book1.getIsbn(), book2.getIsbn()) &&
                book1.getPages() == book2.getPages() &&
                book1.getGenre() == book2.getGenre() &&
                compareAdditionalFields(book1, book2);
    }

    /**
     * Метод для порівняння спеціальних полів книг.
     *
     * @param book1 книга з бібліотеки
     * @param book2 книга яку створюємо/додаємо
     * @return true якщо однакові, false якщо різні
     */
    private boolean compareAdditionalFields(Book book1, Book book2) {
        if (book1 instanceof EBook eBook1 && book2 instanceof EBook eBook2) {
            return eBook1.getFileFormat() == eBook2.getFileFormat() &&
                    Double.compare(eBook1.getFileSize(), eBook2.getFileSize()) == 0;
        } else if (book1 instanceof PaperBook paperBook1 && book2 instanceof PaperBook paperBook2) {
            if (book1 instanceof RareBook rareBook1 && book2 instanceof RareBook rareBook2) {
                return rareBook1.getEstimatedValue() == rareBook2.getEstimatedValue() &&
                        rareBook1.getFirstPrintYear().equals(rareBook2.getFirstPrintYear()) &&
                        paperBook1.getHardcover() == paperBook2.getHardcover();
            }
            return paperBook1.getHardcover() == paperBook2.getHardcover();
        } else if (book1 instanceof Audiobook audiobook1 && book2 instanceof Audiobook audiobook2) {
            return Double.compare(audiobook1.getDuration(), audiobook2.getDuration()) == 0 &&
                    Objects.equals(audiobook1.getNarrator(), audiobook2.getNarrator());
        }
        return false;
    }

    /**
     * Метод для створення глибокої копії книги.
     *
     * @param original Книга для копіювання
     * @return Копія книги
     */

    public Book createBookCopy(Book original) {
        if (original == null) {
            return null;
        }

        try {
            return switch (original) {
                case RareBook rareBook -> new RareBook(rareBook);
                case EBook eBook -> new EBook(eBook);
                case Audiobook audiobook -> new Audiobook(audiobook);
                case PaperBook paperBook -> new PaperBook(paperBook);
                default -> throw new IllegalArgumentException("Невідомий тип книги: " + original.getClass().getName());
            };
        } catch (InvalidDataException | BookException e) {
            String errorId = UUID.randomUUID().toString();
            System.out.println("Виникла помилка. Зверніться до підтримки та вкажіть ID: " + errorId);
            log.error("[ErrorID: {}] - Критична помилка при створенні копії книги. Контекст: operation='createBookCopy', original='{}', bookClass='{}'.",
                    errorId, original, original.getClass().getName(), e);
        }

        return original;
    }

    /**
     * Пошук книг за назвою або частиною назви.
     *
     * @param titlePart Частина назви для пошуку
     * @return Список копій знайдених книг з кількістю
     */
    public Map<Book, Integer> searchByTitle(String titlePart) {
        Map<Book, Integer> foundBooks = new HashMap<>();
        if (titlePart == null) return foundBooks;

        String searchTerm = titlePart.toLowerCase();

        for (Map.Entry<Book, Integer> entry : bookInventory.entrySet()) {
            if (entry.getKey().getTitle().toLowerCase().contains(searchTerm)) {
                foundBooks.put(createBookCopy(entry.getKey()), entry.getValue());
            }
        }
        return foundBooks;
    }

    /**
     * Пошук книг за автором або частиною імені автора.
     *
     * @param authorPart Частина імені автора для пошуку
     * @return Список копій знайдених книг з кількістю
     */
    public Map<Book, Integer> searchByAuthor(String authorPart) {
        Map<Book, Integer> foundBooks = new HashMap<>();
        if (authorPart == null) return foundBooks;

        String searchTerm = authorPart.toLowerCase();

        for (Map.Entry<Book, Integer> entry : bookInventory.entrySet()) {
            if (entry.getKey().getAuthor().toLowerCase().contains(searchTerm)) {
                foundBooks.put(createBookCopy(entry.getKey()), entry.getValue());
            }
        }
        return foundBooks;
    }

    /**
     * Пошук книг за жанром.
     *
     * @param genre Жанр для пошуку
     * @return Список знайдених книг з кількістю
     */
    public Map<Book, Integer> searchByGenre(Genre genre) {
        Map<Book, Integer> foundBooks = new HashMap<>();

        for (Map.Entry<Book, Integer> entry : bookInventory.entrySet()) {
            if (entry.getKey().getGenre() == genre) {
                foundBooks.put(createBookCopy(entry.getKey()), entry.getValue());
            }
        }
        return foundBooks;
    }

    /**
     * Повертає інформацію про всі книги в бібліотеці.
     *
     * @return Список всіх книг з кількістю
     */
    public Map<Book, Integer> getAllBooks() {
        Map<Book, Integer> booksCopy = new HashMap<>();
        for (Map.Entry<Book, Integer> entry : bookInventory.entrySet()) {
            booksCopy.put(createBookCopy(entry.getKey()), entry.getValue());
        }

        return booksCopy;
    }

    /**
     * Повертає відсортований список всіх книг у бібліотеці разом з їх кількістю.
     *
     * @param comparator компаратор, який визначає основний критерій сортування книг
     * @return відсортований список
     */
    public Map<Book, Integer> getAllBooksSorted(Comparator<Book> comparator) {
        Map<Book, Integer> booksCopy = new TreeMap<>((b1, b2) -> {
            int result = comparator.compare(b1, b2);
            // Якщо книги однакові за основним критерієм, порівнюємо за іншими полями
            return result != 0 ? result : b1.compareTo(b2);
        });

        booksCopy.putAll(bookInventory);
        return booksCopy;
    }

    /**
     * Отримує назву бібліотеки.
     *
     * @return Назва бібліотеки
     */
    public String getName() {
        return name;
    }

    /**
     * Встановлює назву бібліотеки.
     *
     * @param name Назва бібліотеки
     */
    public void setName(String name) throws LibraryNameException {
        if (name == null || name.isBlank()) {
            throw new LibraryNameException("Назва бібліотеки не може бути порожньою.");
        }
        this.name = name;
    }

    /**
     * Отримує адрес бібліотеки.
     *
     * @return Адрес бібліотеки
     */
    public String getAddress() {
        return address;
    }

    /**
     * Встановлює адрес бібліотеки.
     *
     * @param address Адрес бібліотеки
     */
    public void setAddress(String address) throws LibraryAddressException {
        if (address == null || address.trim().isEmpty()) {
            throw new LibraryAddressException("Адрес бібліотеки не може бути порожнім");
        }
        this.address = address;
    }

    /**
     * Генерує унікальний хеш-код для об'єкта.
     *
     * @return Числове значення хешу.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name, address, bookInventory);
    }

    /**
     * Перевизначений метод equals для порівняння бібліотек.
     *
     * @param o інший об'єкт
     * @return true, якщо книги мають однакові поля, інакше false
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Library library = (Library) o;
        return Objects.equals(id, library.id) &&
                Objects.equals(name, library.name) &&
                Objects.equals(address, library.address);
    }
}