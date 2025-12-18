package org.com.drop.domain.payment.settlement.scheduler;

import org.com.drop.domain.payment.settlement.service.SettlementReleaseService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementReleaseScheduler {

	private final SettlementReleaseService settlementReleaseService;

	@Scheduled(cron = "0 0 3 * * *")
	public void release() {
		log.info("[SETTLEMENT SCHEDULER] start auto release");
		settlementReleaseService.releaseAutomatically();
	}
}
