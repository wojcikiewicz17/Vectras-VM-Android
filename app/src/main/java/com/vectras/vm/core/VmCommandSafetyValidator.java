package com.vectras.vm.core;

import java.util.regex.Pattern;

/**
 * Regras puras de validação de comando QEMU, sem dependência de UI/Android.
 */
public final class VmCommandSafetyValidator {

    private static final Pattern SAFE_COMMAND_CHARS = Pattern.compile("^[a-zA-Z0-9_./,:=+\\-\"' ]+$");

    private VmCommandSafetyValidator() {
    }

    public enum Reason {
        OK,
        EMPTY,
        NOT_QEMU,
        ILLEGAL_CHARS,
        HAS_CONTROL_OPERATOR,
        HAS_AMPERSAND,
        HAS_NEWLINE,
        HAS_SEMICOLON,
        HAS_PIPE
    }

    public static final class ValidationResult {
        public final boolean safe;
        public final Reason reason;

        private ValidationResult(boolean safe, Reason reason) {
            this.safe = safe;
            this.reason = reason;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, Reason.OK);
        }

        public static ValidationResult fail(Reason reason) {
            return new ValidationResult(false, reason);
        }
    }

    public static ValidationResult validateQemuCommand(String rawCommand) {
        String command = rawCommand == null ? "" : rawCommand.trim();
        if (command.isEmpty()) {
            return ValidationResult.fail(Reason.EMPTY);
        }

        if (!SAFE_COMMAND_CHARS.matcher(command).matches()) {
            return ValidationResult.fail(Reason.ILLEGAL_CHARS);
        }

        if (command.contains("&&") || command.contains("||") || command.contains("$")
                || command.contains("`") || command.contains("<") || command.contains(">")
                || command.contains("(") || command.contains(")")) {
            return ValidationResult.fail(Reason.HAS_CONTROL_OPERATOR);
        }

        if (!command.startsWith("qemu")) {
            return ValidationResult.fail(Reason.NOT_QEMU);
        }

        if (command.contains("&")) {
            return ValidationResult.fail(Reason.HAS_AMPERSAND);
        }

        if (command.contains("\n")) {
            return ValidationResult.fail(Reason.HAS_NEWLINE);
        }

        if (command.contains(";")) {
            return ValidationResult.fail(Reason.HAS_SEMICOLON);
        }

        if (command.contains("|")) {
            return ValidationResult.fail(Reason.HAS_PIPE);
        }

        return ValidationResult.ok();
    }
}
