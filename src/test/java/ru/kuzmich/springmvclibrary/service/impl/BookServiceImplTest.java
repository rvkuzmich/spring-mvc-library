package ru.kuzmich.springmvclibrary.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.kuzmich.springmvclibrary.dto.BookDto;
import ru.kuzmich.springmvclibrary.exceptions.DuplicateResourceException;
import ru.kuzmich.springmvclibrary.exceptions.ResourceNotFoundException;
import ru.kuzmich.springmvclibrary.model.Author;
import ru.kuzmich.springmvclibrary.model.Book;
import ru.kuzmich.springmvclibrary.repository.AuthorRepository;
import ru.kuzmich.springmvclibrary.repository.BookRepository;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private Author author;
    private Book book;
    private Book book2;
    private BookDto bookDto;
    private List<Book> bookList;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setId(1L);
        author.setFirstName("Robert");
        author.setLastName("Martin");
        author.setEmail("r.martin@test.ru");

        book = new Book();
        book.setId(1L);
        book.setTitle("Clean Code");
        book.setIsbn("9780132350884");
        book.setPublishedDate(LocalDate.of(2008, 8, 1));
        book.setGenre("Programming");
        book.setPageCount(464);
        book.setAuthor(author);

        book2 = new Book();
        book2.setId(2L);
        book2.setTitle("The Pragmatic Programmer");
        book2.setIsbn("9780201616224");
        book2.setPublishedDate(LocalDate.of(1999, 10, 20));
        book2.setGenre("Programming");
        book2.setPageCount(352);
        book2.setAuthor(author);

        bookList = Arrays.asList(book, book2);
        pageable = PageRequest.of(0, 10);

        bookDto = new BookDto();
        bookDto.setTitle("Clean Code");
        bookDto.setIsbn("9780132350884");
        bookDto.setPublishedDate(LocalDate.of(2008, 8, 1));
        bookDto.setGenre("Programming");
        bookDto.setPageCount(464);
        bookDto.setAuthorId(1L);
    }

    @Test
    void getAllBooks_ShouldReturnPageOfBooks() {
        Page<Book> bookPage = new PageImpl<>(bookList, pageable, bookList.size());
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(bookPage);

        Page<BookDto> result = bookService.getAllBooks(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
        assertThat(result.getContent().get(0).getIsbn()).isEqualTo("9780132350884");
        assertThat(result.getContent().get(0).getAuthorName()).isEqualTo("Robert Martin");
        assertThat(result.getContent().get(1).getId()).isEqualTo(2L);
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("The Pragmatic Programmer");

        verify(bookRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void getAllBooks_WithEmptyList_ShouldReturnEmptyPage() {
        Page<Book> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        Page<BookDto> result = bookService.getAllBooks(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(bookRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void getBookById_WithValidId_ShouldReturnBookDto() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookDto result = bookService.getBookById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getIsbn()).isEqualTo("9780132350884");
        assertThat(result.getGenre()).isEqualTo("Programming");
        assertThat(result.getPageCount()).isEqualTo(464);
        assertThat(result.getAuthorId()).isEqualTo(1L);
        assertThat(result.getAuthorName()).isEqualTo("Robert Martin");

        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    void getBookById_WithInvalidId_ShouldThrowResourceNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Book not found with id: 99");

        verify(bookRepository, times(1)).findById(99L);
    }

    @Test
    void createBook_WithValidData_ShouldReturnCreatedBook() {
        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(false);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        BookDto result = bookService.createBook(bookDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getIsbn()).isEqualTo("9780132350884");
        assertThat(result.getAuthorName()).isEqualTo("Robert Martin");

        verify(bookRepository, times(1)).existsByIsbn("9780132350884");
        verify(authorRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    void createBook_WithDuplicateIsbn_ShouldThrowDuplicateResourceException() {
        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(true);

        assertThatThrownBy(() -> bookService.createBook(bookDto))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Book with ISBN 9780132350884 already exists");

        verify(bookRepository, times(1)).existsByIsbn("9780132350884");
        verify(authorRepository, never()).findById(anyLong());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void createBook_WithInvalidAuthorId_ShouldThrowResourceNotFoundException() {
        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(false);
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        bookDto.setAuthorId(99L);

        assertThatThrownBy(() -> bookService.createBook(bookDto))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Author not found with id: 99");

        verify(bookRepository, times(1)).existsByIsbn("9780132350884");
        verify(authorRepository, times(1)).findById(99L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void updateBook_WithValidData_ShouldReturnUpdatedBook() {
        BookDto updateDto = new BookDto();
        updateDto.setTitle("Clean Code: Updated Edition");
        updateDto.setIsbn("9780132350885"); // Новый ISBN
        updateDto.setPublishedDate(LocalDate.of(2008, 8, 1));
        updateDto.setGenre("Software Development");
        updateDto.setPageCount(480);
        updateDto.setAuthorId(1L);

        Book existingBook = new Book();
        existingBook.setId(1L);
        existingBook.setTitle("Clean Code");
        existingBook.setIsbn("9780132350884");
        existingBook.setPublishedDate(LocalDate.of(2008, 8, 1));
        existingBook.setGenre("Programming");
        existingBook.setPageCount(464);
        existingBook.setAuthor(author);

        Book updatedBook = new Book();
        updatedBook.setId(1L);
        updatedBook.setTitle("Clean Code: Updated Edition");
        updatedBook.setIsbn("9780132350885");
        updatedBook.setPublishedDate(LocalDate.of(2008, 8, 1));
        updatedBook.setGenre("Software Development");
        updatedBook.setPageCount(480);
        updatedBook.setAuthor(author);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existingBook));
        when(bookRepository.existsByIsbn("9780132350885")).thenReturn(false);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(any(Book.class))).thenReturn(updatedBook);

        BookDto result = bookService.updateBook(1L, updateDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Clean Code: Updated Edition");
        assertThat(result.getIsbn()).isEqualTo("9780132350885");
        assertThat(result.getGenre()).isEqualTo("Software Development");
        assertThat(result.getPageCount()).isEqualTo(480);

        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).existsByIsbn("9780132350885");
        verify(authorRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    void updateBook_WithDuplicateIsbn_ShouldThrowDuplicateResourceException() {
        BookDto updateDto = new BookDto();
        updateDto.setTitle("Different Book");
        updateDto.setIsbn("9780201616224"); // ISBN другой книги
        updateDto.setPublishedDate(LocalDate.of(1999, 10, 20));
        updateDto.setGenre("Programming");
        updateDto.setPageCount(352);
        updateDto.setAuthorId(1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.existsByIsbn("9780201616224")).thenReturn(true);

        assertThatThrownBy(() -> bookService.updateBook(1L, updateDto))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Book with ISBN 9780201616224 already exists");

        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).existsByIsbn("9780201616224");
        verify(authorRepository, never()).findById(anyLong());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void updateBook_WithInvalidId_ShouldThrowResourceNotFoundException() {
        BookDto updateDto = new BookDto();
        updateDto.setTitle("Clean Code");
        updateDto.setIsbn("9780132350884");
        updateDto.setPublishedDate(LocalDate.of(2008, 8, 1));
        updateDto.setGenre("Programming");
        updateDto.setPageCount(464);
        updateDto.setAuthorId(1L);

        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBook(99L, updateDto))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Book not found with id: 99");

        verify(bookRepository, times(1)).findById(99L);
        verify(bookRepository, never()).existsByIsbn(anyString());
        verify(authorRepository, never()).findById(anyLong());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void deleteBook_WithValidId_ShouldDeleteBook() {
        when(bookRepository.existsById(1L)).thenReturn(true);
        doNothing().when(bookRepository).deleteById(1L);

        bookService.deleteBook(1L);

        verify(bookRepository, times(1)).existsById(1L);
        verify(bookRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteBook_WithInvalidId_ShouldThrowResourceNotFoundException() {
        when(bookRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.deleteBook(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Book not found with id: 99");

        verify(bookRepository, times(1)).existsById(99L);
        verify(bookRepository, never()).deleteById(anyLong());
    }

    @Test
    void searchBooksByTitle_WithValidTitle_ShouldReturnMatchingBooks() {
        Page<Book> searchResult = new PageImpl<>(Collections.singletonList(book), pageable, 1);
        when(bookRepository.findByTitleContainingIgnoreCase(eq("Clean"), any(Pageable.class)))
            .thenReturn(searchResult);

        Page<BookDto> result = bookService.searchBooksByTitle("Clean", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(bookRepository, times(1))
            .findByTitleContainingIgnoreCase(eq("Clean"), any(Pageable.class));
    }

    @Test
    void searchBooksByTitle_WithNoMatches_ShouldReturnEmptyPage() {
        Page<Book> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(bookRepository.findByTitleContainingIgnoreCase(eq("Nonexistent"), any(Pageable.class)))
            .thenReturn(emptyPage);

        Page<BookDto> result = bookService.searchBooksByTitle("Nonexistent", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(bookRepository, times(1))
            .findByTitleContainingIgnoreCase(eq("Nonexistent"), any(Pageable.class));
    }

    @Test
    void getBooksByGenre_WithValidGenre_ShouldReturnBooks() {
        Page<Book> genrePage = new PageImpl<>(bookList, pageable, bookList.size());
        when(bookRepository.findByGenre(eq("Programming"), any(Pageable.class))).thenReturn(
            genrePage);

        Page<BookDto> result = bookService.getBooksByGenre("Programming", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getGenre()).isEqualTo("Programming");
        assertThat(result.getContent().get(1).getGenre()).isEqualTo("Programming");

        verify(bookRepository, times(1)).findByGenre(eq("Programming"), any(Pageable.class));
    }

    @Test
    void getBooksByGenre_WithNoBooks_ShouldReturnEmptyPage() {
        Page<Book> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(bookRepository.findByGenre(eq("Fantasy"), any(Pageable.class))).thenReturn(emptyPage);

        Page<BookDto> result = bookService.getBooksByGenre("Fantasy", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(bookRepository, times(1)).findByGenre(eq("Fantasy"), any(Pageable.class));
    }
}