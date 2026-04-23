package tn.hypercloud.service.transport;

import tn.hypercloud.dto.transport.ChatMessageDTO;
import tn.hypercloud.entity.transport.MessageTransport;
import java.util.List;

public interface IMessageService {

    MessageTransport saveMessage(ChatMessageDTO dto);

    List<MessageTransport> getMessagesByCourse(Long courseId);
    void markMessageAsDelivered(Long messageId);
    void markMessageAsRead(Long messageId);
}