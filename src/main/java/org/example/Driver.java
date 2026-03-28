package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Клас Driver керує взаємодією користувача з бібліотекою книг.
 * Дозволяє додавати нові книги різних типів, переглядати список книг, шукати
 * книги та завершувати програму.
 * Логування: DEBUG для допоміжних кроків меню/вводу/файлів, INFO для
 * запуску/завершення та ключових дій,
 * ERROR для винятків у критичних операціях.
 * Рівень логування змінюється без перекомпіляції через змінну Logback LOG_LEVEL
 * (змінна оточення або JVM параметр -DLOG_LEVEL=...).
 */
public class Driver {
    private static Logger log = LoggerFactory.getLogger(Driver.class);

    /**
     * Відображає локалізоване користувацьке повідомлення про помилку з
     * інструкціями.
     */
    private static void showUserFriendlyError(String errorKey, String errorId) {
        ResourceBundle messages = ResourceBundle.getBundle("messages", Locale.getDefault());
        String desc = messages.getString(errorKey + ".desc");
        String inst = messages.getString(errorKey + ".inst");
        String feedbackTemplate = messages.getString("error.feedback");
        String feedback = java.text.MessageFormat.format(feedbackTemplate, errorId);

        System.out.println("\n--- ПОМИЛКА ---");
        System.out.println(desc);
        System.out.println(inst);
        System.out.println(feedback);
        System.out.println("----------------\n");
    }

    /**
     * Основний метод програми. Виводить та обробляє вибір користувача.
     *
     * @param args Аргументи командного рядка (не використовуються).
     */
    public static void main(String[] args) {
        org.slf4j.MDC.put("user", "Admin");
        org.slf4j.MDC.put("sessionId", UUID.randomUUID().toString());

        log.debug("Детальна інформація про стан системи: старт main(), ініціалізація компонентів.");
        log.info("Запуск програми Library_project.");
        Scanner scanner = new Scanner(System.in);
        Library library;

        try {
            // Завантажуємо бібліотеку з файлу
            log.debug("Початок завантаження бібліотеки з файлу.");
            Library loadedLibrary = FileHandler.readLibraryFromFile();
            if (loadedLibrary != null) {
                library = loadedLibrary;
                System.out.println("Дані бібліотеки успішно завантажено з файлу.");
                System.out.println("Name: " + library.getName());
                System.out.println("Address: " + library.getAddress());
                log.info("Бібліотеку '{}' успішно завантажено з файлу.", library.getName());
            } else {
                // Якщо бібліотеки немає у файлі - створюємо нову
                System.out.println("Створення нової бібліотеки:");
                String name = readNonEmptyString(scanner, "Назва бібліотеки: ");
                String address = readNonEmptyString(scanner, "Адреса бібліотеки: ");
                library = new Library(name, address);
                log.info("Створено нову бібліотеку '{}'.", name);
            }

            // Завантажуємо книги з файлу
            log.debug("Початок завантаження книг з файлу.");
            long startTime = System.currentTimeMillis();
            ArrayList<Book> booksFromFile = FileHandler.readBooksFromFile();
            for (Book book : booksFromFile) {
                try {
                    library.addNewBook(book, 1); // Додаємо по одній книзі кожного типу
                } catch (Exception e) {
                    String errorId = UUID.randomUUID().toString();
                    showUserFriendlyError("error.import.book", errorId);
                    log.error(
                            "[ErrorID: {}] - Critical error during book import. Контекст: title='{}', operation='addNewBook', quantity={}.",
                            errorId, book.getTitle(), 1, e);
                }
            }
            long endTime = System.currentTimeMillis();
            System.out.println("[INFO] Loaded " + booksFromFile.size() + " books in " + (endTime - startTime) + " ms.");
            log.debug("Завантажено {} книг з файлу.", booksFromFile.size());
        } catch (LibraryNameException | LibraryAddressException e) {
            String errorId = UUID.randomUUID().toString();
            showUserFriendlyError("error.init.library", errorId);
            log.error(
                    "[ErrorID: {}] - Critical error during library initialization. Контекст: operation='main:initLibrary'.",
                    errorId, e);
            org.slf4j.MDC.clear();
            return;
        } catch (IOException e) {
            String errorId = UUID.randomUUID().toString();
            showUserFriendlyError("error.startup.read", errorId);
            log.error(
                    "[ErrorID: {}] - Critical error during startup data read. Контекст: operation='main:startupRead', inputFile='input.txt'.",
                    errorId, e);
            org.slf4j.MDC.clear();
            return;
        } catch (Exception e) {
            String errorId = UUID.randomUUID().toString();
            showUserFriendlyError("error.general", errorId);
            log.error(
                    "[ErrorID: {}] - Critical error during application startup. Контекст: operation='main:startup', args='{}'.",
                    errorId, Arrays.toString(args), e);
            org.slf4j.MDC.clear();
            return;
        }

        try {
            menu(scanner, library);
            log.debug("Початок збереження даних бібліотеки у файл.");
            FileHandler.saveBooksToFile(library, new ArrayList<>(library.getAllBooks().keySet()));
            System.out.println("Дані успішно збережено у файл.");
            log.info("Дані бібліотеки успішно збережено.");
        } catch (IOException e) {
            String errorId = UUID.randomUUID().toString();
            showUserFriendlyError("error.save.books", errorId);
            log.error(
                    "[ErrorID: {}] - Critical error during data save. Контекст: operation='main:saveBooks', library='{}'.",
                    errorId, library.getName(), e);
        } catch (Exception e) {
            String errorId = UUID.randomUUID().toString();
            showUserFriendlyError("error.general", errorId);
            log.error(
                    "[ErrorID: {}] - Critical error during shutdown processing. Контекст: operation='main:shutdown', library='{}'.",
                    errorId, library.getName(), e);
        } finally {
            scanner.close();
            log.info("Програму завершено.");
            org.slf4j.MDC.clear();
        }
    }

