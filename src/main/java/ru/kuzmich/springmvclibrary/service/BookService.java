package ru.kuzmich.springmvclibrary.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.kuzmich.springmvclibrary.dto.BookDto;

public interface BookService {

    Page<BookDto> getAllBooks(Pageable pageable);

    BookDto getBookById(Long id);

    BookDto createBook(BookDto bookDTO);

    BookDto updateBook(Long id, BookDto bookDTO);

    void deleteBook(Long id);

    Page<BookDto> searchBooksByTitle(String title, Pageable pageable);

    Page<BookDto> getBooksByGenre(String genre, Pageable pageable);
}
