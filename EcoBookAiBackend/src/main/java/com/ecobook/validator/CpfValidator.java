package com.ecobook.validator;

public final class CpfValidator {

    private static final int CPF_LENGTH = 11;

    private CpfValidator() {
    }

    public static String normalize(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("\\D", "");
    }

    public static boolean isValid(String cpf) {
        String normalized = normalize(cpf);
        return normalized != null && normalized.length() == CPF_LENGTH;
    }
}
