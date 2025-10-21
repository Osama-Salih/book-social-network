package com.osama.book.feedback;

import com.osama.book.book.Book;
import com.osama.book.book.BookRepository;
import com.osama.book.common.PageResponse;
import com.osama.book.exception.OperationNotPermittedException;
import com.osama.book.feedback.request.FeedbackRequest;
import com.osama.book.feedback.response.FeedbackResponse;
import com.osama.book.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final BookRepository bookRepository;
    private final FeedbackMapper feedbackMapper;
    private final FeedbackRepository feedbackRepository;

    public Integer saveFeedback(final FeedbackRequest request, final Authentication connectedUser) {
        final Book book = this.bookRepository.findById(request.bookId())
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + request.bookId()));

        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("You can't add feedback on this book since it's archived or not shareable");
        }

        final User user = (User) connectedUser.getPrincipal();
        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You can't add feedback on your own book");
        }

        Feedback feedBack = this.feedbackMapper.toFeedback(request);
        return this.feedbackRepository.save(feedBack).getId();
    }

    public PageResponse<FeedbackResponse> findAllFeedbacksByBookId(final Integer bookId, final int page, final int size, final Authentication connectedUser) {
        final Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> feedbacks = this.feedbackRepository.findAllFeedbacksByBookId(bookId, pageable);
        User user = (User) connectedUser.getPrincipal();
        List<FeedbackResponse> feedbackResponse = feedbacks.stream()
                .map(f -> feedbackMapper.toFeedbackResponse(f, user.getId()))
                .toList();

        return new PageResponse<>(
                feedbackResponse,
                feedbacks.getNumber(),
                feedbacks.getSize(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast()
        );
    }
}