    /**
     * Виводить функції головного меню
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @param library Об'єкт Library, що містить колекцію книг
     */
    private static void menu(Scanner scanner, Library library) {
        int choice;
        do {
            System.out.println("\nГоловне меню бібліотеки '" + library.getName() + "':");
            System.out.println("1. Видача/Повернення книги.");
            System.out.println("2. Пошук книги");
            System.out.println("3. Додати нову книгу");
            System.out.println("4. Модифікувати книгу");
            System.out.println("5. Видалити книгу");
            System.out.println("6. Вивести інформацію про всі книги");
            System.out.println("7. Вивести відсортовану інформацію про всі книги");
            System.out.println("8. Завершити");
            System.out.print("Ваш вибір: ");

            String input = scanner.nextLine();
            try {
                choice = input.isEmpty() ? -1 : Integer.parseInt(input);
            } catch (NumberFormatException e) {
                choice = -1;
                log.debug("Некоректний формат вводу в головному меню: '{}'.", input);
            }

            log.debug("Обрано пункт головного меню: {}", choice);
            try {
                switch (choice) {
                    case 1:
                        showLoanMenu(scanner, library);
                        break;
                    case 2:
                        showSearchMenu(scanner, library);
                        break;
                    case 3:
                        showAddBookMenu(scanner, library);
                        break;
                    case 4:
                        showModifyBookMenu(scanner, library);
                        break;
                    case 5:
                        showDeleteBookMenu(scanner, library);
                        break;
                    case 6:
                        displayAllBooks(library);
                        break;
                    case 7:
                        displaySortedBooks(scanner, library);
                        break;
                    case 8:
                        System.out.println("Програма завершена.");
                        break;
                    default:
                        System.out.println("Невірний вибір, спробуйте ще раз.");
                }
            } catch (LibraryException e) {
                String errorId = UUID.randomUUID().toString();
                showUserFriendlyError("error.menu", errorId);
                log.error(
                        "[ErrorID: {}] - Critical error during main menu processing. Контекст: choice={}, library='{}'.",
                        errorId, choice, library.getName(), e);
            }
        } while (choice != 8);
    }

    /**
     * Відображає меню видачі/повернення книги та обробляє вибір користувача.
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @param library Об'єкт Library, що містить колекцію книг
     */
    private static void showLoanMenu(Scanner scanner, Library library) {
        int choice;
        do {
            System.out.println("\nМеню видачі/повернення книг:");
            System.out.println("1. Видати книгу");
            System.out.println("2. Повернути книгу");
            System.out.println("3. Переглянути активні видачі");
            System.out.println("4. Повернутись до головного меню");
            System.out.print("Ваш вибір: ");

            String input = scanner.nextLine();
            try {
                choice = input.isEmpty() ? -1 : Integer.parseInt(input);
            } catch (NumberFormatException e) {
                choice = -1;
                log.debug("Некоректний формат вводу в меню видачі/повернення: '{}'.", input);
            }

            log.debug("Обрано пункт меню видачі/повернення: {}", choice);
            try {
                switch (choice) {
                    case 1:
                        loanBook(scanner, library);
                        break;
                    case 2:
                        returnBook(scanner, library);
                        break;
                    case 3:
                        displayActiveLoans(library);
                        break;
                    case 4:
                        return;
                    default:
                        System.out.println("Невірний вибір, спробуйте ще раз.");
                }
            } catch (LibraryException e) {
                String errorId = UUID.randomUUID().toString();
                showUserFriendlyError("error.loan", errorId);
                log.error(
                        "[ErrorID: {}] - Critical error during loan/return operation. Контекст: choice={}, library='{}'.",
                        errorId, choice, library.getName(), e);
            }
        } while (choice != 4);
    }

    /**
     * Видає книгу з бібліотеки читачеві
     *
     * @param scanner Об'єкт для зчитування вводу користувача
     * @param library Бібліотека, з якої видається книга
     * @throws BookNotFoundException Якщо книга не знайдена або недоступна
     */
    private static void loanBook(Scanner scanner, Library library) throws BookNotFoundException {
        System.out.println("\nВидача книги:");

        // Отримуємо всі книги з бібліотеки
        Map<Book, Integer> allBooks = library.getAllBooks();
        if (allBooks.isEmpty()) {
            System.out.println("Бібліотека порожня. Немає книг для видачі.");
            return;
        }

        // Виводимо список книг для вибору
        System.out.println("Оберіть книгу для видачі:");
        List<Book> booksList = new ArrayList<>(allBooks.keySet());
        for (int i = 0; i < booksList.size(); i++) {
            Book book = booksList.get(i);
            System.out.println((i + 1) + ". " + book.getTitle() + " (Автор: " + book.getAuthor() +
                    ", Доступно: " + allBooks.get(book) + ")");
        }
        System.out.println((booksList.size() + 1) + ". Повернутись до попереднього меню");

        int bookChoice = readValidInt(scanner, "Введіть номер книги: ", 1, booksList.size() + 1);

        if (bookChoice == booksList.size() + 1) {
            return;
        }

        Book selectedBook = booksList.get(bookChoice - 1);

        // Запитуємо дані про позичальника
        String borrowerName = readNonEmptyString(scanner, "Ім'я позичальника: ");

        // Запитуємо дату видачі
        LocalDate loanDate = readDate(scanner, "Дата видачі (рррр-мм-дд або Enter, за замовчуванням сьогодні): ",
                LocalDate.now());

        // Запитуємо очікувану дату повернення
        LocalDate returnDate = readReturnDate(scanner);

        // Виконуємо видачу
        if (library.loanBook(selectedBook, borrowerName, loanDate, returnDate)) {
            System.out.println("Книга успішно видана!");
            if (returnDate == null) {
                System.out.println("Дата повернення не встановлена");
            }
        }
    }

