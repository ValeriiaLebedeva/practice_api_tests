package ui.pages;

import lombok.Getter;

@Getter
public enum BankAlert {
    USER_CREATED_SUCCESSFULLY("✅ User created successfully!"),
    USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS("Username must be between 3 and 15 characters"),
    NEW_ACCOUNT_CREATED("✅ New Account Created! Account Number: "),

    // депозит
    DEPOSIT_SUCCESSFUL("✅ Successfully deposited $%s to account %s!"),
    DEPOSIT_INVALID_AMOUNT("❌ Please enter a valid amount."),
    DEPOSIT_AMOUNT_TOO_BIG("❌ Please deposit less or equal to 5000$."),
    DEPOSIT_SELECT_ACCOUNT("❌ Please select an account."),
    DEPOSIT_FAILED("❌ Failed to deposit. Please try again."),

    // перевод
    TRANSFER_SUCCESSFUL("✅ Successfully transferred $%s to account %s!"),
    TRANSFER_FILL_ALL_FIELDS("❌ Please fill all fields and confirm."),
    TRANSFER_NO_USER_FOUND("❌ No user found with this account number."),
    TRANSFER_NAME_MISMATCH("❌ The recipient name does not match the registered name."),
    TRANSFER_INSUFFICIENT_FUNDS("❌ Error: Invalid transfer: insufficient funds or invalid accounts"),

    // имя профиля
    NAME_UPDATED_SUCCESSFULLY("✅ Name updated successfully!"),
    NAME_INVALID("❌ Please enter a valid name."),
    NAME_SAME_AS_CURRENT("⚠️ New name is the same as the current one."),
    // сообщение бэка выводится в алерте как есть, без эмодзи
    NAME_MUST_CONTAIN_TWO_WORDS("Name must contain two words with letters only");

    private final String message;

    BankAlert(String message) {
        this.message = message;
    }

    // для сообщений с подстановкой значений (%s): сумма, номер аккаунта
    public String format(Object... args) {
        return String.format(message, args);
    }
}
