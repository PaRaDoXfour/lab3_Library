package org.example;

import java.time.Year;
import java.util.Objects;
import java.util.UUID;

/**
 * Абстрактний клас Book представляє книгу в бібліотеці.
 * Містить інформацію про унікальний ідентифікатор, назву, автора, рік видання, ISBN, кількість сторінок та жанр книги.
 */
public abstract class Book implements Comparable<Book> {
    private UUID id;
    private String title;
    private String author;
    private int year;
    private String isbn;
    private int pages;
    private Genre genre;

    /**
     * Конструктор із параметрами для ініціалізації книги.
     *
     * @param title  Назва книги
     * @param author Автор книги
     * @param year   Рік видання
     * @param isbn   ISBN книги
     * @param pages  Кількість сторінок у книзі
     * @param genre  Жанр книги
     * @throws TitleException якщо назва порожня
     */
    public Book(String title, String author, int year, String isbn, int pages, Genre genre) throws TitleException, AuthorException,YearException, IsbnException, PagesException, GenreException {
        this.id = UUID.randomUUID();
        setTitle(title);
        setAuthor(author);
        setYear(year);
        setIsbn(isbn);
        setPages(pages);
        setGenre(genre);
    }

    /**
     * Конструктор копіювання
     *
     * @param book об'єкт для копіювання
     */
    public Book(Book book) throws BookException {
        if (book == null) throw new BookException("Оригінальна книга не може бути порожньою");
        this.id = book.id;
        this.title = book.title;
        this.author = book.author;
        this.year = book.year;
        this.isbn = book.isbn;
        this.pages = book.pages;
        this.genre = book.genre;
    }

    /**
     * Отримує унікальний ідентифікатор книги
     *
     * @return UUID книги
     */
    public UUID getId() {
        return id;
    }

    /**
     * Встановлює унікальний ідентифікатор
     *
     * @param id Унікальний ідентифікатор
     */
    public void setId(UUID id) throws IDException {
        if (id == null) {
            throw new IDException("ID не може бути null");
        }
        this.id = id;
    }

    /**
     * Отримує назву книги.
     *
     * @return Назва книги
     */
    public String getTitle() {
        return title;
    }

    /**
     * Встановлює назву книги.
     *
     * @param title Назва книги
     * @throws TitleException якщо назва порожня
     */
    public void setTitle(String title) throws TitleException {
        if (title == null || title.trim().isEmpty()) {
            throw new TitleException("Назва книги не може бути порожньою.");
        }
        this.title = title;
    }

    /**
     * Отримує автора книги.
     *
     * @return Автор книги
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Встановлює автора книги.
     *
     * @param author Автор книги
     * @throws AuthorException якщо автор порожній
     */
    public void setAuthor(String author) throws AuthorException {
        if (author == null || author.trim().isEmpty()) {
            throw new AuthorException("Автор книги не може бути порожнім.");
        }
        this.author = author;
    }

    /**
     * Отримує рік видання книги.
     *
     * @return Рік видання
     */
    public int getYear() {
        return year;
    }

    /**
     * Встановлює рік видання книги.
     *
     * @param year Рік видання
     * @throws YearException якщо рік виходить за межі 1450-2025
     */
    public void setYear(int year) throws YearException{
        if (year < 1450 || year > Year.now().getValue()) {
            throw new YearException("Рік видання має бути у діапазоні 1450-" + Year.now().getValue());
        }
        this.year = year;
    }

    /**
     * Отримує ISBN книги.
     *
     * @return ISBN книги
     */
    public String getIsbn() {
        return isbn;
    }

    /**
     * Встановлює ISBN книги.
     *
     * @param isbn ISBN книги у форматі 978-XX-XXXXXX-X
     * @throws IsbnException якщо формат ISBN некоректний
     */
    public void setIsbn(String isbn) throws IsbnException {
        if (isbn == null || !isbn.matches("\\d{3}-\\d{2}-\\d{6}-\\d")) {
            throw new IsbnException("Некоректний формат ISBN (має бути у форматі 978-XX-XXXXXX-X). ");
        }
        this.isbn = isbn;
    }

