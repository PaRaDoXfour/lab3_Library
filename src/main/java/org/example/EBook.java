package org.example;

import java.util.Objects;

/**
 * Клас для електронних книг, успадкований від Book
 */
public class EBook extends Book {
    private EBookFormat format; // у форматі PDF, EPUB та інші
    private double fileSize; // у мегабайтах(MB)

    /**
     * Конструктор створення електронної книги
     *
     * @param title Назва книги
     * @param author Автор книги
     * @param year Рік видання
     * @param isbn ISBN книги
     * @param pages Кількість сторінок
     * @param genre Жанр книги
     * @param format Формат електронної книги
     * @param fileSize Розмір електронної книги
     */
    public EBook(String title, String author, int year, String isbn, int pages, Genre genre,
                 EBookFormat format, double fileSize) throws GenreException, YearException, PagesException, AuthorException, TitleException, IsbnException, FileSizeException {
        super(title, author, year, isbn, pages, genre);
        setFileFormat(format);
        setFileSize(fileSize);
    }

    /**
     * Конструктор копіювання для електронної книги
     *
     * @param ebook об'єкт для копіювання
     */
    public EBook(EBook ebook) throws InvalidDataException, BookException {
        super(ebook);
        this.format = ebook.format;
        this.fileSize = ebook.fileSize;
    }

    /**
     * Отримує формат електронної книги
     *
     * @return Формат електронної книги
     */
    public EBookFormat getFileFormat() { return format; }

    /**
     * Встановлює формат електронної книги
     *
     * @param format Формат електронної книги
     */
    public void setFileFormat(EBookFormat format) throws GenreException {
        if (format == null) throw new GenreException("Формат файлу не може бути порожнім.");
        this.format = format;
    }

    /**
     * Отримує розмір електронної книги
     *
     * @return Розмір електронної книги
     */
    public double getFileSize() {
        return fileSize;
    }

    /**
     * Встановлює розмір електронної книги
     *
     * @param fileSize Розмір електронної книги
     */
    public void setFileSize(double fileSize) throws FileSizeException {
        if (fileSize <= 0) {
            throw new FileSizeException("Розмір файлу має бути додатнім");
        }
        this.fileSize = fileSize;
    }

    /**
     * Перевизначений метод toString для представлення книги у вигляді рядка.
     *
     * @return рядкове представлення книги
     */
    @Override
    public String toString() {
        return "Електронна " + super.toString() + String.format(", "
                + "Формат: %s, Розмір: %.2f MB", format, fileSize);
    }

    /**
     * Генерує унікальний хеш-код для об'єкта.
     *
     * @return Числове значення хешу.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), format, fileSize);
    }

    /**
     * Перевизначений метод equals для порівняння книг.
     *
     * @param o інший об'єкт
     * @return true, якщо книги мають однакові поля, інакше false
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        EBook eBook = (EBook) o;
        return Double.compare(fileSize, eBook.fileSize) == 0
                && format == eBook.format;
    }
}
