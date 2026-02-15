package com.innercircle.sacco.audit.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innercircle.sacco.audit.entity.AuditAction;
import com.innercircle.sacco.audit.entity.AuditEvent;
import com.innercircle.sacco.audit.service.AuditService;
import com.innercircle.sacco.common.event.AuditableEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private AuditService auditService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditEventListener auditEventListener;

    // --- Event type to AuditAction mapping tests ---

    @Test
    void handleAuditableEvent_withCreateEvent_shouldMapToCreateAction() {
        AuditableEvent event = createTestEvent("MEMBER_CREATE", "admin");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("admin"), eq("admin"), eq(AuditAction.CREATE),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withUpdateEvent_shouldMapToUpdateAction() {
        AuditableEvent event = createTestEvent("MEMBER_UPDATE", "user1");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("user1"), eq("user1"), eq(AuditAction.UPDATE),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withDeleteEvent_shouldMapToDeleteAction() {
        AuditableEvent event = createTestEvent("RECORD_DELETE", "admin");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("admin"), eq("admin"), eq(AuditAction.DELETE),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withApproveEvent_shouldMapToApproveAction() {
        AuditableEvent event = createTestEvent("LOAN_APPROVE", "treasurer");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("treasurer"), eq("treasurer"), eq(AuditAction.APPROVE),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withRejectEvent_shouldMapToRejectAction() {
        AuditableEvent event = createTestEvent("LOAN_REJECT", "admin");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("admin"), eq("admin"), eq(AuditAction.REJECT),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withDisburseEvent_shouldMapToDisburseAction() {
        AuditableEvent event = createTestEvent("LOAN_DISBURSE", "admin");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("admin"), eq("admin"), eq(AuditAction.DISBURSE),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withSuspendEvent_shouldMapToSuspendAction() {
        AuditableEvent event = createTestEvent("MEMBER_SUSPEND", "admin");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("admin"), eq("admin"), eq(AuditAction.SUSPEND),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withReactivateEvent_shouldMapToReactivateAction() {
        AuditableEvent event = createTestEvent("MEMBER_REACTIVATE", "admin");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("admin"), eq("admin"), eq(AuditAction.REACTIVATE),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withLoginEvent_shouldMapToLoginAction() {
        AuditableEvent event = createTestEvent("USER_LOGIN", "user1");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("user1"), eq("user1"), eq(AuditAction.LOGIN),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withLogoutEvent_shouldMapToLogoutAction() {
        AuditableEvent event = createTestEvent("USER_LOGOUT", "user1");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("user1"), eq("user1"), eq(AuditAction.LOGOUT),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withConfigEvent_shouldMapToConfigChangeAction() {
        AuditableEvent event = createTestEvent("SYSTEM_CONFIG_CHANGED", "admin");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("admin"), eq("admin"), eq(AuditAction.CONFIG_CHANGE),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withRepaidEvent_shouldMapToRepaidAction() {
        AuditableEvent event = createTestEvent("LOAN_REPAID", "user1");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("user1"), eq("user1"), eq(AuditAction.REPAID),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withRepayEvent_shouldMapToRepaidAction() {
        AuditableEvent event = createTestEvent("LOAN_REPAY", "user1");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("user1"), eq("user1"), eq(AuditAction.REPAID),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withNullEventType_shouldDefaultToUpdate() {
        AuditableEvent event = createTestEvent(null, "system");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("system"), eq("system"), eq(AuditAction.UPDATE),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withUnknownEventType_shouldDefaultToUpdate() {
        AuditableEvent event = createTestEvent("UNKNOWN_TYPE", "admin");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("admin"), eq("admin"), eq(AuditAction.UPDATE),
                any(), any(), any(), any(), isNull());
    }

    // --- Event with fields via reflection ---

    @Test
    void handleAuditableEvent_withEventHavingEntityFields_shouldExtractThem() {
        TestEventWithFields event = new TestEventWithFields(
                "CREATE_MEMBER", "admin",
                "Member", UUID.randomUUID(), null, null);

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("admin"), eq("admin"), eq(AuditAction.CREATE),
                eq("Member"), eq(event.entityId), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_withAfterState_shouldSerializeToJson() throws JsonProcessingException {
        Object afterState = new Object();
        TestEventWithFields event = new TestEventWithFields(
                "CREATE_RECORD", "admin",
                "Member", UUID.randomUUID(), null, afterState);

        when(objectMapper.writeValueAsString(afterState)).thenReturn("{\"name\":\"test\"}");
        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(objectMapper).writeValueAsString(afterState);
        verify(auditService).logEvent(
                eq("admin"), eq("admin"), eq(AuditAction.CREATE),
                eq("Member"), eq(event.entityId), isNull(),
                eq("{\"name\":\"test\"}"), isNull());
    }

    @Test
    void handleAuditableEvent_withJsonSerializationFailure_shouldFallbackToToString() throws JsonProcessingException {
        Object afterState = new Object() {
            @Override
            public String toString() {
                return "fallback-string";
            }
        };
        TestEventWithFields event = new TestEventWithFields(
                "CREATE_RECORD", "admin",
                "Member", UUID.randomUUID(), null, afterState);

        when(objectMapper.writeValueAsString(afterState))
                .thenThrow(new JsonProcessingException("test error") {});
        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("admin"), eq("admin"), eq(AuditAction.CREATE),
                eq("Member"), eq(event.entityId), isNull(),
                eq("fallback-string"), isNull());
    }

    // --- Error handling ---

    @Test
    void handleAuditableEvent_whenAuditServiceThrows_shouldNotPropagateException() {
        AuditableEvent event = createTestEvent("MEMBER_CREATE", "admin");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        // Should not throw
        auditEventListener.handleAuditableEvent(event);
    }

    @Test
    void handleAuditableEvent_withCaseInsensitiveEventType_shouldMapCorrectly() {
        AuditableEvent event = createTestEvent("member_create", "admin");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("admin"), eq("admin"), eq(AuditAction.CREATE),
                any(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_ipAddressShouldBeNull() {
        AuditableEvent event = createTestEvent("MEMBER_CREATE", "admin");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), isNull());
    }

    @Test
    void handleAuditableEvent_actorNameShouldEqualActor() {
        AuditableEvent event = createTestEvent("MEMBER_CREATE", "john@example.com");

        when(auditService.logEvent(anyString(), anyString(), any(AuditAction.class),
                anyString(), any(), any(), any(), any()))
                .thenReturn(AuditEvent.builder().build());

        auditEventListener.handleAuditableEvent(event);

        verify(auditService).logEvent(
                eq("john@example.com"), eq("john@example.com"),
                any(AuditAction.class), any(), any(), any(), any(), isNull());
    }

    // --- Helpers ---

    private AuditableEvent createTestEvent(String eventType, String actor) {
        return new AuditableEvent() {
            @Override
            public String getEventType() {
                return eventType;
            }

            @Override
            public String getActor() {
                return actor;
            }
        };
    }

    /**
     * Test event class with entity fields accessible via reflection.
     */
    static class TestEventWithFields implements AuditableEvent {
        private final String eventTypeValue;
        private final String actorValue;
        @SuppressWarnings("unused")
        private final String entityType;
        final UUID entityId;
        @SuppressWarnings("unused")
        private final Object beforeState;
        @SuppressWarnings("unused")
        private final Object afterState;

        TestEventWithFields(String eventType, String actor,
                            String entityType, UUID entityId,
                            Object beforeState, Object afterState) {
            this.eventTypeValue = eventType;
            this.actorValue = actor;
            this.entityType = entityType;
            this.entityId = entityId;
            this.beforeState = beforeState;
            this.afterState = afterState;
        }

        @Override
        public String getEventType() {
            return eventTypeValue;
        }

        @Override
        public String getActor() {
            return actorValue;
        }
    }
}
