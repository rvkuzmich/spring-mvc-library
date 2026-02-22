package ru.kuzmich.springmvclibrary.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.kuzmich.springmvclibrary.dto.AuthorDto;
import ru.kuzmich.springmvclibrary.dto.AuthorSummaryDto;

public interface AuthorService {

    Page<AuthorSummaryDto> getAllAuthors(Pageable pageable);

    AuthorDto getAuthorById(Long id);

    AuthorDto getAuthorByEmail(String email);

    AuthorDto createAuthor(AuthorDto authorDTO);

    AuthorDto updateAuthor(Long id, AuthorDto authorDTO);

    AuthorDto partialUpdateAuthor(Long id, AuthorDto authorDTO);

    void deleteAuthor(Long id);
}
