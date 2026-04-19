package tn.hypercloud.service.forum;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.forum.Forum;
import tn.hypercloud.entity.forum.Review;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.forum.ForumRepository;
import tn.hypercloud.repository.forum.ReviewRepository;
import tn.hypercloud.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ForumRepository forumRepository;

    public Review create(Long forumId, Review review) {
        User user = SecurityUtils.getCurrentUser();

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.CLIENT_TOURISTE) {
            throw new RuntimeException("Accès refusé");
        }

        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new RuntimeException("Forum introuvable"));

        review.setForum(forum);
        review.setUser(user);
        review.setCreatedAt(LocalDateTime.now());

        return reviewRepository.save(review);
    }

    public List<Review> getAll() {
        return reviewRepository.findAll();
    }

    public Review getById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review introuvable"));
    }

    public Review update(Long id, Review updated) {
        User user = SecurityUtils.getCurrentUser();
        Review review = getById(id);

        boolean isOwner = review.getUser() != null && review.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Accès refusé");
        }

        review.setRating(updated.getRating());
        review.setComment(updated.getComment());
        review.setStatus(updated.getStatus());

        return reviewRepository.save(review);
    }

    public void delete(Long id) {
        User user = SecurityUtils.getCurrentUser();
        Review review = getById(id);

        boolean isOwner = review.getUser() != null && review.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Accès refusé");
        }

        reviewRepository.delete(review);
    }
    public List<Review> getByForum(Long forumId) {
        return reviewRepository.findByForumId(forumId);
    }
}