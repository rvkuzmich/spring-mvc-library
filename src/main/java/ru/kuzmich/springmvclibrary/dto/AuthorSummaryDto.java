package ru.kuzmich.springmvclibrary.dto;

import lombok.Data;

@Data
public class AuthorSummaryDto {

    private Long id;
    private String fullName;
    private String email;
    private Integer booksCount;

    public AuthorSummaryDto(Long id, String firstName, String lastName, String email,
        Integer booksCount) {
        this.id = id;
        this.fullName = firstName + " " + lastName;
        this.email = email;
        this.booksCount = booksCount;
    }
}
