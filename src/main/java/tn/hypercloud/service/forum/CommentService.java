package tn.hypercloud.service.forum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.hypercloud.entity.forum.Forum;
import tn.hypercloud.entity.forum.ForumComment;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.forum.ForumCommentRepository;
import tn.hypercloud.repository.forum.ForumRepository;
import tn.hypercloud.security.SecurityUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final ForumCommentRepository commentRepository;
    private final ForumRepository forumRepository;

    public ForumComment create(Long forumId, ForumComment comment) {
        User user = SecurityUtils.getCurrentUser();

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.CLIENT_TOURISTE) {
            throw new RuntimeException("Accès refusé");
        }

        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new RuntimeException("Forum introuvable"));

        comment.setForum(forum);
        comment.setUser(user);
        comment.setCreatedAt(LocalDateTime.now());

        return commentRepository.save(comment);
    }

    public List<ForumComment> getAll() {
        return commentRepository.findAll();
    }

    public ForumComment getById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commentaire introuvable"));
    }

    public ForumComment update(Long id, ForumComment updated) {
        User user = SecurityUtils.getCurrentUser();
        ForumComment comment = getById(id);

        boolean isOwner = comment.getUser() != null && comment.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Accès refusé");
        }

        comment.setContent(updated.getContent());
        return commentRepository.save(comment);
    }

    public void delete(Long id) {
        User user = SecurityUtils.getCurrentUser();

        // Si le commentaire n'existe pas → on ignore sans erreur
        ForumComment comment = commentRepository.findById(id).orElse(null);
        if (comment == null) return;

        boolean isOwner = comment.getUser() != null && comment.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Accès refusé");
        }

        commentRepository.delete(comment);
    }
    public List<ForumComment> getByForum(Long forumId) {
        return commentRepository.findByForumId(forumId);
    }

    // 🎤 Voice Comment Processing
    public ForumComment createVoiceComment(Long forumId, MultipartFile audioFile) throws IOException {
        User user = SecurityUtils.getCurrentUser();

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.CLIENT_TOURISTE) {
            throw new RuntimeException("Accès refusé");
        }

        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new RuntimeException("Forum introuvable"));

        // Generate filename
        String fileName = generateVoiceFileName(user.getId(), audioFile.getOriginalFilename());
        
        // Save file
        String voiceUrl = saveVoiceFile(audioFile, fileName);

        // Create ForumComment with voice properties
        ForumComment voiceComment = ForumComment.builder()
                .content("Message vocal")
                .voiceUrl(voiceUrl)
                .voiceDuration(0) // Will be extracted from audio on frontend
                .forum(forum)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        return commentRepository.save(voiceComment);
    }

    private String generateVoiceFileName(Long userId, String originalFileName) {
        return String.format("voice_%d_%d.webm", userId, System.currentTimeMillis());
    }

    private String saveVoiceFile(MultipartFile file, String fileName) throws IOException {
        // Create voice-messages directory if not exists
        Path uploadDir = Paths.get("uploads/voice-messages");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Save file
        Path filePath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        // Return absolute URL
        return String.format("http://localhost:8080/uploads/voice-messages/%s", fileName);
    }
}