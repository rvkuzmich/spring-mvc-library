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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.kuzmich.springmvclibrary.dto.AuthorDto;
import ru.kuzmich.springmvclibrary.dto.AuthorSummaryDto;
import ru.kuzmich.springmvclibrary.exceptions.DuplicateResourceException;
import ru.kuzmich.springmvclibrary.exceptions.ResourceNotFoundException;
import ru.kuzmich.springmvclibrary.service.AuthorService;

@WebMvcTest(AuthorController.class)
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthorService authorService;

    private AuthorDto authorDto;
    private AuthorSummaryDto authorSummaryDto;
    private List<AuthorSummaryDto> authorSummaryList;
    private Pageable pageable;
    private Page<AuthorSummaryDto> authorPage;

    @BeforeEach
    void setUp() {
        authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setFirstName("Ivan");
        authorDto.setLastName("Ivanov");
        authorDto.setEmail("i.ivanov@test.ru");
        authorDto.setBookIds(Arrays.asList(1L, 2L));
        authorDto.setBookTitles(Arrays.asList("Book 1", "Book 2"));
        authorDto.setBooksCount(2);

        authorSummaryDto = new AuthorSummaryDto(1L, "Ivan", "Ivanov", "i.ivanov@test.ru", 2);

        authorSummaryList = Arrays.asList(
            authorSummaryDto,
            new AuthorSummaryDto(2L, "Petr", "Petrov", "p.petrov@test.ru", 1)
        );

        pageable = PageRequest.of(0, 10);
        authorPage = new PageImpl<>(authorSummaryList, pageable, authorSummaryList.size());
    }

    @Test
    void getAllAuthors_ShouldReturnPageOfAuthors() throws Exception {
        when(authorService.getAllAuthors(any(Pageable.class))).thenReturn(authorPage);

        mockMvc.perform(get("/api/v1/authors")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "lastName,asc")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)));

        verify(authorService, times(1)).getAllAuthors(any(Pageable.class));
    }

    @Test
    void getAllAuthors_WithDefaultPageable_ShouldUseDefaultValues() throws Exception {
        when(authorService.getAllAuthors(any(Pageable.class))).thenReturn(authorPage);

        mockMvc.perform(get("/api/v1/authors")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(authorService, times(1)).getAllAuthors(any(Pageable.class));
    }

    @Test
    void getAllAuthors_WhenNoAuthors_ShouldReturnEmptyPage() throws Exception {
        Page<AuthorSummaryDto> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(authorService.getAllAuthors(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/authors")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)));

        verify(authorService, times(1)).getAllAuthors(any(Pageable.class));
    }

    @Test
    void getAuthorById_WithValidId_ShouldReturnAuthor() throws Exception {
        when(authorService.getAuthorById(1L)).thenReturn(authorDto);

        mockMvc.perform(get("/api/v1/authors/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(authorService, times(1)).getAuthorById(1L);
    }

    @Test
    void getAuthorById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        when(authorService.getAuthorById(99L)).thenThrow(
            new ResourceNotFoundException("Author not found with id: 99"));

        mockMvc.perform(get("/api/v1/authors/99")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(authorService, times(1)).getAuthorById(99L);
    }

    @Test
    void getAuthorByEmail_WithValidEmail_ShouldReturnAuthor() throws Exception {
        when(authorService.getAuthorByEmail("i.ivanov@test.ru")).thenReturn(authorDto);

        mockMvc.perform(get("/api/v1/authors/email/i.ivanov@test.ru")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(authorService, times(1)).getAuthorByEmail("i.ivanov@test.ru");
    }

    @Test
    void getAuthorByEmail_WithInvalidEmail_ShouldReturnNotFound() throws Exception {
        when(authorService.getAuthorByEmail("invalid@test.ru"))
            .thenThrow(
                new ResourceNotFoundException("Author not found with email: invalid@test.ru"));

        mockMvc.perform(get("/api/v1/authors/email/invalid@test.ru")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(authorService, times(1)).getAuthorByEmail("invalid@test.ru");
    }

    @Test
    void createAuthor_WithValidData_ShouldReturnCreatedAuthor() throws Exception {
        AuthorDto newAuthorDto = new AuthorDto();
        newAuthorDto.setFirstName("Sidor");
        newAuthorDto.setLastName("Sidorov");
        newAuthorDto.setEmail("s.sidorov@test.ru");

        AuthorDto createdAuthorDto = new AuthorDto();
        createdAuthorDto.setId(2L);
        createdAuthorDto.setFirstName("Sidor");
        createdAuthorDto.setLastName("Sidorov");
        createdAuthorDto.setEmail("s.sidorov@test.ru");
        createdAuthorDto.setBookIds(Collections.emptyList());
        createdAuthorDto.setBookTitles(Collections.emptyList());
        createdAuthorDto.setBooksCount(0);

        when(authorService.createAuthor(any(AuthorDto.class))).thenReturn(createdAuthorDto);

        mockMvc.perform(post("/api/v1/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAuthorDto)))
            .andExpect(status().isCreated());

        verify(authorService, times(1)).createAuthor(any(AuthorDto.class));
    }

    @Test
    void createAuthor_WithDuplicateEmail_ShouldReturnConflict() throws Exception {
        AuthorDto newAuthorDto = new AuthorDto();
        newAuthorDto.setFirstName("Sidor");
        newAuthorDto.setLastName("Sidorov");
        newAuthorDto.setEmail("i.ivanov@test.ru");

        when(authorService.createAuthor(any(AuthorDto.class)))
            .thenThrow(new DuplicateResourceException(
                "Author with email i.ivanov@test.ru already exists"));

        mockMvc.perform(post("/api/v1/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAuthorDto)))
            .andExpect(status().isConflict());

        verify(authorService, times(1)).createAuthor(any(AuthorDto.class));
    }

    @Test
    void createAuthor_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        AuthorDto invalidAuthorDto = new AuthorDto();
        invalidAuthorDto.setFirstName("");
        invalidAuthorDto.setLastName("Ivanov");
        invalidAuthorDto.setEmail("invalid-email");

        mockMvc.perform(post("/api/v1/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidAuthorDto)))
            .andExpect(status().isBadRequest());

        verify(authorService, never()).createAuthor(any(AuthorDto.class));
    }

    @Test
    void updateAuthor_WithValidData_ShouldReturnUpdatedAuthor() throws Exception {
        AuthorDto updateDto = new AuthorDto();
        updateDto.setFirstName("Ivan Updated");
        updateDto.setLastName("Ivanov Updated");
        updateDto.setEmail("i.updated@test.ru");

        AuthorDto updatedAuthorDto = new AuthorDto();
        updatedAuthorDto.setId(1L);
        updatedAuthorDto.setFirstName("Ivan Updated");
        updatedAuthorDto.setLastName("Ivanov Updated");
        updatedAuthorDto.setEmail("i.updated@test.ru");
        updatedAuthorDto.setBookIds(Arrays.asList(1L, 2L));
        updatedAuthorDto.setBookTitles(Arrays.asList("Book 1", "Book 2"));
        updatedAuthorDto.setBooksCount(2);

        when(authorService.updateAuthor(eq(1L), any(AuthorDto.class))).thenReturn(updatedAuthorDto);

        mockMvc.perform(put("/api/v1/authors/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk());

        verify(authorService, times(1)).updateAuthor(eq(1L), any(AuthorDto.class));
    }

    @Test
    void updateAuthor_WithInvalidId_ShouldReturnNotFound() throws Exception {
        AuthorDto updateDto = new AuthorDto();
        updateDto.setFirstName("Ivan");
        updateDto.setLastName("Ivanov");
        updateDto.setEmail("i.ivanov@test.ru");

        when(authorService.updateAuthor(eq(99L), any(AuthorDto.class)))
            .thenThrow(new ResourceNotFoundException("Author not found with id: 99"));

        mockMvc.perform(put("/api/v1/authors/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isNotFound());

        verify(authorService, times(1)).updateAuthor(eq(99L), any(AuthorDto.class));
    }

    @Test
    void partialUpdateAuthor_WithValidData_ShouldReturnUpdatedAuthor() throws Exception {
        AuthorDto partialUpdateDto = new AuthorDto();
        partialUpdateDto.setEmail("new.email@example.com");

        AuthorDto updatedAuthorDto = new AuthorDto();
        updatedAuthorDto.setId(1L);
        updatedAuthorDto.setFirstName("Ivan");
        updatedAuthorDto.setLastName("Ivanov");
        updatedAuthorDto.setEmail("new.email@example.com");
        updatedAuthorDto.setBookIds(Arrays.asList(1L, 2L));
        updatedAuthorDto.setBookTitles(Arrays.asList("Book 1", "Book 2"));
        updatedAuthorDto.setBooksCount(2);

        when(authorService.partialUpdateAuthor(eq(1L), any(AuthorDto.class))).thenReturn(
            updatedAuthorDto);

        mockMvc.perform(patch("/api/v1/authors/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdateDto)))
            .andExpect(status().isOk());

        verify(authorService, times(1)).partialUpdateAuthor(eq(1L), any(AuthorDto.class));
    }

    @Test
    void deleteAuthor_WithValidId_ShouldReturnNoContent() throws Exception {
        doNothing().when(authorService).deleteAuthor(1L);

        mockMvc.perform(delete("/api/v1/authors/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(authorService, times(1)).deleteAuthor(1L);
    }

    @Test
    void deleteAuthor_WithInvalidId_ShouldReturnNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Author not found with id: 99"))
            .when(authorService).deleteAuthor(99L);

        mockMvc.perform(delete("/api/v1/authors/99")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(authorService, times(1)).deleteAuthor(99L);
    }
}