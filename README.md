# Library System (Individual Mini-Project)

## Project Description
This project is a Java console program developed as part of a laboratory project. It is based on the architecture of the previous project and implements a library fund management system.

The main goal of the project is to implement the functionality of issuing and returning books, as well as using **Stream API** to process data collections.

## Implemented functionality
According to the technical specifications, the **"Book Issue/Book Return"** option has been added to the main menu, which allows:
* **Issue a book**: register the issuance of the selected book to the borrower with a date entry.
* **Return a book**: update the issuance status and return the copy to the library inventory.
* **View active issuances**: display a list of all books currently in the hands of users.

## Architecture and Classes
The project uses an object-oriented model to describe library entities:

### Entities (Models)
* **`Book`**: an abstract base class that contains general information about a book: title, author, year, ISBN, and genre.
* **`EBook`, `PaperBook`, `Audiobook`, `RareBook`**: specialized book types with their own attributes (file size, format, narrator, cost, etc.).
* **`LoanRecord`**: a new class that represents a record of a specific book being issued to a specific person.

### Logic and Services
* **`Library`**: a class for managing inventory (`Map<Book, Integer>`) and the list of issuances (`List<LoanRecord>`).
* **`FileHandler`**: a class for automatically loading and saving data to the `input.txt` file.
* **`Driver`**: the entry point of the program, implementing a text-based user interface.

## Data Handling
* **Save**: the program automatically reads the library from the `input.txt` file on startup and saves all changes (including new books) on exit.
* **Validation**: an `Exception` system is implemented to check the correctness of the ISBN, year of publication, number of pages, and availability of copies in the library.

## Data file format (input.txt)
### For correct data loading, the file must have the following structure:

1. **Library block**:

[Library]

Library name

Library address

2. **Book blocks** (separated by a blank line):

[BookType] (e.g. [EBook] or [PaperBook])

Title

Author

Year

ISBN

Pages

Genre

[Type-specific fields]

## Technologies used

* **Java Collection Framework**: using `HashMap`, `TreeMap` and `ArrayList` to manage the library.
* **Java Stream API**: used to filter records, search by UUID and sort books.
* **Java Time API**: working with issue and return dates (`LocalDate`).

**Due date:** June 13, 2025

