package com.innercircle.sacco.common.guard;

import com.innercircle.sacco.common.exception.InvalidStateTransitionException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class TransitionGuard<S extends Enum<S>> {

    private final Map<S, Set<S>> allowedTransitions;
    private final String entityType;

    private TransitionGuard(String entityType, Map<S, Set<S>> allowedTransitions) {
        this.entityType = entityType;
        this.allowedTransitions = allowedTransitions;
    }

    public void validate(S current, S target) {
        if (!isAllowed(current, target)) {
            throw new InvalidStateTransitionException(entityType, current.name(), target.name());
        }
    }

    public boolean isAllowed(S current, S target) {
        Set<S> targets = allowedTransitions.get(current);
        return targets != null && targets.contains(target);
    }

    public Set<S> getAllowedTargets(S current) {
        Set<S> targets = allowedTransitions.get(current);
        return targets != null ? targets : Collections.emptySet();
    }

    public static <S extends Enum<S>> Builder<S> builder(String entityType) {
        return new Builder<>(entityType);
    }

    public static final class Builder<S extends Enum<S>> {

        private final String entityType;
        private final java.util.HashMap<S, EnumSet<S>> transitions = new java.util.HashMap<>();
        private Class<S> enumType;

        private Builder(String entityType) {
            this.entityType = entityType;
        }

        public Builder<S> allow(S from, S to) {
            if (enumType == null) {
                @SuppressWarnings("unchecked")
                Class<S> type = (Class<S>) from.getDeclaringClass();
                enumType = type;
            }
            transitions.computeIfAbsent(from, k -> EnumSet.noneOf(enumType)).add(to);
            return this;
        }

        public TransitionGuard<S> build() {
            if (enumType == null) {
                Map<S, Set<S>> empty = Collections.emptyMap();
                return new TransitionGuard<>(entityType, empty);
            }
            EnumMap<S, Set<S>> immutable = new EnumMap<>(enumType);
            transitions.forEach((key, value) ->
                    immutable.put(key, Collections.unmodifiableSet(EnumSet.copyOf(value))));
            return new TransitionGuard<>(entityType, Collections.unmodifiableMap(immutable));
        }
    }
}
