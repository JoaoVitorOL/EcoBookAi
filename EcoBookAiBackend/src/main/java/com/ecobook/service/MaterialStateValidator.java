package com.ecobook.service;

import com.ecobook.exception.InvalidStateTransitionException;
import com.ecobook.model.enums.StatusMaterial;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class MaterialStateValidator {

    /**
     * Verifies that a material is currently available for the requested operation.
     * @param currentStatus c ur re nt st at us
     * @param context c on te xt
     */
    public void requireAvailable(StatusMaterial currentStatus, String context) {
        if (currentStatus != StatusMaterial.DISPONIVEL) {
            throw new InvalidStateTransitionException(context);
        }
    }

    /**
     * Validates a material status transition before it is persisted.
     * @param currentStatus c ur re nt st at us
     * @param targetStatus t ar ge ts ta tu s
     */
    public void validateTransition(StatusMaterial currentStatus, StatusMaterial targetStatus) {
        boolean isValid = switch (currentStatus) {
            case DISPONIVEL -> targetStatus == StatusMaterial.RESERVADO;
            case RESERVADO -> EnumSet.of(StatusMaterial.DISPONIVEL, StatusMaterial.DOADO).contains(targetStatus);
            case DOADO, CANCELADO -> false;
        };

        if (!isValid) {
            throw new InvalidStateTransitionException(
                    "Não é permitido mudar o material de " + currentStatus + " para " + targetStatus
            );
        }
    }
}
