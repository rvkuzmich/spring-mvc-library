package ru.kuzmich.springmvclibrary.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.kuzmich.springmvclibrary.dto.BookDto;
import ru.kuzmich.springmvclibrary.exceptions.DuplicateResourceException;
import ru.kuzmich.springmvclibrary.exceptions.ResourceNotFoundException;
import ru.kuzmich.springmvclibrary.service.BookService;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    private BookDto bookDto;
    private List<BookDto> bookList;
    private Pageable pageable;
    private Page<BookDto> bookPage;

    @BeforeEach
    void setUp() {
        bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Clean Code");
        bookDto.setIsbn("9780132350884");
        bookDto.setPublishedDate(LocalDate.of(2008, 8, 1));
        bookDto.setGenre("Programming");
        bookDto.setPageCount(464);
        bookDto.setAuthorId(1L);
        bookDto.setAuthorName("Robert Martin");

        BookDto bookDto2 = new BookDto();
        bookDto2.setId(2L);
        bookDto2.setTitle("The Pragmatic Programmer");
        bookDto2.setIsbn("9780201616224");
        bookDto2.setPublishedDate(LocalDate.of(1999, 10, 20));
        bookDto2.setGenre("Programming");
        bookDto2.setPageCount(352);
        bookDto2.setAuthorId(2L);
        bookDto2.setAuthorName("David Thomas");

        bookList = Arrays.asList(bookDto, bookDto2);

        pageable = PageRequest.of(0, 10);
        bookPage = new PageImpl<>(bookList, pageable, bookList.size());
    }

    @Test
    void getAllBooks_ShouldReturnPageOfBooks() throws Exception {
        when(bookService.getAllBooks(any(Pageable.class))).thenReturn(bookPage);

        mockMvc.perform(get("/api/v1/books")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "title,asc")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)));

        verify(bookService, times(1)).getAllBooks(any(Pageable.class));
    }

    @Test
    void getAllBooks_WithDefaultPageable_ShouldUseDefaultValues() throws Exception {
        when(bookService.getAllBooks(any(Pageable.class))).thenReturn(bookPage);

        mockMvc.perform(get("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(bookService, times(1)).getAllBooks(any(Pageable.class));
    }

    @Test
    void getAllBooks_WhenNoBooks_ShouldReturnEmptyPage() throws Exception {
        Page<BookDto> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(bookService.getAllBooks(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)));

        verify(bookService, times(1)).getAllBooks(any(Pageable.class));
    }

    @Test
    void getBookById_WithValidId_ShouldReturnBook() throws Exception {
        when(bookService.getBookById(1L)).thenReturn(bookDto);

        mockMvc.perform(get("/api/v1/books/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(bookService, times(1)).getBookById(1L);
    }

    @Test
    void getBookById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        when(bookService.getBookById(99L)).thenThrow(
            new ResourceNotFoundException("Book not found with id: 99"));

        mockMvc.perform(get("/api/v1/books/99")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(bookService, times(1)).getBookById(99L);
    }

    @Test
    void createBook_WithValidData_ShouldReturnCreatedBook() throws Exception {
        BookDto newBookDto = new BookDto();
        newBookDto.setTitle("Clean Architecture");
        newBookDto.setIsbn("9780134494166");
        newBookDto.setPublishedDate(LocalDate.of(2017, 9, 10));
        newBookDto.setGenre("Programming");
        newBookDto.setPageCount(432);
        newBookDto.setAuthorId(1L);

        BookDto createdBookDto = new BookDto();
        createdBookDto.setId(3L);
        createdBookDto.setTitle("Clean Architecture");
        createdBookDto.setIsbn("9780134494166");
        createdBookDto.setPublishedDate(LocalDate.of(2017, 9, 10));
        createdBookDto.setGenre("Programming");
        createdBookDto.setPageCount(432);
        createdBookDto.setAuthorId(1L);
        createdBookDto.setAuthorName("Robert Martin");

        when(bookService.createBook(any(BookDto.class))).thenReturn(createdBookDto);

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBookDto)))
            .andExpect(status().isCreated());

        verify(bookService, times(1)).createBook(any(BookDto.class));
    }

    @Test
    void createBook_WithDuplicateIsbn_ShouldReturnConflict() throws Exception {
        BookDto newBookDto = new BookDto();
        newBookDto.setTitle("Clean Code");
        newBookDto.setIsbn("9780132350884");
        newBookDto.setPublishedDate(LocalDate.of(2008, 8, 1));
        newBookDto.setGenre("Programming");
        newBookDto.setPageCount(464);
        newBookDto.setAuthorId(1L);

        when(bookService.createBook(any(BookDto.class)))
            .thenThrow(
                new DuplicateResourceException("Book with ISBN 9780132350884 already exists"));

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBookDto)))
            .andExpect(status().isConflict());

        verify(bookService, times(1)).createBook(any(BookDto.class));
    }

    @Test
    void createBook_WithInvalidAuthorId_ShouldReturnNotFound() throws Exception {
        BookDto newBookDto = new BookDto();
        newBookDto.setTitle("Clean Architecture");
        newBookDto.setIsbn("9780134494166");
        newBookDto.setPublishedDate(LocalDate.of(2017, 9, 10));
        newBookDto.setGenre("Programming");
        newBookDto.setPageCount(432);
        newBookDto.setAuthorId(99L);

        when(bookService.createBook(any(BookDto.class)))
            .thenThrow(new ResourceNotFoundException("Author not found with id: 99"));

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBookDto)))
            .andExpect(status().isNotFound());

        verify(bookService, times(1)).createBook(any(BookDto.class));
    }

    @Test
    void createBook_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        BookDto invalidBookDto = new BookDto();
        invalidBookDto.setTitle("");
        invalidBookDto.setIsbn("invalid-isbn");
        invalidBookDto.setPublishedDate(LocalDate.now().plusDays(1));
        invalidBookDto.setPageCount(-100);

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidBookDto)))
            .andExpect(status().isBadRequest());

        verify(bookService, never()).createBook(any(BookDto.class));
    }

    @Test
    void updateBook_WithValidData_ShouldReturnUpdatedBook() throws Exception {
        BookDto updateDto = new BookDto();
        updateDto.setTitle("Clean Code: Updated Edition");
        updateDto.setIsbn("9780132350884");
        updateDto.setPublishedDate(LocalDate.of(2008, 8, 1));
        updateDto.setGenre("Software Development");
        updateDto.setPageCount(480);
        updateDto.setAuthorId(1L);

        BookDto updatedBookDto = new BookDto();
        updatedBookDto.setId(1L);
        updatedBookDto.setTitle("Clean Code: Updated Edition");
        updatedBookDto.setIsbn("9780132350884");
        updatedBookDto.setPublishedDate(LocalDate.of(2008, 8, 1));
        updatedBookDto.setGenre("Software Development");
        updatedBookDto.setPageCount(480);
        updatedBookDto.setAuthorId(1L);
        updatedBookDto.setAuthorName("Robert Martin");

        when(bookService.updateBook(eq(1L), any(BookDto.class))).thenReturn(updatedBookDto);

        mockMvc.perform(put("/api/v1/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk());

        verify(bookService, times(1)).updateBook(eq(1L), any(BookDto.class));
    }

    @Test
    void updateBook_WithInvalidId_ShouldReturnNotFound() throws Exception {
        BookDto updateDto = new BookDto();
        updateDto.setTitle("Clean Code");
        updateDto.setIsbn("9780132350884");
        updateDto.setPublishedDate(LocalDate.of(2008, 8, 1));
        updateDto.setGenre("Programming");
        updateDto.setPageCount(464);
        updateDto.setAuthorId(1L);

        when(bookService.updateBook(eq(99L), any(BookDto.class)))
            .thenThrow(new ResourceNotFoundException("Book not found with id: 99"));

        mockMvc.perform(put("/api/v1/books/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isNotFound());

        verify(bookService, times(1)).updateBook(eq(99L), any(BookDto.class));
    }

    @Test
    void deleteBook_WithValidId_ShouldReturnNoContent() throws Exception {
        doNothing().when(bookService).deleteBook(1L);

        mockMvc.perform(delete("/api/v1/books/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(bookService, times(1)).deleteBook(1L);
    }

    @Test
    void deleteBook_WithInvalidId_ShouldReturnNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Book not found with id: 99"))
            .when(bookService).deleteBook(99L);

        mockMvc.perform(delete("/api/v1/books/99")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(bookService, times(1)).deleteBook(99L);
    }

    @Test
    void searchBooks_WithValidTitle_ShouldReturnMatchingBooks() throws Exception {
        Page<BookDto> searchResult = new PageImpl<>(Collections.singletonList(bookDto), pageable,
            1);
        when(bookService.searchBooksByTitle(eq("Clean"), any(Pageable.class))).thenReturn(
            searchResult);

        mockMvc.perform(get("/api/v1/books/search")
                .param("title", "Clean")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)));

        verify(bookService, times(1)).searchBooksByTitle(eq("Clean"), any(Pageable.class));
    }

    @Test
    void searchBooks_WithNoMatches_ShouldReturnEmptyPage() throws Exception {
        Page<BookDto> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(bookService.searchBooksByTitle(eq("Nonexistent"), any(Pageable.class))).thenReturn(
            emptyPage);

        mockMvc.perform(get("/api/v1/books/search")
                .param("title", "Nonexistent")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(bookService, times(1)).searchBooksByTitle(eq("Nonexistent"), any(Pageable.class));
    }

    @Test
    void getBooksByGenre_WithValidGenre_ShouldReturnBooks() throws Exception {
        when(bookService.getBooksByGenre(eq("Programming"), any(Pageable.class))).thenReturn(
            bookPage);

        mockMvc.perform(get("/api/v1/books/genre/Programming")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)));

        verify(bookService, times(1)).getBooksByGenre(eq("Programming"), any(Pageable.class));
    }

    @Test
    void getBooksByGenre_WithNoBooks_ShouldReturnEmptyPage() throws Exception {
        Page<BookDto> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(bookService.getBooksByGenre(eq("Fantasy"), any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/books/genre/Fantasy")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)));

        verify(bookService, times(1)).getBooksByGenre(eq("Fantasy"), any(Pageable.class));
    }
}