    /**
     * Повертає книгу до бібліотеки
     *
     * @param scanner Об'єкт для зчитування вводу
     * @param library Бібліотека, куди повертається книга
     * @throws BookNotFoundException Якщо запис про видачу не знайдено
     */
    private static void returnBook(Scanner scanner, Library library) throws BookNotFoundException {
        System.out.println("\nПовернення книги:");

        // Отримуємо активні видачі
        List<LoanRecord> activeLoans = library.getActiveLoans();
        if (activeLoans.isEmpty()) {
            System.out.println("Немає активних видач.");
            return;
        }

        // Виводимо список активних видач
        System.out.println("Оберіть запис про видачу для повернення:");
        for (int i = 0; i < activeLoans.size(); i++) {
            LoanRecord record = activeLoans.get(i);
            System.out.println((i + 1) + ". " + record.getBook().getTitle() +
                    " (Позичальник: " + record.getBorrowerName() +
                    ", Дата видачі: " + record.getLoanDate() + ")");
        }
        System.out.println((activeLoans.size() + 1) + ". Повернутись до попереднього меню");

        int loanChoice = readValidInt(scanner, "Введіть номер запису: ", 1, activeLoans.size() + 1);

        if (loanChoice == activeLoans.size() + 1) {
            return;
        }

        LoanRecord selectedLoan = activeLoans.get(loanChoice - 1);

        // Запитуємо фактичну дату повернення
        LocalDate returnDate = readDate(scanner,
                "Фактична дата повернення (рррр-мм-дд або Enter, за замовчуванням сьогодні): ", LocalDate.now());

        // Виконуємо повернення
        if (library.returnBook(selectedLoan.getRecordId(), returnDate)) {
            System.out.println("Книга успішно повернена!");
        }
    }

    /**
     * Відображає список активних виданих книг
     *
     * @param library Бібліотека, з якої отримуємо дані
     */
    private static void displayActiveLoans(Library library) {
        List<LoanRecord> activeLoans = library.getActiveLoans();
        if (activeLoans.isEmpty()) {
            System.out.println("Немає активних видач.");
            return;
        }

        System.out.println("\nАктивні видачі:");
        for (LoanRecord record : activeLoans) {
            System.out.println(record);
        }
    }

