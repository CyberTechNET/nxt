package nxt;

import nxt.crypto.EncryptedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CoinShuffler {

    private static enum State {
        INITIATED,
        CONTINUED,
        FINALIZED,
        CANCELLED
    }

    private static class Shuffling {

        private final Long shufflingId;
        private final Long currencyId;
        private final long amount;
        private final byte numberOfParticipants;
        private final short maxInitiationDelay;
        private final short maxContinuationDelay;
        private final short maxFinalizationDelay;
        private final short maxCancellationDelay;

        private final int hashCode;

        private State state;
        private int lastActionTimestamp;
        private final List<Long> participants;
        private final Map<Long, EncryptedData> encryptedRecipients;
        private final Map<Long, Long[]> decryptedRecipients;
        private final Map<Long, byte[]> nonces;

        Shuffling(Long shufflingId, Long currencyId, long amount, byte numberOfParticipants, short maxInitiationDelay, short maxContinuationDelay, short maxFinalizationDelay, short maxCancellationDelay) {
            this.shufflingId = shufflingId;
            this.currencyId = currencyId;
            this.amount = amount;
            this.numberOfParticipants = numberOfParticipants;
            this.maxInitiationDelay = maxInitiationDelay;
            this.maxContinuationDelay = maxContinuationDelay;
            this.maxFinalizationDelay = maxFinalizationDelay;
            this.maxCancellationDelay = maxCancellationDelay;

            hashCode = currencyId.hashCode() ^ Long.valueOf(amount).hashCode() ^ Byte.valueOf(numberOfParticipants).hashCode() ^ Short.valueOf(maxInitiationDelay).hashCode() ^ Short.valueOf(maxContinuationDelay).hashCode() ^ Short.valueOf(maxFinalizationDelay).hashCode() ^ Short.valueOf(maxCancellationDelay).hashCode();

            state = State.INITIATED;
            lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
            participants = new ArrayList<>(numberOfParticipants);
            encryptedRecipients = new HashMap<>();
            decryptedRecipients = new HashMap<>();
            nonces = new HashMap<>();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return this.currencyId.equals(((Shuffling)obj).currencyId)
                    && this.amount == ((Shuffling)obj).amount
                    && this.numberOfParticipants == ((Shuffling)obj).numberOfParticipants
                    && this.maxInitiationDelay == ((Shuffling)obj).maxInitiationDelay
                    && this.maxContinuationDelay == ((Shuffling)obj).maxContinuationDelay
                    && this.maxFinalizationDelay == ((Shuffling)obj).maxFinalizationDelay
                    && this.maxCancellationDelay == ((Shuffling)obj).maxCancellationDelay;
        }

        public void addParticipant(Long accountId) {
            participants.add(accountId);
            if (participants.size() == numberOfParticipants) {
                state = State.CONTINUED;
            }
            lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
        }

    }

    private static final Map<Long, Shuffling> shufflings = new HashMap<>();

    public static boolean isInitiated(Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.state == State.INITIATED;
    }

    public static boolean isContinued(Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.state == State.CONTINUED;
    }

    public static boolean isFinalized(Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.state == State.FINALIZED;
    }

    public static boolean isCancelled(Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.state == State.CANCELLED;
    }

    public static byte getNumberOfParticipants(Long shufflingId) {
        return shufflings.get(shufflingId).numberOfParticipants;
    }

    public static boolean isParticipant(Long accountId, Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.participants.contains(accountId);
    }

    public static boolean sentEncryptedRecipients(Long accountId, Long shufflingId) {
        return shufflings.get(shufflingId).encryptedRecipients.get(accountId) != null;
    }

    public static boolean sentDecryptedRecipients(Long accountId, Long shufflingId) {
        return shufflings.get(shufflingId).decryptedRecipients.get(accountId) != null;
    }

    public static boolean sentNonce(Long accountId, Long shufflingId) {
        return shufflings.get(shufflingId).nonces.get(accountId) != null;
    }

    public static void initiateShuffling(Long transactionId, Account account, Long currencyId, long amount, byte numberOfParticipants, short maxInitiationDelay, short maxContinuationDelay, short maxFinalizationDelay, short maxCancellationDelay) {
        account.addToCurrencyBalanceQNT(currencyId, -amount);

        Shuffling newShuffling = new Shuffling(transactionId, currencyId, amount, numberOfParticipants, maxInitiationDelay, maxContinuationDelay, maxFinalizationDelay, maxCancellationDelay);

        for (Shuffling existingShuffling : shufflings.values()) {
            if (existingShuffling.state == State.INITIATED && newShuffling.equals(existingShuffling)) {
                existingShuffling.addParticipant(account.getId());
                return;
            }
        }

        shufflings.put(transactionId, newShuffling);
    }

    public static void continueShuffling(Account account, Long shufflingId, EncryptedData recipients) {
        Shuffling shuffling = shufflings.get(shufflingId);
        shuffling.encryptedRecipients.put(account.getId(), recipients);
        if (shuffling.encryptedRecipients.size() == shuffling.numberOfParticipants) {
            shuffling.state = State.FINALIZED;
        }
        shuffling.lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
    }

    public static void finalizeShuffling(Account account, Long shufflingId, Long[] recipients) {
        // TODO: Implement!
    }

    public static void cancelShuffling(Account account, Long shuffling, byte[] nonce) {
        // TODO: Implement!
    }

}
