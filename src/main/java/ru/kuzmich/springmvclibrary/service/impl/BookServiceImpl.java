package ru.kuzmich.springmvclibrary.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kuzmich.springmvclibrary.dto.BookDto;
import ru.kuzmich.springmvclibrary.exceptions.DuplicateResourceException;
import ru.kuzmich.springmvclibrary.exceptions.ResourceNotFoundException;
import ru.kuzmich.springmvclibrary.model.Author;
import ru.kuzmich.springmvclibrary.model.Book;
import ru.kuzmich.springmvclibrary.repository.AuthorRepository;
import ru.kuzmich.springmvclibrary.repository.BookRepository;
import ru.kuzmich.springmvclibrary.service.BookService;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    private final AuthorRepository authorRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BookDto> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable)
            .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public BookDto getBookById(Long id) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        return convertToDto(book);
    }

    @Override
    @Transactional
    public BookDto createBook(BookDto bookDto) {
        if (bookRepository.existsByIsbn(bookDto.getIsbn())) {
            throw new DuplicateResourceException(
                "Book with ISBN " + bookDto.getIsbn() + " already exists");
        }

        Author author = authorRepository.findById(bookDto.getAuthorId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Author not found with id: " + bookDto.getAuthorId()));

        Book book = convertToEntity(bookDto);
        book.setAuthor(author);

        Book savedBook = bookRepository.save(book);
        return convertToDto(savedBook);
    }

    @Transactional
    public BookDto updateBook(Long id, BookDto bookDto) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        if (!book.getIsbn().equals(bookDto.getIsbn()) &&
            bookRepository.existsByIsbn(bookDto.getIsbn())) {
            throw new DuplicateResourceException(
                "Book with ISBN " + bookDto.getIsbn() + " already exists");
        }

        Author author = authorRepository.findById(bookDto.getAuthorId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Author not found with id: " + bookDto.getAuthorId()));

        updateBookFromDto(book, bookDto);
        book.setAuthor(author);

        Book updatedBook = bookRepository.save(book);
        return convertToDto(updatedBook);
    }

    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<BookDto> searchBooksByTitle(String title, Pageable pageable) {
        return bookRepository.findByTitleContainingIgnoreCase(title, pageable)
            .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Page<BookDto> getBooksByGenre(String genre, Pageable pageable) {
        return bookRepository.findByGenre(genre, pageable)
            .map(this::convertToDto);
    }

    private BookDto convertToDto(Book book) {
        BookDto dto = new BookDto();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setIsbn(book.getIsbn());
        dto.setPublishedDate(book.getPublishedDate());
        dto.setGenre(book.getGenre());
        dto.setPageCount(book.getPageCount());
        dto.setAuthorId(book.getAuthor().getId());
        dto.setAuthorName(book.getAuthor().getFirstName() + " " + book.getAuthor().getLastName());
        return dto;
    }

    private Book convertToEntity(BookDto dto) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setIsbn(dto.getIsbn());
        book.setPublishedDate(dto.getPublishedDate());
        book.setGenre(dto.getGenre());
        book.setPageCount(dto.getPageCount());
        return book;
    }

    private void updateBookFromDto(Book book, BookDto dto) {
        book.setTitle(dto.getTitle());
        book.setIsbn(dto.getIsbn());
        book.setPublishedDate(dto.getPublishedDate());
        book.setGenre(dto.getGenre());
        book.setPageCount(dto.getPageCount());
    }
}
