/*package dao;

import java.util.*;
import model.Book;
import util.FileHelper;
import util.Config;

public class BookDAO {
    public static List<Book> getAllBooks() {
        List<Book> list = new ArrayList<>();
        for (String line : FileHelper.readLines(Config.BOOKS_FILE)) {
            Book b = Book.fromString(line);
            if (b != null) list.add(b);
        }
        return list;
    }

    public static Book findById(String id) {
        for (Book b : getAllBooks()) {
            if (b.getId().equals(id)) return b;
        }
        return null;
    }

    public static List<Book> searchByTitle(String keyword) {
        List<Book> results = new ArrayList<>();
        for (Book b : getAllBooks()) {
            if (b.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                results.add(b);
            }
        }
        return results;
    }

    public static void updateBooks(List<Book> books) {
        List<String> lines = new ArrayList<>();
        for (Book b : books) {
            lines.add(b.toString());
        }
        FileHelper.writeLines(Config.BOOKS_FILE, lines);
    }
    public static void addBook(Book book) {
        FileHelper.appendLine(Config.BOOKS_FILE, book.toString());
    }

    public static void deleteBook(String id) {
        List<Book> books = getAllBooks();
        books.removeIf(b -> b.getId().equalsIgnoreCase(id));
        updateBooks(books);
    }

    public static void toggleStatus(String id) {
        List<Book> books = getAllBooks();
        for (Book b : books) {
            if (b.getId().equalsIgnoreCase(id)) {
                b.setAvailable(!b.isAvailable());
            }
        }
        updateBooks(books);
    }

}
*/
