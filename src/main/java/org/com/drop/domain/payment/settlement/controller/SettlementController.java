package org.com.drop.domain.payment.settlement.controller;

import org.com.drop.domain.payment.settlement.service.SettlementReleaseService;
import org.com.drop.global.rsdata.RsData;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class SettlementController {

	private final SettlementReleaseService settlementReleaseService;

	@PostMapping("/confirm")
	public RsData<Void> confirm(@RequestParam Long paymentId) {
		settlementReleaseService.releaseByPurchaseConfirm(paymentId);
		return new RsData<>(null);
	}
}

