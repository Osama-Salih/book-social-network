package com.osama.book.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookTransactionRepository extends JpaRepository<TransactionHistory, Integer> {

    @Query("""
            SELECT history
            FROM TransactionHistory history
            WHERE history.user.id = :userId
            """)
    Page<TransactionHistory> findAllBorrowedBooks(Pageable pageable, @Param("userId") Integer userId);

    @Query("""
            SELECT history
            FROM TransactionHistory history
            WHERE history.book.owner.id = :userId
            """)
    Page<TransactionHistory> findAllReturnedBooks(Pageable pageable, @Param("userId") Integer userId);

    @Query("""
            SELECT transactionHistory
            (COUNT(*) > 0) AS isBorrowed
            FROM TransactionHistory transactionHistory
            WHERE transactionHistory.user.id =: userId
            AND transactionHistory.book.id =: bookId
            AND transactionHistory.returnedApprove = false
            """)
    boolean isAlreadyBorrowed(final @Param("bookId") Integer bookId, final @Param("userId") Integer userId);


    @Query("""
            SELECT transaction
            FROM TransactionHistory transaction
            WHERE transaction.user.id = :userId
            AND transaction.book.id = :bookId
            AND transaction.returned = false
            AND transaction.returnedApprove = false
            """)
    Optional<TransactionHistory> findByBookIdAndUserId(final @Param("bookId") Integer bookId, final @Param("userId") Integer userId);


    @Query("""
            SELECT transaction
            FROM TransactionHistory transaction
            WHERE transaction.book.owner.id = :OwnerId
            AND transaction.book.id = :bookId
            AND transaction.returned = true
            AND transaction.returnedApprove = false
            """)
    Optional<TransactionHistory> findByBookIdAndOwnerId(final @Param("bookId") Integer bookId, final @Param("OwnerId") Integer OwnerId);
}
