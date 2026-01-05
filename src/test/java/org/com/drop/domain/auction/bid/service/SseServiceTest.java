package org.com.drop.domain.auction.bid.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
	@Autowired
	private SseService sseService;
	private Map<Long, List<SseEmitter>> sseEmitters;
	private Long auctionId = 1L;
	private Long price = Long.MAX_VALUE;

	@BeforeEach
	void setUp() throws Exception {
		sseService = new SseService();
		Field field = SseService.class.getDeclaredField("sseEmitters");
		field.setAccessible(true);
		sseEmitters = (Map<Long, List<SseEmitter>>) field.get(sseService);
	}

	@Nested
	class SubscribeTest {
		@Test
		@DisplayName("알림 구독-성공")
		void t1() throws Exception {
			SseEmitter emitter = sseService.subscribe(auctionId);

			assertThat(emitter).isNotNull();
			assertThat(sseEmitters.get(auctionId)).contains(emitter);
			assertThat(emitter.getTimeout()).isEqualTo(60 * 60 * 1000L);
		}

		@Test
		@DisplayName("알림 구독-실패- auctionId가 null")
		void t1_1() {
			assertThatThrownBy(() -> sseService.subscribe(null))
				.isInstanceOf(NullPointerException.class);
		}
	}

	@Nested
	class NotifyHighestPriceTest {
		@Test
		@DisplayName("최고가 알림-성공")
		void t2() throws Exception {
			SseEmitter emitter = spy(new SseEmitter());
			sseEmitters.put(auctionId, new CopyOnWriteArrayList<>(List.of(emitter)));

			sseService.notifyHighestPrice(auctionId, price);

			verify(emitter, times(1))
				.send(any(SseEmitter.SseEventBuilder.class));

			assertThat(sseEmitters.get(auctionId)).contains(emitter);
		}

		@Test
		@DisplayName("최고가 알림-실패-emitter 비었음")
		void t2_1() throws Exception {
			sseEmitters.put(auctionId, new CopyOnWriteArrayList<>());

			assertThatCode(() ->
				sseService.notifyHighestPrice(auctionId, price)
			).doesNotThrowAnyException();
		}
	}

	@Nested
	class RemoveTest {
		@Test
		@DisplayName("Emitter 완료 콜백 - 성공")
		void t3() {
			SseEmitter emitter = sseService.subscribe(auctionId);

			Runnable callback = (Runnable) org.springframework.test.util.ReflectionTestUtils
				.getField(emitter, "completionCallback");

			if (callback != null) callback.run();

			assertThat(sseEmitters.get(auctionId)).doesNotContain(emitter);
		}

		@Test
		@DisplayName("Emitter가 제거-전송중 오류 발생")
		void t4() throws Exception {
			SseEmitter mockEmitter = mock(SseEmitter.class);
			doThrow(IOException.class).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

			sseEmitters.put(auctionId, new CopyOnWriteArrayList<>(List.of(mockEmitter)));

			sseService.notifyHighestPrice(auctionId, price);

			assertThat(sseEmitters.get(auctionId)).doesNotContain(mockEmitter);
		}
	}
}
