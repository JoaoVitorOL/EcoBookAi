package com.ecobook.service;

import com.ecobook.exception.InvalidStateTransitionException;
import com.ecobook.model.enums.StatusMaterial;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class MaterialStateValidator {

    public void requireAvailable(StatusMaterial currentStatus, String context) {
        if (currentStatus != StatusMaterial.DISPONIVEL) {
            throw new InvalidStateTransitionException(context);
        }
    }

    public void validateTransition(StatusMaterial currentStatus, StatusMaterial targetStatus) {
        boolean isValid = switch (currentStatus) {
            case DISPONIVEL -> targetStatus == StatusMaterial.RESERVADO;
            case RESERVADO -> EnumSet.of(StatusMaterial.DISPONIVEL, StatusMaterial.DOADO).contains(targetStatus);
            case DOADO, CANCELADO -> false;
        };

        if (!isValid) {
            throw new InvalidStateTransitionException(
                    "Nao e permitido mudar o material de " + currentStatus + " para " + targetStatus
            );
        }
    }
}
