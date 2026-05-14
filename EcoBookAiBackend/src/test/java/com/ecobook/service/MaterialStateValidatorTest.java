package com.ecobook.service;

import com.ecobook.exception.InvalidStateTransitionException;
import com.ecobook.model.enums.StatusMaterial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaterialStateValidatorTest {

    private final MaterialStateValidator validator = new MaterialStateValidator();

    @Test
    @DisplayName("should allow the current runtime material transitions")
    void shouldAllowCurrentRuntimeTransitions() {
        assertThatCode(() -> validator.validateTransition(StatusMaterial.DISPONIVEL, StatusMaterial.RESERVADO))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateTransition(StatusMaterial.RESERVADO, StatusMaterial.DISPONIVEL))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateTransition(StatusMaterial.RESERVADO, StatusMaterial.DOADO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should reject legacy or invalid transitions")
    void shouldRejectInvalidTransitions() {
        assertThatThrownBy(() -> validator.validateTransition(StatusMaterial.DISPONIVEL, StatusMaterial.CANCELADO))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> validator.validateTransition(StatusMaterial.DOADO, StatusMaterial.DISPONIVEL))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> validator.validateTransition(StatusMaterial.CANCELADO, StatusMaterial.RESERVADO))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
