package org.com.drop.domain.payment.method.service;

import java.util.List;

import org.com.drop.domain.payment.method.domain.PaymentMethod;
import org.com.drop.domain.payment.method.dto.CardResponse;
import org.com.drop.domain.payment.method.dto.RegisterCardRequest;
import org.com.drop.domain.payment.method.repository.PaymentMethodRepository;
import org.com.drop.domain.payment.payment.infra.toss.util.CustomerKeyGenerator;
import org.com.drop.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentMethodService {

	private final PaymentMethodRepository paymentMethodRepository;
	private final CustomerKeyGenerator customerKeyGenerator;

	public void registerCard(Long userId, RegisterCardRequest request) {

		if (paymentMethodRepository.existsByBillingKey(request.billingKey())) {
			throw ErrorCode.PAY_ALREADY_PROCESSED.serviceException("이미 등록된 카드입니다.");
		}

		String customerKey =
			customerKeyGenerator.generate("user:" + userId);

		PaymentMethod method = PaymentMethod.builder()
			.userId(userId)
			.cardCompany(request.cardCompany())
			.customerKey(customerKey)
			.billingKey(request.billingKey())
			.cardNumberMasked(request.cardNumberMasked())
			.cardName(request.cardName())
			.build();

		paymentMethodRepository.save(method);
	}

	@Transactional(readOnly = true)
	public List<CardResponse> getCards(Long userId) {
		return paymentMethodRepository.findByUserId(userId)
			.stream()
			.map(CardResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public PaymentMethod getPrimaryMethod(Long userId) {

		return paymentMethodRepository.findByUserId(userId).stream()
			.findFirst()
			.orElseThrow(() ->
				ErrorCode.USER_PAYMENT_NOT_FOUND
					.serviceException("등록된 결제 수단이 없습니다.")
			);
	}

	public void deleteCard(Long userId, Long cardId) {

		PaymentMethod method = paymentMethodRepository.findById(cardId)
			.orElseThrow(() ->
				ErrorCode.USER_PAYMENT_NOT_FOUND.serviceException("등록된 결제 수단이 없습니다.")
			);

		if (!method.getUserId().equals(userId)) {
			throw ErrorCode.USER_UNAUTHORIZED
				.serviceException("본인 카드만 삭제할 수 있습니다.");
		}

		paymentMethodRepository.delete(method);
	}
}
