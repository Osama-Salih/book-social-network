package com.osama.book.book;

import com.osama.book.book.request.BookRequest;
import com.osama.book.book.response.BookResponse;
import com.osama.book.book.response.BorrowedBookResponse;
import com.osama.book.common.PageResponse;
import com.osama.book.exception.OperationNotPermittedException;
import com.osama.book.file.FileStorageService;
import com.osama.book.history.BookTransactionRepository;
import com.osama.book.history.TransactionHistory;
import com.osama.book.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookTransactionRepository transactionRepository;
    private final BookMapper bookMapper;
    private final FileStorageService fileStorageService;

    public Integer save(final BookRequest request, final Authentication connectedUser) {
        final User user = (User) connectedUser.getPrincipal();
        final Book book = this.bookMapper.toBook(request);
        book.setOwner(user);
        return this.bookRepository.save(book).getId();
    }

    public BookResponse findById(final Integer bookId) {
        return this.bookRepository.findById(bookId)
                .map(this.bookMapper::toBookResponse)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));
    }

    public PageResponse<BookResponse> findAll(final int page, final int size, final Authentication connectedUser) {
        final User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = this.bookRepository.findAllDisplayableBooks(pageable, user.getId());
        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BookResponse> findAllBooksByOwner(final int page, final int size, final Authentication connectedUser) {
        final User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = this.bookRepository.findAll(BookSpecification.withOwnerId(user.getId()), pageable);
        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(final int page, final int size, final Authentication connectedUser) {
        final User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<TransactionHistory> borrowedBooks = this.transactionRepository.findAllBorrowedBooks(pageable, user.getId());
        List<BorrowedBookResponse> bookResponse = borrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponse,
                borrowedBooks.getNumber(),
                borrowedBooks.getSize(),
                borrowedBooks.getTotalElements(),
                borrowedBooks.getTotalPages(),
                borrowedBooks.isFirst(),
                borrowedBooks.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(final int page, final int size, final Authentication connectedUser) {
        final User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<TransactionHistory> borrowedBooks = this.transactionRepository.findAllReturnedBooks(pageable, user.getId());
        List<BorrowedBookResponse> bookResponse = borrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponse,
                borrowedBooks.getNumber(),
                borrowedBooks.getSize(),
                borrowedBooks.getTotalElements(),
                borrowedBooks.getTotalPages(),
                borrowedBooks.isFirst(),
                borrowedBooks.isLast()
        );
    }

    public Integer updateShareableStatus(final Integer bookId, final Authentication connectedUser) {
        final Book book = this.bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found"));
        final User user = (User) connectedUser.getPrincipal();

        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You can not update others book shareable status");
        }
        book.setShareable(!book.isShareable());
        this.bookRepository.save(book);
        return bookId;
    }

    public Integer updateArchivedStatus(final Integer bookId, final Authentication connectedUser) {
        final Book book = this.bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found"));
        final User user = (User) connectedUser.getPrincipal();

        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You can not update others book archived status");
        }
        book.setArchived(!book.isArchived());
        this.bookRepository.save(book);
        return bookId;
    }

    public Integer borrowBook(final Integer bookId, final Authentication connectedUser) {
        final Book book = this.bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));

        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("You can't borrow this book since it's archived or not shareable");
        }

        final User user = (User) connectedUser.getPrincipal();
        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You can't borrow you own book");
        }

        final boolean isAlreadyBorrowed = this.transactionRepository.isAlreadyBorrowed(bookId, user.getId());
        if (isAlreadyBorrowed) {
            throw new OperationNotPermittedException("The requested book is already borrowed");
        }

        final TransactionHistory transactionHistory = TransactionHistory.builder()
                .book(book)
                .user(user)
                .returned(false)
                .returnedApprove(false)
                .build();
        return this.transactionRepository.save(transactionHistory).getId();
    }

    public Integer borrowedReturn(final Integer bookId, final Authentication connectedUser) {
        final Book book = this.bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));

        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("You can't borrow or return this book since it's archived or not shareable");
        }

        final User user = (User) connectedUser.getPrincipal();
        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You can't borrow or return you own book");
        }

        TransactionHistory returnedBook = this.transactionRepository.findByBookIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("You can't return book that you didn't borrowed"));

        returnedBook.setReturned(true);
        return this.transactionRepository.save(returnedBook).getId();
    }

    public Integer approveReturnBorrowedBook(final Integer bookId, final Authentication connectedUser) {
        final Book book = this.bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));

        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("You can't approve this book since it's archived or not shareable");
        }

        final User user = (User) connectedUser.getPrincipal();
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You only can approve you own books");
        }

        TransactionHistory approvedBook = this.transactionRepository.findByBookIdAndOwnerId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("The book is not returned yet to be approved"));

        approvedBook.setReturnedApprove(true);
        return this.transactionRepository.save(approvedBook).getId();
    }

    public void uploadBookCoverPicture(final MultipartFile file, final Authentication connectedUser, final Integer bookId) {
        final Book book = this.bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));

        final User user = (User) connectedUser.getPrincipal();
        var bookCover = fileStorageService.saveFile(file, user.getId());
        book.setBookCover(bookCover);
        this.bookRepository.save(book);
    }
}