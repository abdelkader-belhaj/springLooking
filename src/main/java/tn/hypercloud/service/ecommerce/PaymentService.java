package tn.hypercloud.service.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.ecommerce.PaymentDTO;
import tn.hypercloud.entity.ecommerce.Order;
import tn.hypercloud.entity.ecommerce.Payment;
import tn.hypercloud.repository.ecommerce.PaymentRepository;
import tn.hypercloud.repository.ecommerce.OrderRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentDTO createPayment(PaymentDTO paymentDTO) {
        Order order = orderRepository.findById(paymentDTO.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + paymentDTO.getOrderId()));
        
        Payment payment = Payment.builder()
                .order(order)
                .stripePaymentIntentId(paymentDTO.getStripePaymentIntentId())
                .amount(paymentDTO.getAmount())
                .status(Payment.PaymentStatus.valueOf(paymentDTO.getStatus()))
                .build();
        Payment saved = paymentRepository.save(payment);
        return mapToDTO(saved);
    }

    public PaymentDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        return mapToDTO(payment);
    }

    public List<PaymentDTO> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public PaymentDTO updatePayment(Long id, PaymentDTO paymentDTO) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        
        payment.setStripePaymentIntentId(paymentDTO.getStripePaymentIntentId());
        payment.setAmount(paymentDTO.getAmount());
        payment.setStatus(Payment.PaymentStatus.valueOf(paymentDTO.getStatus()));
        payment.setPaidAt(paymentDTO.getPaidAt());
        
        Payment updated = paymentRepository.save(payment);
        return mapToDTO(updated);
    }

    public void deletePayment(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new RuntimeException("Payment not found with id: " + id);
        }
        paymentRepository.deleteById(id);
    }

    private PaymentDTO mapToDTO(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .amount(payment.getAmount())
                .status(payment.getStatus().toString())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