    /**
     * Отримує кількість сторінок у книзі.
     *
     * @return Кількість сторінок
     */
    public int getPages() {
        return pages;
    }

    /**
     * Встановлює кількість сторінок у книзі.
     *
     * @param pages Кількість сторінок (має бути більше ніж 0)
     * @throws PagesException якщо кількість сторінок недопустима
     */
    public void setPages(int pages) throws PagesException {
        if (pages <= 0) {
            throw new PagesException("Кількість сторінок має бути додатнім числом");
        }
        this.pages = pages;
    }

    /**
     * Отримує жанр книги.
     *
     * @return Жанр книги
     */
    public Genre getGenre() {
        return genre;
    }

    /**
     * Встановлює жанр книги.
     *
     * @param genre Жанр книги
     * @throws GenreException якщо жанр порожній
     */
    public void setGenre(Genre genre) throws GenreException {
        if (genre == null) {
            throw new GenreException("Жанр книги не може бути порожнім.");
        }
        this.genre = genre;
    }

    /**
     * Перевизначений метод toString для представлення книги у вигляді рядка.
     *
     * @return рядкове представлення книги
     */
    @Override
    public String toString() {
        return "Книга: " + title + ", Автор: " + author + ", UUID: " + id +  ", Рік видання: " + year + ", ISBN: " + isbn +
                ", Сторінок: " + pages + ", Жанр: " + genre;
    }

    /**
     * Перевизначений метод compareTo для сортування книг.
     * Сортування за назвою, потім за автором, потім за роком видання.
     *
     * @param other інша книга для порівняння
     * @return результат порівняння
     */
    @Override
    public int compareTo(Book other) {
        // Спочатку порівнюємо базові поля
        int result = this.getTitle().compareTo(other.getTitle());
        if (result != 0) return result;

        result = this.getAuthor().compareTo(other.getAuthor());
        if (result != 0) return result;

        result = Integer.compare(this.getYear(), other.getYear());
        if (result!=0) return result;

        result = this.getIsbn().compareTo(other.getIsbn());
        if (result != 0) return result;

        result = Integer.compare(this.getPages(), other.getPages());
        if (result != 0) return result;

        result = this.getGenre().compareTo(other.getGenre());
        if (result != 0) return result;

        // Якщо базові поля рівні, порівнюємо за типом книги
        switch (this) {
            case EBook thisE when other instanceof EBook otherE -> {
                result = thisE.getFileFormat().compareTo(otherE.getFileFormat());
                if (result != 0) return result;
                return Double.compare(thisE.getFileSize(), otherE.getFileSize());
            }
            case Audiobook thisA when other instanceof Audiobook otherA -> {
                result = Double.compare(thisA.getDuration(), otherA.getDuration());
                if (result != 0) return result;
                return thisA.getNarrator().compareTo(otherA.getNarrator());
            }
            case RareBook thisR when other instanceof RareBook otherR -> {
                result = Integer.compare(thisR.getEstimatedValue(), otherR.getEstimatedValue());
                if (result != 0) return result;
                return thisR.getFirstPrintYear().compareTo(otherR.getFirstPrintYear());
            }
            case PaperBook thisP when other instanceof PaperBook otherP -> {
                return Boolean.compare(thisP.getHardcover(), otherP.getHardcover());
            }
            default -> {
            }
        }

        return 0;
    }

    /**
     * Генерує унікальний хеш-код для об'єкта
     *
     * @return Числове значення хешу.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, title, author, year, isbn, pages, genre);
    }

    /**
     * Перевизначений метод equals для порівняння книг.
     *
     * @param obj інший об'єкт
     * @return true, якщо книги мають однакові поля, інакше false
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Book book=(Book) obj;
        return id.equals(book.id)&&
                title.equals(book.title) &&
                author.equals(book.author) &&
                year == book.year &&
                isbn.equals(book.isbn) &&
                pages == book.pages &&
                genre == book.genre;
    }
}
