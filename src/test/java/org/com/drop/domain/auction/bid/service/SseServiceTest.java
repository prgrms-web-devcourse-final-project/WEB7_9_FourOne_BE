package org.com.drop.domain.auction.bid.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class SseServiceTest {

	private SseService sseService;
	private Map<Long, List<SseEmitter>> sseEmitters;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() throws Exception {
		sseService = new SseService();

		Field field = SseService.class.getDeclaredField("sseEmitters");
		field.setAccessible(true);
		sseEmitters = (Map<Long, List<SseEmitter>>)field.get(sseService);
	}

	@Test
	@DisplayName("구독 시 Emitter가 생성되고 연결 이벤트가 전송되어야 한다")
	void subscribe_success() {
		Long auctionId = 1L;

		SseEmitter emitter = sseService.subscribe(auctionId);

		assertThat(emitter).isNotNull();
		assertThat(sseEmitters.get(auctionId)).contains(emitter);
		assertThat(emitter.getTimeout()).isEqualTo(60 * 60 * 1000L);
	}

	@Test
	@DisplayName("최고가 갱신 시 해당 경매의 모든 구독자에게 알림이 전송되어야 한다")
	void notifyHighestPrice_success() throws IOException {

		Long auctionId = 1L;
		Long price = 50000L;

		SseEmitter mockEmitter = mock(SseEmitter.class);
		sseEmitters.computeIfAbsent(auctionId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(mockEmitter);

		sseService.notifyHighestPrice(auctionId, price);

		verify(mockEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
	}

	@Test
	@DisplayName("전송 중 IOException 발생 시 해당 Emitter는 제거되어야 한다")
	void notifyHighestPrice_fail_ioException() throws IOException {

		Long auctionId = 1L;
		SseEmitter mockEmitter = mock(SseEmitter.class);

		doThrow(IOException.class).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

		sseEmitters.computeIfAbsent(auctionId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(mockEmitter);

		sseService.notifyHighestPrice(auctionId, 1000L);

		assertThat(sseEmitters.get(auctionId)).doesNotContain(mockEmitter);
	}

	@Test
	@DisplayName("onCompletion 콜백 발생 시 Emitter가 리스트에서 제거되어야 한다")
	void onCompletion_removesEmitter() {
		Long auctionId = 1L;
		SseEmitter emitter = sseService.subscribe(auctionId);
		assertThat(sseEmitters.get(auctionId)).hasSize(1);
		emitter.complete();

		assertThat(sseEmitters.get(auctionId)).doesNotContain(emitter);
	}
}
