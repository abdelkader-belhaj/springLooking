package tn.hypercloud.service.event;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.event.EventReviewRequest;
import tn.hypercloud.dto.event.EventReviewResponse;
import tn.hypercloud.entity.event.EventActivity;
import tn.hypercloud.entity.event.EventReservation;
import tn.hypercloud.entity.event.EventReview;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.exception.GlobalExceptionHandler.ApiException;
import tn.hypercloud.repository.event.EventActivityRepository;
import tn.hypercloud.repository.event.EventReservationRepository;
import tn.hypercloud.repository.event.EventReviewRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventReviewService {

    private final EventReviewRepository reviewRepository;
    private final EventActivityRepository eventRepository;
    private final EventReservationRepository reservationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<EventReviewResponse> getByEvent(Integer eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Event not found : " + eventId);
        }

        return reviewRepository.findByEventIdOrderByUpdatedAtDesc(eventId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventReviewResponse submit(Integer eventId, EventReviewRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != Role.CLIENT_TOURISTE) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Seul un client peut publier un avis");
        }

        EventActivity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found : " + eventId));

        if (event.getEndDate() == null || event.getEndDate().isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Vous pourrez publier votre avis après la fin de l'événement");
        }

        boolean hasConfirmedReservation = reservationRepository.existsByEventIdAndUserIdAndStatus(
                eventId,
                user.getId(),
                EventReservation.ReservationStatus.CONFIRMED
        );

        if (!hasConfirmedReservation) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Avis autorisé uniquement après paiement confirmé pour cet événement");
        }

        EventReview review = reviewRepository.findByEventIdAndUserId(eventId, user.getId())
                .orElseGet(() -> EventReview.builder()
                        .event(event)
                        .user(user)
                        .build());

        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());

        return toResponse(reviewRepository.save(review));
    }

    private EventReviewResponse toResponse(EventReview review) {
        return EventReviewResponse.builder()
                .id(review.getId())
                .eventId(review.getEvent() != null ? review.getEvent().getId() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userName(review.getUser() != null ? review.getUser().getFullName() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
