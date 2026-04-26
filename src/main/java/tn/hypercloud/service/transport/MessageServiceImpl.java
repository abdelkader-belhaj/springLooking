package tn.hypercloud.service.transport;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.controller.transport.RealTimeTransportController;
import tn.hypercloud.dto.transport.ChatMessageDTO;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.MessageTransport;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.transport.CourseRepository;
import tn.hypercloud.repository.transport.MessageTransportRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageServiceImpl implements IMessageService {

    private final MessageTransportRepository messageRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final RealTimeTransportController realTimeController;

    public MessageServiceImpl(
            MessageTransportRepository messageRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            @Lazy RealTimeTransportController realTimeController) {
        this.messageRepository = messageRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.realTimeController = realTimeController;
    }

    @Override
    @Transactional
    public MessageTransport saveMessage(ChatMessageDTO dto) {
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course non trouvée"));
        User sender = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        MessageTransport message = MessageTransport.builder()
                .course(course)
                .sender(sender)
                .contenu(dto.getContenu())
                .build();
        message = messageRepository.save(message);
        ChatMessageDTO broadcast = ChatMessageDTO.builder()
                .id(message.getId())
                .courseId(dto.getCourseId())
                .senderId(dto.getSenderId())
                .senderRole(dto.getSenderRole())
                .contenu(dto.getContenu())
                .dateEnvoi(message.getDateEnvoi().toString())
                .delivered(true)
                .isRead(false)
                .build();
        realTimeController.broadcastChatMessage(broadcast);
        return message;
    }

    @Override
    public List<MessageTransport> getMessagesByCourse(Long courseId) {
        return messageRepository.findByCourse_IdCourseOrderByDateEnvoiAsc(courseId);
    }

    @Override
    @Transactional
    public void markMessageAsDelivered(Long messageId) {
        messageRepository.markAsDelivered(messageId);
    }

    @Override
    @Transactional
    public void markMessageAsRead(Long messageId) {
        messageRepository.markAsRead(messageId, LocalDateTime.now());
    }
}