package com.osama.book.book;

import org.springframework.data.jpa.domain.Specification;

public class BookSpecification {

    public static Specification<Book> withOwnerId(final Integer ownerId) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.equal(root.get("owner").get("id"), ownerId);
    }
}
