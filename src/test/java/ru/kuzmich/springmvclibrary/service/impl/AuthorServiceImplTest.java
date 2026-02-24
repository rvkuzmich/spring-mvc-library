package ru.kuzmich.springmvclibrary.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import ru.kuzmich.springmvclibrary.dto.AuthorDto;
import ru.kuzmich.springmvclibrary.dto.AuthorSummaryDto;
import ru.kuzmich.springmvclibrary.exceptions.DuplicateResourceException;
import ru.kuzmich.springmvclibrary.exceptions.ResourceNotFoundException;
import ru.kuzmich.springmvclibrary.model.Author;
import ru.kuzmich.springmvclibrary.model.Book;
import ru.kuzmich.springmvclibrary.repository.AuthorRepository;
import ru.kuzmich.springmvclibrary.repository.BookRepository;

@ExtendWith(MockitoExtension.class)
class AuthorServiceImplTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private AuthorServiceImpl authorService;

    private Author author;
    private Author author2;
    private AuthorDto authorDto;
    private List<Author> authorList;
    private Pageable pageable;
    private Book book1;
    private Book book2;

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setId(1L);
        author.setFirstName("Ivan");
        author.setLastName("Ivanov");
        author.setEmail("i.ivanov@test.ru");

        author2 = new Author();
        author2.setId(2L);
        author2.setFirstName("Petr");
        author2.setLastName("Petrov");
        author2.setEmail("p.petrov@test.ru");

        book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Book 1");
        book1.setIsbn("1111111111");
        book1.setPublishedDate(LocalDate.now());
        book1.setGenre("Fiction");
        book1.setPageCount(200);
        book1.setAuthor(author);

        book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Book 2");
        book2.setIsbn("2222222222");
        book2.setPublishedDate(LocalDate.now());
        book2.setGenre("Non-Fiction");
        book2.setPageCount(300);
        book2.setAuthor(author);

        author.setBooks(Arrays.asList(book1, book2));

        authorList = Arrays.asList(author, author2);
        pageable = PageRequest.of(0, 10);

        authorDto = new AuthorDto();
        authorDto.setFirstName("Ivan");
        authorDto.setLastName("Ivanov");
        authorDto.setEmail("i.ivanov@test.ru");
    }

    @Test
    void getAllAuthors_ShouldReturnPageOfAuthorSummaries() {
        Page<Author> authorPage = new PageImpl<>(authorList, pageable, authorList.size());
        when(authorRepository.findAll(any(Pageable.class))).thenReturn(authorPage);

        Page<AuthorSummaryDto> result = authorService.getAllAuthors(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Ivan Ivanov");
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("i.ivanov@test.ru");
        assertThat(result.getContent().get(0).getBooksCount()).isEqualTo(2);
        assertThat(result.getContent().get(1).getId()).isEqualTo(2L);
        assertThat(result.getContent().get(1).getFullName()).isEqualTo("Petr Petrov");
        assertThat(result.getContent().get(1).getBooksCount()).isEqualTo(0);

        verify(authorRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void getAllAuthors_WithEmptyList_ShouldReturnEmptyPage() {
        Page<Author> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(authorRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        Page<AuthorSummaryDto> result = authorService.getAllAuthors(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(authorRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void getAuthorById_WithValidId_ShouldReturnAuthorDto() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        AuthorDto result = authorService.getAuthorById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("Ivan");
        assertThat(result.getLastName()).isEqualTo("Ivanov");
        assertThat(result.getEmail()).isEqualTo("i.ivanov@test.ru");
        assertThat(result.getBookIds()).hasSize(2);
        assertThat(result.getBookTitles()).hasSize(2);
        assertThat(result.getBooksCount()).isEqualTo(2);
        assertThat(result.getBookIds()).containsExactly(1L, 2L);
        assertThat(result.getBookTitles()).containsExactly("Book 1", "Book 2");

        verify(authorRepository, times(1)).findById(1L);
    }

    @Test
    void getAuthorById_WithInvalidId_ShouldThrowResourceNotFoundException() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.getAuthorById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Author not found with id: 99");

        verify(authorRepository, times(1)).findById(99L);
    }

    @Test
    void getAuthorByEmail_WithValidEmail_ShouldReturnAuthorDto() {
        when(authorRepository.findByEmail("i.ivanov@test.ru")).thenReturn(Optional.of(author));

        AuthorDto result = authorService.getAuthorByEmail("i.ivanov@test.ru");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("i.ivanov@test.ru");

        verify(authorRepository, times(1)).findByEmail("i.ivanov@test.ru");
    }

    @Test
    void getAuthorByEmail_WithInvalidEmail_ShouldThrowResourceNotFoundException() {
        when(authorRepository.findByEmail("invalid@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.getAuthorByEmail("invalid@example.com"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Author not found with email: invalid@example.com");

        verify(authorRepository, times(1)).findByEmail("invalid@example.com");
    }

    @Test
    void createAuthor_WithValidData_ShouldReturnCreatedAuthor() {
        when(authorRepository.findByEmail("i.ivanov@test.ru")).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenReturn(author);

        AuthorDto result = authorService.createAuthor(authorDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("Ivan");
        assertThat(result.getLastName()).isEqualTo("Ivanov");
        assertThat(result.getEmail()).isEqualTo("i.ivanov@test.ru");
        assertThat(result.getBooksCount()).isEqualTo(2);

        verify(authorRepository, times(1)).findByEmail("i.ivanov@test.ru");
        verify(authorRepository, times(1)).save(any(Author.class));
    }

    @Test
    void createAuthor_WithDuplicateEmail_ShouldThrowDuplicateResourceException() {
        when(authorRepository.findByEmail("i.ivanov@test.ru")).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> authorService.createAuthor(authorDto))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Author with email i.ivanov@test.ru already exists");

        verify(authorRepository, times(1)).findByEmail("i.ivanov@test.ru");
        verify(authorRepository, never()).save(any(Author.class));
    }

    @Test
    void updateAuthor_WithValidData_ShouldReturnUpdatedAuthor() {
        AuthorDto updateDto = new AuthorDto();
        updateDto.setFirstName("Ivan Updated");
        updateDto.setLastName("Ivanov Updated");
        updateDto.setEmail("i.updated@test.ru");

        Author updatedAuthor = new Author();
        updatedAuthor.setId(1L);
        updatedAuthor.setFirstName("Ivan Updated");
        updatedAuthor.setLastName("Ivanov Updated");
        updatedAuthor.setEmail("i.updated@test.ru");
        updatedAuthor.setBooks(author.getBooks());

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorRepository.findByEmail("i.updated@test.ru")).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenReturn(updatedAuthor);

        AuthorDto result = authorService.updateAuthor(1L, updateDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("Ivan Updated");
        assertThat(result.getLastName()).isEqualTo("Ivanov Updated");
        assertThat(result.getEmail()).isEqualTo("i.updated@test.ru");

        verify(authorRepository, times(1)).findById(1L);
        verify(authorRepository, times(1)).findByEmail("i.updated@test.ru");
        verify(authorRepository, times(1)).save(any(Author.class));
    }

    @Test
    void updateAuthor_WithDuplicateEmail_ShouldThrowDuplicateResourceException() {
        AuthorDto updateDto = new AuthorDto();
        updateDto.setFirstName("Ivan");
        updateDto.setLastName("Ivanov");
        updateDto.setEmail("p.petrov@test.ru");

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorRepository.findByEmail("p.petrov@test.ru")).thenReturn(Optional.of(author2));

        assertThatThrownBy(() -> authorService.updateAuthor(1L, updateDto))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Author with email p.petrov@test.ru already exists");

        verify(authorRepository, times(1)).findById(1L);
        verify(authorRepository, times(1)).findByEmail("p.petrov@test.ru");
        verify(authorRepository, never()).save(any(Author.class));
    }

    @Test
    void updateAuthor_WithInvalidId_ShouldThrowResourceNotFoundException() {
        AuthorDto updateDto = new AuthorDto();
        updateDto.setFirstName("Ivan");
        updateDto.setLastName("Ivanov");
        updateDto.setEmail("i.ivanov@test.ru");

        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.updateAuthor(99L, updateDto))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Author not found with id: 99");

        verify(authorRepository, times(1)).findById(99L);
        verify(authorRepository, never()).findByEmail(anyString());
        verify(authorRepository, never()).save(any(Author.class));
    }

    @Test
    void partialUpdateAuthor_WithEmailUpdate_ShouldReturnUpdatedAuthor() {
        AuthorDto partialDto = new AuthorDto();
        partialDto.setEmail("new.email@test.ru");

        Author updatedAuthor = new Author();
        updatedAuthor.setId(1L);
        updatedAuthor.setFirstName("Ivan");
        updatedAuthor.setLastName("Ivanov");
        updatedAuthor.setEmail("new.email@test.ru");
        updatedAuthor.setBooks(author.getBooks());

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorRepository.findByEmail("new.email@test.ru")).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenReturn(updatedAuthor);

        AuthorDto result = authorService.partialUpdateAuthor(1L, partialDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("Ivan");
        assertThat(result.getLastName()).isEqualTo("Ivanov");
        assertThat(result.getEmail()).isEqualTo("new.email@test.ru");

        verify(authorRepository, times(1)).findById(1L);
        verify(authorRepository, times(1)).findByEmail("new.email@test.ru");
        verify(authorRepository, times(1)).save(any(Author.class));
    }

    @Test
    void partialUpdateAuthor_WithFirstNameUpdate_ShouldReturnUpdatedAuthor() {
        AuthorDto partialDto = new AuthorDto();
        partialDto.setFirstName("Vanya");

        Author updatedAuthor = new Author();
        updatedAuthor.setId(1L);
        updatedAuthor.setFirstName("Vanya");
        updatedAuthor.setLastName("Ivanov");
        updatedAuthor.setEmail("i.ivanov@test.ru");
        updatedAuthor.setBooks(author.getBooks());

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorRepository.save(any(Author.class))).thenReturn(updatedAuthor);

        AuthorDto result = authorService.partialUpdateAuthor(1L, partialDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("Vanya");
        assertThat(result.getLastName()).isEqualTo("Ivanov");
        assertThat(result.getEmail()).isEqualTo("i.ivanov@test.ru");

        verify(authorRepository, times(1)).findById(1L);
        verify(authorRepository, never()).findByEmail(anyString());
        verify(authorRepository, times(1)).save(any(Author.class));
    }

    @Test
    void partialUpdateAuthor_WithDuplicateEmail_ShouldThrowDuplicateResourceException() {
        AuthorDto partialDto = new AuthorDto();
        partialDto.setEmail("p.petrov@test.ru");

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorRepository.findByEmail("p.petrov@test.ru")).thenReturn(Optional.of(author2));

        assertThatThrownBy(() -> authorService.partialUpdateAuthor(1L, partialDto))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Author with email p.petrov@test.ru already exists");

        verify(authorRepository, times(1)).findById(1L);
        verify(authorRepository, times(1)).findByEmail("p.petrov@test.ru");
        verify(authorRepository, never()).save(any(Author.class));
    }

    @Test
    void deleteAuthor_WithValidId_ShouldDeleteAuthor() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        doNothing().when(authorRepository).delete(author);

        authorService.deleteAuthor(1L);

        verify(authorRepository, times(1)).findById(1L);
        verify(authorRepository, times(1)).delete(author);
    }

    @Test
    void deleteAuthor_WithInvalidId_ShouldThrowResourceNotFoundException() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.deleteAuthor(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Author not found with id: 99");

        verify(authorRepository, times(1)).findById(99L);
        verify(authorRepository, never()).delete(any(Author.class));
    }
}