    /**
     * Зчитує очікувану дату повернення книги
     *
     * @param scanner Об'єкт для зчитування вводу
     * @return LocalDate або null, якщо дата не вказана
     */
    private static LocalDate readReturnDate(Scanner scanner) {
        while (true) {
            System.out.print("Очікувана дата повернення (рррр-мм-дд або Enter, якщо невідома): ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                return null; // Користувач не ввів дату - повертаємо null
            }

            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Невірний формат дати. Спробуйте ще раз.");
            }
        }
    }

    /**
     * Зчитує дату з валідацією формату
     *
     * @param scanner      Об'єкт для зчитування вводу
     * @param prompt       Повідомлення для користувача
     * @param defaultValue Значення за замовчуванням (може бути null)
     * @return Об'єкт LocalDate
     */
    private static LocalDate readDate(Scanner scanner, String prompt, LocalDate defaultValue) {
        LocalDate date = null;
        while (date == null) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.isEmpty() && defaultValue != null) {
                return defaultValue;
            }

            try {
                date = LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Невірний формат дати. Використовуйте рррр-мм-дд.");
            }
        }
        return date;
    }

    /**
     * Відображає меню видалення книги та обробляє вибір користувача.
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @param library Об'єкт Library, що містить колекцію книг
     */
    private static void showDeleteBookMenu(Scanner scanner, Library library)
            throws InvalidDataException, BookNotFoundException {
        System.out.println("\nВидалення книги:");

        // Отримуємо всі книги з бібліотеки
        Map<Book, Integer> allBooks = library.getAllBooks();
        if (allBooks.isEmpty()) {
            System.out.println("Бібліотека порожня. Немає книг для видалення.");
            return;
        }

        // Виводимо список книг для вибору
        System.out.println("Оберіть книгу для видалення:");
        List<Book> booksList = new ArrayList<>(allBooks.keySet());
        for (int i = 0; i < booksList.size(); i++) {
            System.out.println((i + 1) + ". " + booksList.get(i));
        }
        System.out.println((booksList.size() + 1) + ". Повернутись до попереднього меню");

        int bookChoice = readValidInt(scanner, "Введіть номер книги: ", 1, booksList.size() + 1);

        // Обробка вибору "Повернутись"
        if (bookChoice == booksList.size() + 1) {
            return;
        }

        Book selectedBook = booksList.get(bookChoice - 1);

        // Підтвердження видалення
        System.out.println("\nВи дійсно хочете видалити наступну книгу?");
        System.out.println(selectedBook);
        System.out.println("1. Так, видалити");
        System.out.println("2. Ні, повернутись");

        int confirmChoice = readValidInt(scanner, "Ваш вибір: ", 1, 2);

        if (confirmChoice == 1) {
            // Виконуємо видалення
            if (library.delete(selectedBook)) {
                System.out.println("Книга успішно видалена!");
            } else {
                System.out.println("Помилка при видаленні книги.");
            }
        }
    }

    /**
     * Відображає меню для модифікації атрибутів книги
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @param library Об'єкт Library, що містить колекцію книг
     */
    private static void showModifyBookMenu(Scanner scanner, Library library) {
        System.out.println("\nМодифікація книги:");

        // Спочатку знаходимо книги для модифікації
        Map<Book, Integer> allBooks = library.getAllBooks();
        if (allBooks.isEmpty()) {
            System.out.println("Бібліотека порожня. Немає книг для модифікації.");
            return;
        }

        // Виводимо список книг для вибору
        System.out.println("Оберіть книгу для модифікації: ");
        List<Book> booksList = new ArrayList<>(allBooks.keySet());
        for (int i = 0; i < booksList.size(); i++) {
            System.out.println((i + 1) + ". " + booksList.get(i));
        }
        System.out.println((booksList.size() + 1) + ". Повернутись до попереднього меню");

        int bookChoice = readValidInt(scanner, "Введіть номер книги: ", 1, booksList.size() + 1);

        // Обробка вибору "Повернутись"
        if (bookChoice == booksList.size() + 1) {
            return;
        }

        Book selectedBook = booksList.get(bookChoice - 1);

        // Створюємо копію книги для модифікації
        Book modifiedBook = library.createBookCopy(selectedBook);

        // Вибір атрибута для зміни
        int maxOption = 6;
        System.out.println("\nОберіть атрибут для зміни:");
        System.out.println("1. Назва");
        System.out.println("2. Автор");
        System.out.println("3. Рік видання");
        System.out.println("4. ISBN");
        System.out.println("5. Кількість сторінок");
        System.out.println("6. Жанр");

        // Додаткові атрибути залежно від типу книги
        if (modifiedBook instanceof EBook) {
            System.out.println("7. Формат");
            System.out.println("8. Розмір файлу");
            maxOption = 8;
        } else if (modifiedBook instanceof PaperBook) {
            System.out.println("7. Тип обкладинки");
            if (modifiedBook instanceof RareBook) {
                System.out.println("8. Орієнтована вартість");
                System.out.println("9. Рік першого видання");
                maxOption = 9;
            } else {
                maxOption = 7;
            }
        } else if (modifiedBook instanceof Audiobook) {
            System.out.println("7. Тривалість");
            System.out.println("8. Диктор");
            maxOption = 8;
        }
        System.out.println((maxOption + 1) + ". Повернутись до попереднього меню");

        int attributeChoice = readValidInt(scanner, "Ваш вибір: ", 1,
                modifiedBook instanceof RareBook ? 10
                        : modifiedBook instanceof EBook || modifiedBook instanceof Audiobook ? 9
                                : modifiedBook instanceof PaperBook ? 8 : 7);

        // Обробка вибору "Повернутись"
        if (attributeChoice == maxOption + 1) {
            return;
        }

        // Модифікація обраного атрибута
        try {
            switch (attributeChoice) {
                case 1:
                    modifiedBook.setTitle(readNonEmptyString(scanner, "Нова назва: "));
                    break;
                case 2:
                    modifiedBook.setAuthor(readNonEmptyString(scanner, "Новий автор: "));
                    break;
                case 3:
                    modifiedBook.setYear(readValidInt(scanner, "Новий рік видання: ", 1450, Year.now().getValue()));
                    break;
                case 4:
                    modifiedBook.setIsbn(readValidIsbn(scanner));
                    break;
                case 5:
                    modifiedBook.setPages(readValidInt(scanner, "Нова кількість сторінок: ", 1, Integer.MAX_VALUE));
                    break;
                case 6:
                    modifiedBook.setGenre(readValidGenre(scanner));
                    break;
                case 7:
                    if (modifiedBook instanceof EBook) {
                        ((EBook) modifiedBook).setFileFormat(readEBookFormat(scanner));
                    } else if (modifiedBook instanceof PaperBook) {
                        ((PaperBook) modifiedBook).setHardcover(readCoverType(scanner));
                    } else if (modifiedBook instanceof Audiobook) {
                        ((Audiobook) modifiedBook)
                                .setDuration(readValidDouble(scanner, "Нова тривалість (год.): ", 0.1, 100));
                    }
                    break;
                case 8:
                    if (modifiedBook instanceof EBook) {
                        ((EBook) modifiedBook)
                                .setFileSize(readValidDouble(scanner, "Новий розмір файлу (MB): ", 0.0, 1000));
                    } else if (modifiedBook instanceof RareBook) {
                        ((RareBook) modifiedBook).setEstimatedValue(
                                readValidInt(scanner, "Нова орієнтовна вартість ($): ", 1, Integer.MAX_VALUE));
                    } else if (modifiedBook instanceof Audiobook) {
                        ((Audiobook) modifiedBook).setNarrator(readNonEmptyString(scanner, "Новий диктор: "));
                    }
                    break;
                case 9:
                    if (modifiedBook instanceof RareBook) {
                        ((RareBook) modifiedBook).setFirstPrintYear(
                                readValidInt(scanner, "Новий рік першого видання: ", 1450, modifiedBook.getYear()));
                    }
                    break;
            }

            // Виконуємо оновлення
            if (library.update(selectedBook, modifiedBook)) {
                System.out.println("Книга успішно оновлена!");
            } else {
                System.out.println("Помилка при оновленні книги.");
            }
        } catch (TitleException | AuthorException | YearException | IsbnException | PagesException | GenreException
                | FileSizeException | DurationException | ValueException | FirstPrintYearException
                | NarratorException e) {
            System.out.println("Помилка: " + e.getMessage());
        }
    }

    /**
     * Відображає меню пошуку книг та обробляє вибір користувача.
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @param library Об'єкт Library, що містить колекцію книг
     */
    private static void showSearchMenu(Scanner scanner, Library library) {
        int choice;
        do {
            System.out.println("\nОберіть критерій пошуку:");
            System.out.println("1. Пошук за UUID");
            System.out.println("2. Пошук за назвою");
            System.out.println("3. Пошук за автором");
            System.out.println("4. Пошук за жанром");
            System.out.println("5. Повернутись до головного меню");
            System.out.print("Ваш вибір: ");

            String input = scanner.nextLine();
            try {
                choice = input.isEmpty() ? -1 : Integer.parseInt(input);
            } catch (NumberFormatException e) {
                choice = -1;
            }

            switch (choice) {
                case 1:
                    searchAndDisplayByUUID(scanner, library);
                    break;
                case 2:
                    searchAndDisplayByTitle(scanner, library);
                    break;
                case 3:
                    searchAndDisplayByAuthor(scanner, library);
                    break;
                case 4:
                    searchAndDisplayByGenre(scanner, library);
                case 5:
                    System.out.println("Програма завершена.");
                    break;
                default:
                    System.out.println("Невірний вибір, спробуйте ще раз.");
            }
        } while (choice != 5);
    }

    private static void searchAndDisplayByUUID(Scanner scanner, Library library) {
        System.out.println("\nВведіть UUID книги для пошуку: ");
        String uuidString = scanner.nextLine().trim();

        try {
            UUID uuid = UUID.fromString(uuidString);
            Book foundBook = library.searchByUUID(uuid);

            if (foundBook != null) {
                System.out.println("\nЗнайдена книга:");
                System.out.println(foundBook);
                System.out.println("Кількість: " + library.getAllBooks().get(foundBook));
            } else {
                System.out.println("\nКнига за UUID " + uuidString + " не знайдена.");
            }
        } catch (Exception e) {
            String errorId = UUID.randomUUID().toString();
            showUserFriendlyError("error.uuid.search", errorId);
            log.error("[ErrorID: {}] - Critical error during UUID search. Контекст: uuidInput='{}'.",
                    errorId, uuidString, e);
        }
    }

    /**
     * Виконує пошук книг за назвою та відображає результати.
     *
     * @param scanner Об'єкт Scanner для зчитування вводу
     * @param library Об'єкт Library, що містить колекцію книг
     */
    private static void searchAndDisplayByTitle(Scanner scanner, Library library) {
        System.out.println("\nВведіть назву або частину назви для пошуку: ");
        String searchTerm = scanner.nextLine().trim();
        Map<Book, Integer> foundBooks = library.searchByTitle(searchTerm);
        displaySearchResults(foundBooks, "за назвою: " + searchTerm);
    }

    /**
     * Виконує пошук книг за автором та відображає результати.
     *
     * @param scanner Об'єкт Scanner для зчитування вводу
     * @param library Об'єкт Library, що містить колекцію книг
     */
    private static void searchAndDisplayByAuthor(Scanner scanner, Library library) {
        System.out.println("\nВведіть автора або його частину для пошуку: ");
        String searchTerm = scanner.nextLine().trim();
        Map<Book, Integer> foundBooks = library.searchByAuthor(searchTerm);
        displaySearchResults(foundBooks, "за автором: " + searchTerm);
    }

    /**
     * Виконує пошук книг за жанром та відображає результати.
     *
     * @param scanner Об'єкт Scanner для зчитування вводу
     * @param library Об'єкт Library, що містить колекцію книг
     */
    private static void searchAndDisplayByGenre(Scanner scanner, Library library) {
        Genre genre = readValidGenre(scanner);
        Map<Book, Integer> foundBooks = library.searchByGenre(genre);
        displaySearchResults(foundBooks, "за жанром: " + genre);
    }

    /**
     * Відображає результати пошуку у стандартизованому форматі.
     *
     * @param books    Мапа знайдених книг (книга → кількість)
     * @param criteria Рядок, що описує критерій пошуку
     */
    private static void displaySearchResults(Map<Book, Integer> books, String criteria) {
        if (books.isEmpty()) {
            System.out.println("\nКниги " + criteria + " не знайдено.");
        } else {
            System.out.println("\nЗнайдені книги " + criteria + ":");
            for (Map.Entry<Book, Integer> entry : books.entrySet()) {
                System.out.println(entry.getKey() + " (Кількість: " + entry.getValue() + ")");
            }
            System.out.println("Всього знайдено: " + books.size() + " позицій");
        }
    }

    /**
     * Відображає всі книги в бібліотеці.
     *
     * @param library Об'єкт Library для отримання книг
     */
    private static void displayAllBooks(Library library) throws InvalidDataException {
        Map<Book, Integer> allBooks = library.getAllBooks();
        if (allBooks.isEmpty()) {
            System.out.println("Бібліотека порожня.");
        } else {
            System.out.println("\nУсі книги в бібліотеці:");
            for (Map.Entry<Book, Integer> entry : allBooks.entrySet()) {
                System.out.println(entry.getKey() + " (Кількість: " + entry.getValue() + ")");
            }
            System.out.println("Всього книг: " + allBooks.size() + " позицій");
        }
    }

    /**
     * Відображає всі книги в бібліотеці у відсортованому порядку.
     *
     * @param library Об'єкт Library для отримання книг
     */
    private static void displaySortedBooks(Scanner scanner, Library library) {
        int choice;
        do {
            System.out.println("\nОберіть критерій сортування:");
            System.out.println("1. За назвою");
            System.out.println("2. За автором");
            System.out.println("3. За роком видання");
            System.out.println("4. За кількістю сторінок");
            System.out.println("5. Повернутись до попереднього меню");
            System.out.print("Ваш вибір: ");

            String input = scanner.nextLine();
            try {
                choice = input.isEmpty() ? -1 : Integer.parseInt(input);
            } catch (NumberFormatException e) {
                choice = -1;
            }

            Map<Book, Integer> sortedBooks;
            switch (choice) {
                case 1: // За назвою
                    sortedBooks = library.getAllBooksSorted(BookComparator.byTitle);
                    break;
                case 2: // За автором
                    sortedBooks = library.getAllBooksSorted(BookComparator.byAuthor);
                    break;
                case 3: // За роком видання
                    sortedBooks = library.getAllBooksSorted(BookComparator.byYear);
                    break;
                case 4: // За кількістю сторінок
                    sortedBooks = library.getAllBooksSorted(BookComparator.byPages);
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Невірний вибір, спробуйте ще раз.");
                    continue;
            }

            if (sortedBooks.isEmpty()) {
                System.out.println("Бібліотека порожня.");
            } else {
                System.out.println("\nУсі книги в бібліотеці (відсортовані):");
                for (Map.Entry<Book, Integer> entry : sortedBooks.entrySet()) {
                    System.out.println(entry.getKey() + " (Кількість: " + entry.getValue() + ")");
                }
                System.out.println("Всього книг: " + sortedBooks.size() + " позицій");
            }
        } while (choice < 1 || choice > 5);
    }

    /**
     * Виводить меню вибору типу книги для створення
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @param library Об'єкт Library, що містить колекцію книг
     */
    private static void showAddBookMenu(Scanner scanner, Library library) {
        int choice;
        do {
            System.out.println("\nОберіть тип книги для додавання:");
            System.out.println("1. Електронна книга");
            System.out.println("2. Паперова книга");
            System.out.println("3. Аудіокнига");
            System.out.println("4. Рідкісна книга");
            System.out.println("5. Повернутись до головного меню");
            System.out.print("Ваш вибір: ");

            String input = scanner.nextLine();
            try {
                choice = input.isEmpty() ? -1 : Integer.parseInt(input);
            } catch (NumberFormatException e) {
                choice = -1;
            }

            int quantity = 1;
            if (choice >= 1 && choice <= 4) {
                quantity = readValidInt(scanner, "Кількість книг для додавання: ", 1, 1000);
            }

            switch (choice) {
                case 1:
                    addEBook(scanner, library, quantity);
                    break;
                case 2:
                    addPaperBook(scanner, library, quantity);
                    break;
                case 3:
                    addAudiobook(scanner, library, quantity);
                    break;
                case 4:
                    addRareBook(scanner, library, quantity);
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Невірний вибір, спробуйте ще раз.");
            }
        } while (choice < 1 || choice > 5);
    }

    /**
     * Додає нову електронну книгу до бібліотеки після отримання даних від
     * користувача.
     *
     * @param scanner  Об'єкт Scanner для читання вводу
     * @param library  Об'єкт Library, що містить колекцію книг
     * @param quantity Кількість книг
     */
    private static void addEBook(Scanner scanner, Library library, int quantity) {
        try {
            System.out.println("\nДодавання електронної книги:");
            String title = readNonEmptyString(scanner, "Назва: ");
            String author = readNonEmptyString(scanner, "Автор: ");
            int year = readValidInt(scanner, "Рік видання: ", 1450, Year.now().getValue());
            String isbn = readValidIsbn(scanner);
            int pages = readValidInt(scanner, "Кількість сторінок: ", 1, Integer.MAX_VALUE);
            Genre genre = readValidGenre(scanner);

            EBookFormat format = readEBookFormat(scanner);
            double size = readValidDouble(scanner, "Розмір файлу (MB): ", 0.0, 1000);

            EBook eBook = new EBook(title, author, year, isbn, pages, genre, format, size);
            if (library.addNewBook(eBook, quantity)) {
                System.out.println("Електронна книга успішно додана!");
            } else {
                System.out.println("Помилка при додаванні книги.");
            }
        } catch (GenreException | YearException | PagesException | AuthorException | TitleException | IsbnException
                | FileSizeException e) {
            System.out.println("Помилка введення даних: " + e.getMessage());
        } catch (Exception e) {
            String errorId = UUID.randomUUID().toString();
            showUserFriendlyError("error.general", errorId);
            log.error(
                    "[ErrorID: {}] - Critical error during eBook addition. Контекст: operation='addEBook', quantity={}.",
                    errorId, quantity, e);
        }
    }

    /**
     * Додає нову паперову книгу до бібліотеки після отримання даних від
     * користувача.
     *
     * @param scanner  Об'єкт Scanner для читання вводу
     * @param library  Об'єкт Library, що містить колекцію книг
     * @param quantity Кількість книг
     */
    private static void addPaperBook(Scanner scanner, Library library, int quantity) {
        try {
            System.out.println("\nДодавання паперової книги:");
            String title = readNonEmptyString(scanner, "Назва: ");
            String author = readNonEmptyString(scanner, "Автор: ");
            int year = readValidInt(scanner, "Рік видання: ", 1450, Year.now().getValue());
            String isbn = readValidIsbn(scanner);
            int pages = readValidInt(scanner, "Кількість сторінок: ", 1, Integer.MAX_VALUE);
            Genre genre = readValidGenre(scanner);

            boolean hardcover = readCoverType(scanner);

            PaperBook paperBook = new PaperBook(title, author, year, isbn, pages, genre, hardcover);
            if (library.addNewBook(paperBook, quantity)) {
                System.out.println("Паперова книга успішно додана!");
            } else {
                System.out.println("Помилка при додаванні книги.");
            }
        } catch (GenreException | YearException | PagesException | AuthorException | TitleException | IsbnException e) {
            System.out.println("Помилка при створенні паперової книги: " + e.getMessage());
        }
    }

    /**
     * Додає нову аудіокнигу до бібліотеки після отримання даних від користувача.
     *
     * @param scanner  Об'єкт Scanner для читання вводу
     * @param library  Об'єкт Library, що містить колекцію книг
     * @param quantity Кількість книг
     */
    private static void addAudiobook(Scanner scanner, Library library, int quantity) {
        try {
            System.out.println("\nДодавання аудіокниги:");
            String title = readNonEmptyString(scanner, "Назва: ");
            String author = readNonEmptyString(scanner, "Автор: ");
            int year = readValidInt(scanner, "Рік видання: ", 1450, Year.now().getValue());
            String isbn = readValidIsbn(scanner);
            int pages = 1;
            Genre genre = readValidGenre(scanner);

            double duration = readValidDouble(scanner, "Тривалість (год.): ", 0.1, 100);
            String narrator = readNonEmptyString(scanner, "Диктор: ");

            Audiobook audioBook = new Audiobook(title, author, year, isbn, pages, genre, duration, narrator);
            if (library.addNewBook(audioBook, quantity)) {
                System.out.println("Аудіокнига успішно додана!");
            } else {
                System.out.println("Помилка при додаванні книги.");
            }
        } catch (GenreException | YearException | PagesException | AuthorException | TitleException | IsbnException
                | DurationException | NarratorException e) {
            System.out.println("Помилка при створенні аудіокниги: " + e.getMessage());
        }
    }

    /**
     * Додає нову рідкісну книгу до бібліотеки після отримання даних від
     * користувача.
     *
     * @param scanner  Об'єкт Scanner для читання вводу
     * @param library  Об'єкт Library, що містить колекцію книг
     * @param quantity Кількість книг
     */
    private static void addRareBook(Scanner scanner, Library library, int quantity) {
        try {
            System.out.println("\nДодавання рідкісної книги:");
            String title = readNonEmptyString(scanner, "Назва: ");
            String author = readNonEmptyString(scanner, "Автор: ");
            int year = readValidInt(scanner, "Рік видання: ", 1450, Year.now().getValue());
            String isbn = readValidIsbn(scanner);
            int pages = readValidInt(scanner, "Кількість сторінок: ", 1, Integer.MAX_VALUE);
            Genre genre = readValidGenre(scanner);

            boolean hardcover = readCoverType(scanner);
            int value = readValidInt(scanner, "Орієнтовна вартість ($): ", 1, Integer.MAX_VALUE);
            int firstPrintYear = readValidInt(scanner, "Рік першого видання: ", 1450, year);

            RareBook rareBook = new RareBook(title, author, year, isbn, pages, genre, hardcover, value, firstPrintYear);
            if (library.addNewBook(rareBook, quantity)) {
                System.out.println("Рідкісна книга успішно додана!");
            } else {
                System.out.println("Помилка при додаванні книги.");
            }
        } catch (GenreException | YearException | PagesException | AuthorException | TitleException | IsbnException
                | ValueException | FirstPrintYearException e) {
            System.out.println("Помилка при створенні рідкісної книги: " + e.getMessage());
        }
    }

    /**
     * Зчитує непорожній рядок від користувача.
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @param prompt  Повідомлення для користувача.
     * @return Введений рядок
     */
    private static String readNonEmptyString(Scanner scanner, String prompt) {
        String input;
        do {
            System.out.print(prompt);
            input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("Помилка: Поле не може бути порожнім.");
            }
        } while (input.isEmpty());
        return input;
    }

    /**
     * Зчитує коректне число у вказаному діапазоні.
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @param prompt  Повідомлення для користувача.
     * @param min     Мінімальне значення.
     * @param max     Максимальне значення.
     * @return Валідне ціле число у межах [min, max].
     */
    private static int readValidInt(Scanner scanner, String prompt, int min, int max) {
        int value = min - 1;
        while (value < min || value > max) {
            System.out.print(prompt);
            try {
                value = Integer.parseInt(scanner.nextLine());
                if (value < min || value > max) {
                    System.out.println("Помилка: Введене число має бути в межах " + min + "-" + max);
                    log.debug("Число поза межами [{}-{}]: {}", min, max, value);
                }
            } catch (NumberFormatException e) {
                System.out.println("Помилка: Введіть коректне число.");
                log.debug("Не вдалося розпарсити ціле число у readValidInt.", e);
            }
        }
        return value;
    }

    /**
     * Зчитує коректне число з плаваючою точкою у вказаному діапазоні.
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @param prompt  Повідомлення для користувача.
     * @param min     Мінімальне значення.
     * @param max     Максимальне значення.
     * @return Валідне число у межах [min, max].
     */
    private static double readValidDouble(Scanner scanner, String prompt, double min, double max) {
        double value = min - 1;
        while (value <= min || value > max) {
            System.out.print(prompt);
            try {
                value = Double.parseDouble(scanner.nextLine());
                if (value <= min || value > max) {
                    System.out.println("Помилка: Введене число має бути в межах " + min + "-" + max);
                    log.debug("Число з плаваючою точкою поза межами ({}-{}]: {}", min, max, value);
                }
            } catch (NumberFormatException e) {
                System.out.println("Помилка: Введіть коректне число.");
                log.debug("Не вдалося розпарсити число з плаваючою точкою у readValidDouble.", e);
            }
        }
        return value;
    }

    /**
     * Зчитує коректний ISBN у форматі 978-XX-XXXXXX-X
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @return Валідний ISBN
     */
    private static String readValidIsbn(Scanner scanner) {
        String isbn = "";
        while (!isbn.matches("978-\\d{2}-\\d{4,6}-\\d")) {
            System.out.print("ISBN (978-XX-XXXXXX-X): ");
            isbn = scanner.nextLine();
            if (!isbn.matches("978-\\d{2}-\\d{4,6}-\\d")) {
                System.out.println("Помилка: Некоректний формат ISBN.");
            }
        }
        return isbn;
    }

    /**
     * Зчитує коректний жанр книги від користувача.
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @return Валідний жанр зі списку Genre
     */
    private static Genre readValidGenre(Scanner scanner) {
        System.out.println("Оберіть жанр: ");
        Genre[] genres = Genre.values();
        for (int i = 0; i < genres.length; i++) {
            System.out.println((i + 1) + ". " + genres[i]);
        }

        int genreChoice = -1;
        while (genreChoice < 1 || genreChoice > genres.length) {
            System.out.print("Виберіть номер жанру (1-" + genres.length + "): ");
            try {
                genreChoice = Integer.parseInt(scanner.nextLine());
                if (genreChoice < 1 || genreChoice > genres.length) {
                    System.out.println("Помилка: Виберіть номер із доступного списку.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Помилка: Введіть число.");
            }
        }
        return genres[genreChoice - 1];
    }

    /**
     * Зчитує коректний формат електронної книги від користувача.
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @return Валідний формат зі списку EBookFormat
     */
    private static EBookFormat readEBookFormat(Scanner scanner) {
        System.out.println("Доступні формати: ");
        EBookFormat[] formats = EBookFormat.values();
        for (int i = 0; i < formats.length; i++) {
            System.out.printf("%d. %s\n", i + 1, formats[i]);
        }

        int choice = readValidInt(scanner, "Оберіть формат (1-" + formats.length + "): ", 1, formats.length);
        return formats[choice - 1];
    }

    /**
     * Зчитує коректний тип обкладинки паперової книги від користувача.
     *
     * @param scanner Об'єкт Scanner для читання вводу
     * @return Булеве значення для типу обкладинки
     */
    private static boolean readCoverType(Scanner scanner) {
        System.out.println("Тип обкладинки:");
        System.out.println("1. М'яка");
        System.out.println("2. Тверда");

        int choice = readValidInt(scanner, "Оберіть тип обкладинки (1-2): ", 1, 2);
        return choice == 2;
    }

    public void MyMethod() {
    }
}

/*
 * Звичайна книга:
 * Грокаємо глибоке вивчення
 * Ендрю Траск
 * 2019
 * 978-54-461133-4
 * 352
 * Комп'ютерна література
 * -------------------------
 * Електронна книга:
 * Core Java Volume I - Fundamentals
 * Cay S. Horstmann
 * 2024
 * 978-13-532837-3
 * 1478
 * 3
 * 2
 * 11.3
 * -------------------------
 * Паперова книга:
 * 1984
 * Джордж Орвел
 * 1949
 * 978-45-678901-2
 * 328
 * 5
 * 2
 * ------- ще 1 -----------
 * Гобіт, або Туди і звідти
 * Дж.Р.Р. Толкін
 * 1937
 * 978-00-747606-1
 * 310
 * 8
 * 1
 * -------------------------
 * Аудіокнига:
 * Шерлок Холмс. Вивчення в багряних тонах
 * Артур Конан Дойл
 * 2021
 * 978-33-445566-7
 * 2
 * 6.5
 * Ігор Степанов
 * 
 * -------------------------
 * Рідкісна книга:
 * Кобзар
 * Тарас Шевченко
 * 1840
 * 978-00-111222-3
 * 150
 * Історичний
 * тверда
 * 200
 * 1840
 */