package com.innercircle.sacco.common.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    /**
     * Concrete subclass for testing the abstract BaseEntity.
     */
    private static class TestEntity extends BaseEntity {
    }

    @Test
    void onCreate_shouldGenerateIdWhenNull() {
        TestEntity entity = new TestEntity();
        assertThat(entity.getId()).isNull();

        entity.onCreate();

        assertThat(entity.getId()).isNotNull();
    }

    @Test
    void onCreate_shouldNotOverrideExistingId() {
        TestEntity entity = new TestEntity();
        UUID existingId = UUID.randomUUID();
        entity.setId(existingId);

        entity.onCreate();

        assertThat(entity.getId()).isEqualTo(existingId);
    }

    @Test
    void onCreate_shouldSetCreatedAt() {
        TestEntity entity = new TestEntity();
        Instant before = Instant.now();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isAfterOrEqualTo(before);
        assertThat(entity.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void onCreate_shouldSetUpdatedAt() {
        TestEntity entity = new TestEntity();
        Instant before = Instant.now();

        entity.onCreate();

        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(before);
        assertThat(entity.getUpdatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void onCreate_createdAtAndUpdatedAtShouldBeEqual() {
        TestEntity entity = new TestEntity();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isEqualTo(entity.getUpdatedAt());
    }

    @Test
    void onUpdate_shouldUpdateUpdatedAt() {
        TestEntity entity = new TestEntity();
        entity.onCreate();

        Instant createdAt = entity.getCreatedAt();
        Instant originalUpdatedAt = entity.getUpdatedAt();

        // Small sleep to ensure time progresses
        Instant before = Instant.now();
        entity.onUpdate();

        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(before);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void onUpdate_shouldNotChangeCreatedAt() {
        TestEntity entity = new TestEntity();
        entity.onCreate();

        Instant createdAt = entity.getCreatedAt();

        entity.onUpdate();

        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void settersAndGetters_shouldWork() {
        TestEntity entity = new TestEntity();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        entity.setId(id);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy("admin");
        entity.setVersion(1L);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        assertThat(entity.getCreatedBy()).isEqualTo("admin");
        assertThat(entity.getVersion()).isEqualTo(1L);
    }

    @Test
    void version_shouldDefaultToNull() {
        TestEntity entity = new TestEntity();

        assertThat(entity.getVersion()).isNull();
    }

    @Test
    void createdBy_shouldDefaultToNull() {
        TestEntity entity = new TestEntity();

        assertThat(entity.getCreatedBy()).isNull();
    }

    @Test
    void id_shouldDefaultToNull() {
        TestEntity entity = new TestEntity();

        assertThat(entity.getId()).isNull();
    }

    @Test
    void multipleOnCreateCalls_shouldNotRegenerateIdIfAlreadySet() {
        TestEntity entity = new TestEntity();
        entity.onCreate();
        UUID firstId = entity.getId();

        entity.onCreate();

        assertThat(entity.getId()).isEqualTo(firstId);
    }
}
