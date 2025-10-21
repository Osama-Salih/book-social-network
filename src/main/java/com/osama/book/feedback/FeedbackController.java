package com.osama.book.feedback;

import com.osama.book.common.PageResponse;
import com.osama.book.feedback.request.FeedbackRequest;
import com.osama.book.feedback.response.FeedbackResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("feedbacks")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "Feedback API")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<Integer> saveFeedback(
            @RequestBody
            @Valid
            final FeedbackRequest request,
            Authentication connectedUser) {
        return ResponseEntity.ok(this.feedbackService.saveFeedback(request, connectedUser));
    }

    @GetMapping("/book/{book-id}")
    public ResponseEntity<PageResponse<FeedbackResponse>> findAllFeedbackByBookId(
            @PathVariable("book-id") Integer bookId,
            Authentication connectedUser,
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size) {
        return ResponseEntity.ok(this.feedbackService.findAllFeedbacksByBookId(bookId, page, size, connectedUser));
    }
}
