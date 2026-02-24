package ru.kuzmich.springmvclibrary.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kuzmich.springmvclibrary.dto.AuthorDto;
import ru.kuzmich.springmvclibrary.dto.AuthorSummaryDto;
import ru.kuzmich.springmvclibrary.exceptions.DuplicateResourceException;
import ru.kuzmich.springmvclibrary.exceptions.ResourceNotFoundException;
import ru.kuzmich.springmvclibrary.model.Author;
import ru.kuzmich.springmvclibrary.model.Book;
import ru.kuzmich.springmvclibrary.repository.AuthorRepository;
import ru.kuzmich.springmvclibrary.repository.BookRepository;
import ru.kuzmich.springmvclibrary.service.AuthorService;

@Service
@Data
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    private final BookRepository bookRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuthorSummaryDto> getAllAuthors(Pageable pageable) {
        return authorRepository.findAll(pageable)
            .map(author -> new AuthorSummaryDto(
                author.getId(),
                author.getFirstName(),
                author.getLastName(),
                author.getEmail(),
                author.getBooks().size()
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorDto getAuthorById(Long id) {
        Author author = authorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + id));
        return convertToDetailedDto(author);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorDto getAuthorByEmail(String email) {
        Author author = authorRepository.findByEmail(email)
            .orElseThrow(
                () -> new ResourceNotFoundException("Author not found with email: " + email));
        return convertToDetailedDto(author);
    }

    @Override
    @Transactional
    public AuthorDto createAuthor(AuthorDto authorDTO) {
        if (authorRepository.findByEmail(authorDTO.getEmail()).isPresent()) {
            throw new DuplicateResourceException(
                "Author with email " + authorDTO.getEmail() + " already exists");
        }

        Author author = convertToEntity(authorDTO);
        Author savedAuthor = authorRepository.save(author);
        return convertToDetailedDto(savedAuthor);
    }

    @Override
    @Transactional
    public AuthorDto updateAuthor(Long id, AuthorDto authorDTO) {
        Author author = authorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + id));

        if (!author.getEmail().equals(authorDTO.getEmail()) &&
            authorRepository.findByEmail(authorDTO.getEmail()).isPresent()) {
            throw new DuplicateResourceException(
                "Author with email " + authorDTO.getEmail() + " already exists");
        }

        updateAuthorFromDTO(author, authorDTO);
        Author updatedAuthor = authorRepository.save(author);
        return convertToDetailedDto(updatedAuthor);
    }

    @Override
    @Transactional
    public AuthorDto partialUpdateAuthor(Long id, AuthorDto authorDTO) {
        Author author = authorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + id));

        if (authorDTO.getFirstName() != null) {
            author.setFirstName(authorDTO.getFirstName());
        }

        if (authorDTO.getLastName() != null) {
            author.setLastName(authorDTO.getLastName());
        }

        if (authorDTO.getEmail() != null && !author.getEmail().equals(authorDTO.getEmail())) {
            if (authorRepository.findByEmail(authorDTO.getEmail()).isPresent()) {
                throw new DuplicateResourceException(
                    "Author with email " + authorDTO.getEmail() + " already exists");
            }
            author.setEmail(authorDTO.getEmail());
        }

        Author updatedAuthor = authorRepository.save(author);
        return convertToDetailedDto(updatedAuthor);
    }

    @Override
    @Transactional
    public void deleteAuthor(Long id) {
        Author author = authorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + id));

        authorRepository.delete(author);
    }


    private AuthorDto convertToDetailedDto(Author author) {
        AuthorDto dto = new AuthorDto();
        dto.setId(author.getId());
        dto.setFirstName(author.getFirstName());
        dto.setLastName(author.getLastName());
        dto.setEmail(author.getEmail());

        List<Long> bookIds = author.getBooks().stream()
            .map(Book::getId)
            .collect(Collectors.toList());
        dto.setBookIds(bookIds);

        List<String> bookTitles = author.getBooks().stream()
            .map(Book::getTitle)
            .collect(Collectors.toList());
        dto.setBookTitles(bookTitles);

        dto.setBooksCount(author.getBooks().size());

        return dto;
    }

    private Author convertToEntity(AuthorDto dto) {
        Author author = new Author();
        author.setFirstName(dto.getFirstName());
        author.setLastName(dto.getLastName());
        author.setEmail(dto.getEmail());
        return author;
    }

    private void updateAuthorFromDTO(Author author, AuthorDto dto) {
        author.setFirstName(dto.getFirstName());
        author.setLastName(dto.getLastName());
        author.setEmail(dto.getEmail());
    }
}
