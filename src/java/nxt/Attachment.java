package nxt;

import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;

public interface Attachment {

    public int getSize();
    public byte[] getBytes();
    public JSONObject getJSONObject();

    TransactionType getTransactionType();


    public final static class MessagingArbitraryMessage implements Attachment {

        private final byte[] message;

        public MessagingArbitraryMessage(byte[] message) {
            this.message = message;
        }

        @Override
        public int getSize() {
            return 4 + message.length;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(message.length);
            buffer.put(message);

            return buffer.array();
        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("message", message == null ? null : Convert.toHexString(message));

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ARBITRARY_MESSAGE;
        }

        public byte[] getMessage() {
            return message;
        }
    }

    public final static class MessagingEncryptedMessage implements Attachment {

        private final EncryptedData encryptedMessage;

        public MessagingEncryptedMessage(EncryptedData encryptedMessage) {
            this.encryptedMessage = encryptedMessage;
        }

        @Override
        public int getSize() {
            return 2 + encryptedMessage.getData().length + encryptedMessage.getNonce().length;
        }

        @Override
        public byte[] getBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putShort((short)encryptedMessage.getData().length);
            buffer.put(encryptedMessage.getData());
            buffer.put(encryptedMessage.getNonce());
            return buffer.array();
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("message", Convert.toHexString(encryptedMessage.getData()));
            attachment.put("nonce", Convert.toHexString(encryptedMessage.getNonce()));
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ENCRYPTED_MESSAGE;
        }

        public EncryptedData getEncryptedMessage() {
            return encryptedMessage;
        }

    }

    public final static class MessagingAliasAssignment implements Attachment {

        private final String aliasName;
        private final String aliasURI;

        public MessagingAliasAssignment(String aliasName, String aliasURI) {

            this.aliasName = aliasName.trim().intern();
            this.aliasURI = aliasURI.trim().intern();

        }

        @Override
        public int getSize() {
            return 1 + Convert.toBytes(aliasName).length + 2 + Convert.toBytes(aliasURI).length;
        }

        @Override
        public byte[] getBytes() {
            byte[] alias = Convert.toBytes(this.aliasName);
            byte[] uri = Convert.toBytes(this.aliasURI);

            ByteBuffer buffer = ByteBuffer.allocate(1 + alias.length + 2 + uri.length);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put((byte)alias.length);
            buffer.put(alias);
            buffer.putShort((short)uri.length);
            buffer.put(uri);
            return buffer.array();
        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("alias", aliasName);
            attachment.put("uri", aliasURI);

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_ASSIGNMENT;
        }

        public String getAliasName() {
            return aliasName;
        }

        public String getAliasURI() {
            return aliasURI;
        }
    }

    public final static class MessagingAliasSell implements Attachment {

        private final String aliasName;
        private final long priceNQT;

        public MessagingAliasSell(String aliasName, long priceNQT) {
            this.aliasName = aliasName;
            this.priceNQT = priceNQT;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_SELL;
        }

        @Override
        public int getSize() {
            return 1 + Convert.toBytes(aliasName).length + 8;
        }

        @Override
        //todo: fix ???
        public byte[] getBytes() {
            byte[] aliasBytes = Convert.toBytes(aliasName);

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put((byte)aliasBytes.length);
            buffer.put(aliasBytes);
            buffer.putLong(priceNQT);
            return buffer.array();
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("alias", aliasName);
            attachment.put("priceNQT", priceNQT);
            return attachment;
        }

        public String getAliasName(){
            return aliasName;
        }

        public long getPriceNQT(){
            return priceNQT;
        }
    }

    public final static class MessagingAliasBuy implements Attachment {

        private final String aliasName;

        public MessagingAliasBuy(String aliasName) {
            this.aliasName = aliasName;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_BUY;
        }

        @Override
        public int getSize() {
            return 1 + Convert.toBytes(aliasName).length;
        }

        @Override
        //todo: fix ???
        public byte[] getBytes() {
            byte[] aliasBytes = Convert.toBytes(aliasName);

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put((byte)aliasBytes.length);
            buffer.put(aliasBytes);
            return buffer.array();
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("alias", aliasName);
            return attachment;
        }

        public String getAliasName(){
            return aliasName;
        }
    }

    public final static class MessagingPollCreation implements Attachment {

        private final String pollName;
        private final String pollDescription;
        private final String[] pollOptions;
        private final byte minNumberOfOptions, maxNumberOfOptions;
        private final boolean optionsAreBinary;

        public MessagingPollCreation(String pollName, String pollDescription, String[] pollOptions, byte minNumberOfOptions, byte maxNumberOfOptions, boolean optionsAreBinary) {

            this.pollName = pollName;
            this.pollDescription = pollDescription;
            this.pollOptions = pollOptions;
            this.minNumberOfOptions = minNumberOfOptions;
            this.maxNumberOfOptions = maxNumberOfOptions;
            this.optionsAreBinary = optionsAreBinary;

        }

        @Override
        public int getSize() {
            int size = 2 + Convert.toBytes(pollName).length + 2 + Convert.toBytes(pollDescription).length + 1;
            for (String pollOption : pollOptions) {
                size += 2 + Convert.toBytes(pollOption).length;
            }
            size +=  1 + 1 + 1;
            return size;
        }

        @Override
        public byte[] getBytes() {
            byte[] name = Convert.toBytes(this.pollName);
            byte[] description = Convert.toBytes(this.pollDescription);
            byte[][] options = new byte[this.pollOptions.length][];
            for (int i = 0; i < this.pollOptions.length; i++) {
                options[i] = Convert.toBytes(this.pollOptions[i]);
            }

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putShort((short)name.length);
            buffer.put(name);
            buffer.putShort((short)description.length);
            buffer.put(description);
            buffer.put((byte)options.length);
            for (byte[] option : options) {
                buffer.putShort((short) option.length);
                buffer.put(option);
            }
            buffer.put(this.minNumberOfOptions);
            buffer.put(this.maxNumberOfOptions);
            buffer.put(this.optionsAreBinary ? (byte)1 : (byte)0);

            return buffer.array();
        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("name", this.pollName);
            attachment.put("description", this.pollDescription);
            JSONArray options = new JSONArray();
            if (this.pollOptions != null) {
                Collections.addAll(options, this.pollOptions);
            }
            attachment.put("options", options);
            attachment.put("minNumberOfOptions", this.minNumberOfOptions);
            attachment.put("maxNumberOfOptions", this.maxNumberOfOptions);
            attachment.put("optionsAreBinary", this.optionsAreBinary);

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.POLL_CREATION;
        }

        public String getPollName() { return pollName; }

        public String getPollDescription() { return pollDescription; }

        public String[] getPollOptions() { return pollOptions; }

        public byte getMinNumberOfOptions() { return minNumberOfOptions; }

        public byte getMaxNumberOfOptions() { return maxNumberOfOptions; }

        public boolean isOptionsAreBinary() { return optionsAreBinary; }

    }

    public final static class MessagingVoteCasting implements Attachment {

        private final Long pollId;
        private final byte[] pollVote;

        public MessagingVoteCasting(Long pollId, byte[] pollVote) {

            this.pollId = pollId;
            this.pollVote = pollVote;

        }

        @Override
        public int getSize() {
            return 8 + 1 + this.pollVote.length;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(this.pollId);
            buffer.put((byte)this.pollVote.length);
            buffer.put(this.pollVote);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("pollId", Convert.toUnsignedLong(this.pollId));
            JSONArray vote = new JSONArray();
            if (this.pollVote != null) {
                for (byte aPollVote : this.pollVote) {
                    vote.add(aPollVote);
                }
            }
            attachment.put("vote", vote);
            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.VOTE_CASTING;
        }

        public Long getPollId() { return pollId; }

        public byte[] getPollVote() { return pollVote; }

    }

    public final static class MessagingHubAnnouncement implements Attachment {

        private final long minFeePerByteNQT;
        private final String[] uris;

        public MessagingHubAnnouncement(long minFeePerByteNQT, String[] uris) {
            this.minFeePerByteNQT = minFeePerByteNQT;
            this.uris = uris;
        }

        @Override
        public int getSize() {
            int size = 8 + 1;
            for (String uri : uris) {
                size += 2 + Convert.toBytes(uri).length;
            }
            return size;
        }

        @Override
        public byte[] getBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(minFeePerByteNQT);
            buffer.put((byte) uris.length);
            for (String uri : uris) {
                byte[] uriBytes = Convert.toBytes(uri);
                buffer.putShort((short)uriBytes.length);
                buffer.put(uriBytes);
            }
            return buffer.array();
        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("minFeePerByteNQT", minFeePerByteNQT);
            JSONArray uris = new JSONArray();
            Collections.addAll(uris, this.uris);
            attachment.put("uris", uris);
            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.HUB_ANNOUNCEMENT;
        }

        public long getMinFeePerByteNQT() {
            return minFeePerByteNQT;
        }

        public String[] getUris() {
            return uris;
        }

    }

    public final static class MessagingAccountInfo implements Attachment {

        private final String name;
        private final String description;

        public MessagingAccountInfo(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public int getSize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length;
        }

        @Override
        public byte[] getBytes() {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);

            ByteBuffer buffer = ByteBuffer.allocate(1 + name.length + 2 + description.length);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put((byte)name.length);
            buffer.put(name);
            buffer.putShort((short)description.length);
            buffer.put(description);
            return buffer.array();
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("name", name);
            attachment.put("description", description);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ACCOUNT_INFO;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

    }

    public final static class ColoredCoinsAssetIssuance implements Attachment {

        private final String name;
        private final String description;
        private final long quantityQNT;
        private final byte decimals;

        public ColoredCoinsAssetIssuance(String name, String description, long quantityQNT, byte decimals) {

            this.name = name;
            this.description = Convert.nullToEmpty(description);
            this.quantityQNT = quantityQNT;
            this.decimals = decimals;

        }

        @Override
        public int getSize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 8 + 1;
        }

        @Override
        public byte[] getBytes() {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);

            ByteBuffer buffer = ByteBuffer.allocate(1 + name.length + 2 + description.length + 8 + 1);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put((byte)name.length);
            buffer.put(name);
            buffer.putShort((short)description.length);
            buffer.put(description);
            buffer.putLong(quantityQNT);
            buffer.put(decimals);
            return buffer.array();
        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("quantityQNT", quantityQNT);
            attachment.put("decimals", decimals);

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_ISSUANCE;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public byte getDecimals() {
            return decimals;
        }
    }

    public final static class ColoredCoinsAssetTransfer implements Attachment {

        private final Long assetId;
        private final long quantityQNT;
        private final String comment;

        public ColoredCoinsAssetTransfer(Long assetId, long quantityQNT, String comment) {
            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.comment = Convert.nullToEmpty(comment);
        }

        @Override
        public int getSize() {
            return 8 + 8 + 2 + Convert.toBytes(comment).length;
        }

        @Override
        public byte[] getBytes() {
            byte[] commentBytes = Convert.toBytes(this.comment);
            ByteBuffer buffer = ByteBuffer.allocate(8 + 8 + 2 + commentBytes.length);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(Convert.nullToZero(assetId));
            buffer.putLong(quantityQNT);
            buffer.putShort((short) commentBytes.length);
            buffer.put(commentBytes);
            return buffer.array();
        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("asset", Convert.toUnsignedLong(assetId));
            attachment.put("quantityQNT", quantityQNT);
            attachment.put("comment", comment);

            return attachment;

        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_TRANSFER;
        }

        public Long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public String getComment() {
            return comment;
        }

    }

    abstract static class ColoredCoinsOrderPlacement implements Attachment {

        private final Long assetId;
        private final long quantityQNT;
        private final long priceNQT;

        private ColoredCoinsOrderPlacement(Long assetId, long quantityQNT, long priceNQT) {

            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.priceNQT = priceNQT;

        }

        @Override
        public int getSize() {
            return 8 + 8 + 8;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(Convert.nullToZero(assetId));
            buffer.putLong(quantityQNT);
            buffer.putLong(priceNQT);

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("asset", Convert.toUnsignedLong(assetId));
            attachment.put("quantityQNT", quantityQNT);
            attachment.put("priceNQT", priceNQT);

            return attachment;

        }

        public Long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public long getPriceNQT() {
            return priceNQT;
        }
    }

    public final static class ColoredCoinsAskOrderPlacement extends ColoredCoinsOrderPlacement {

        public ColoredCoinsAskOrderPlacement(Long assetId, long quantityQNT, long priceNQT) {
            super(assetId, quantityQNT, priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_PLACEMENT;
        }

    }

    public final static class ColoredCoinsBidOrderPlacement extends ColoredCoinsOrderPlacement {

        public ColoredCoinsBidOrderPlacement(Long assetId, long quantityQNT, long priceNQT) {
            super(assetId, quantityQNT, priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_PLACEMENT;
        }

    }

    abstract static class ColoredCoinsOrderCancellation implements Attachment {

        private final Long orderId;

        private ColoredCoinsOrderCancellation(Long orderId) {
            this.orderId = orderId;
        }

        @Override
        public int getSize() {
            return 8;
        }

        @Override
        public byte[] getBytes() {

            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(Convert.nullToZero(orderId));

            return buffer.array();

        }

        @Override
        public JSONObject getJSONObject() {

            JSONObject attachment = new JSONObject();
            attachment.put("order", Convert.toUnsignedLong(orderId));

            return attachment;

        }

        public Long getOrderId() {
            return orderId;
        }
    }

    public final static class ColoredCoinsAskOrderCancellation extends ColoredCoinsOrderCancellation {

        public ColoredCoinsAskOrderCancellation(Long orderId) {
            super(orderId);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION;
        }

    }

    public final static class ColoredCoinsBidOrderCancellation extends ColoredCoinsOrderCancellation {

        public ColoredCoinsBidOrderCancellation(Long orderId) {
            super(orderId);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_CANCELLATION;
        }

    }

    public final static class DigitalGoodsListing implements Attachment {

        private final String name;
        private final String description;
        private final String tags;
        private final int quantity;
        private final long priceNQT;

        public DigitalGoodsListing(String name, String description, String tags, int quantity, long priceNQT) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
        }

        @Override
        public int getSize() {
            return 2 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 2
                        + Convert.toBytes(tags).length + 4 + 8;
        }

        @Override
        public byte[] getBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            byte[] nameBytes = Convert.toBytes(name);
            buffer.putShort((short)nameBytes.length);
            buffer.put(nameBytes);
            byte[] descriptionBytes = Convert.toBytes(description);
            buffer.putShort((short)descriptionBytes.length);
            buffer.put(descriptionBytes);
            byte[] tagsBytes = Convert.toBytes(tags);
            buffer.putShort((short)tagsBytes.length);
            buffer.put(tagsBytes);
            buffer.putInt(quantity);
            buffer.putLong(priceNQT);
            return buffer.array();
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("tags", tags);
            attachment.put("quantity", quantity);
            attachment.put("priceNQT", priceNQT);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.LISTING;
        }

        public String getName() { return name; }

        public String getDescription() { return description; }

        public String getTags() { return tags; }

        public int getQuantity() { return quantity; }

        public long getPriceNQT() { return priceNQT; }

    }

    public final static class DigitalGoodsDelisting implements Attachment {

        private final Long goodsId;

        public DigitalGoodsDelisting(Long goodsId) {
            this.goodsId = goodsId;
        }

        @Override
        public int getSize() {
            return 8;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(goodsId);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("goods", Convert.toUnsignedLong(goodsId));
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELISTING;
        }

        public Long getGoodsId() { return goodsId; }

    }

    public final static class DigitalGoodsPriceChange implements Attachment {

        private final Long goodsId;
        private final long priceNQT;

        public DigitalGoodsPriceChange(Long goodsId, long priceNQT) {
            this.goodsId = goodsId;
            this.priceNQT = priceNQT;
        }

        @Override
        public int getSize() {
            return 8 + 8;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(goodsId);
                buffer.putLong(priceNQT);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("goods", Convert.toUnsignedLong(goodsId));
            attachment.put("priceNQT", priceNQT);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PRICE_CHANGE;
        }

        public Long getGoodsId() { return goodsId; }

        public long getPriceNQT() { return priceNQT; }

    }

    public final static class DigitalGoodsQuantityChange implements Attachment {

        private final Long goodsId;
        private final int deltaQuantity;

        public DigitalGoodsQuantityChange(Long goodsId, int deltaQuantity) {
            this.goodsId = goodsId;
            this.deltaQuantity = deltaQuantity;
        }

        @Override
        public int getSize() {
            return 8 + 4;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(goodsId);
                buffer.putInt(deltaQuantity);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("goods", Convert.toUnsignedLong(goodsId));
            attachment.put("deltaQuantity", deltaQuantity);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.QUANTITY_CHANGE;
        }

        public Long getGoodsId() { return goodsId; }

        public int getDeltaQuantity() { return deltaQuantity; }

    }

    public final static class DigitalGoodsPurchase implements Attachment {

        private final Long goodsId;
        private final int quantity;
        private final long priceNQT;
        private final int deliveryDeadlineTimestamp;
        private final EncryptedData note;

        public DigitalGoodsPurchase(Long goodsId, int quantity, long priceNQT, int deliveryDeadlineTimestamp, EncryptedData note) {
            this.goodsId = goodsId;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
            this.deliveryDeadlineTimestamp = deliveryDeadlineTimestamp;
            this.note = note;
        }

        @Override
        public int getSize() {
            return 8 + 4 + 8 + 4 + 2 + note.getData().length + note.getNonce().length;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(goodsId);
                buffer.putInt(quantity);
                buffer.putLong(priceNQT);
                buffer.putInt(deliveryDeadlineTimestamp);
                buffer.putShort((short)note.getData().length);
                buffer.put(note.getData());
                buffer.put(note.getNonce());
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("goods", Convert.toUnsignedLong(goodsId));
            attachment.put("quantity", quantity);
            attachment.put("priceNQT", priceNQT);
            attachment.put("deliveryDeadlineTimestamp", deliveryDeadlineTimestamp);
            attachment.put("note", Convert.toHexString(note.getData()));
            attachment.put("noteNonce", Convert.toHexString(note.getNonce()));
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PURCHASE;
        }

        public Long getGoodsId() { return goodsId; }

        public int getQuantity() { return quantity; }

        public long getPriceNQT() { return priceNQT; }

        public int getDeliveryDeadlineTimestamp() { return deliveryDeadlineTimestamp; }

        public EncryptedData getNote() { return note; }

    }

    public final static class DigitalGoodsDelivery implements Attachment {

        private final Long purchaseId;
        private final EncryptedData goods;
        private final long discountNQT;

        public DigitalGoodsDelivery(Long purchaseId, EncryptedData goods, long discountNQT) {
            this.purchaseId = purchaseId;
            this.goods = goods;
            this.discountNQT = discountNQT;
        }

        @Override
        public int getSize() {
            return 8 + 2 + goods.getData().length + goods.getNonce().length + 8;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(purchaseId);
                buffer.putShort((short)goods.getData().length);
                buffer.put(goods.getData());
                buffer.put(goods.getNonce());
                buffer.putLong(discountNQT);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("purchase", Convert.toUnsignedLong(purchaseId));
            attachment.put("goodsData", Convert.toHexString(goods.getData()));
            attachment.put("goodsNonce", Convert.toHexString(goods.getNonce()));
            attachment.put("discountNQT", discountNQT);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELIVERY;
        }

        public Long getPurchaseId() { return purchaseId; }

        public EncryptedData getGoods() { return goods; }

        public long getDiscountNQT() { return discountNQT; }

    }

    public final static class DigitalGoodsFeedback implements Attachment {

        private final Long purchaseId;
        private final EncryptedData note;

        public DigitalGoodsFeedback(Long purchaseId, EncryptedData note) {
            this.purchaseId = purchaseId;
            this.note = note;
        }

        @Override
        public int getSize() {
            try {
                return 8 + 2 + note.getData().length + note.getNonce().length;
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(purchaseId);
                buffer.putShort((short)note.getData().length);
                buffer.put(note.getData());
                buffer.put(note.getNonce());
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("purchase", Convert.toUnsignedLong(purchaseId));
            attachment.put("note", Convert.toHexString(note.getData()));
            attachment.put("noteNonce", Convert.toHexString(note.getNonce()));
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.FEEDBACK;
        }

        public Long getPurchaseId() { return purchaseId; }

        public EncryptedData getNote() { return note; }

    }

    public final static class DigitalGoodsRefund implements Attachment {

        private final Long purchaseId;
        private final long refundNQT;
        private final EncryptedData note;

        public DigitalGoodsRefund(Long purchaseId, long refundNQT, EncryptedData note) {
            this.purchaseId = purchaseId;
            this.refundNQT = refundNQT;
            this.note = note;
        }

        @Override
        public int getSize() {
            try {
                return 8 + 8 + 2 + note.getData().length + note.getNonce().length;
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return 0;
            }
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(purchaseId);
                buffer.putLong(refundNQT);
                buffer.putShort((short)note.getData().length);
                buffer.put(note.getData());
                buffer.put(note.getNonce());
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("purchase", Convert.toUnsignedLong(purchaseId));
            attachment.put("refundNQT", refundNQT);
            attachment.put("note", Convert.toHexString(note.getData()));
            attachment.put("noteNonce", Convert.toHexString(note.getNonce()));
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.REFUND;
        }

        public Long getPurchaseId() { return purchaseId; }

        public long getRefundNQT() { return refundNQT; }

        public EncryptedData getNote() { return note; }

    }

    public final static class AccountControlEffectiveBalanceLeasing implements Attachment {

        private final short period;

        public AccountControlEffectiveBalanceLeasing(short period) {
            this.period = period;
        }

        @Override
        public int getSize() {
            return 2;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putShort(period);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("period", period);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AccountControl.EFFECTIVE_BALANCE_LEASING;
        }

        public short getPeriod() {
            return period;
        }
    }

    public final static class MonetarySystemCurrencyIssuance implements Attachment {

        private final String name;
        private final String code;
        private final String description;
        private final byte type;
        private final long totalSupplyNQT;
        private final int issuanceHeight;
        private final long minReservePerUnitNQT;
        private final long mintingSlope;

        public MonetarySystemCurrencyIssuance(String name, String code, String description, byte type, long totalSupplyNQT, int issuanceHeight, long minReservePerUnitNQT, long mintingSlope) {
            this.name = name;
            this.code = code;
            this.description = description;
            this.type = type;
            this.totalSupplyNQT = totalSupplyNQT;
            this.issuanceHeight = issuanceHeight;
            this.minReservePerUnitNQT = minReservePerUnitNQT;
            this.mintingSlope = mintingSlope;
        }

        @Override
        public int getSize() {
            return 1 + Convert.toBytes(name).length + Constants.CURRENCY_CODE_LENGTH + 2 + Convert.toBytes(description).length + 1 + 8 + 4 + 8 + 8;
        }

        @Override
        public byte[] getBytes() {
            try {
                byte[] name = Convert.toBytes(this.name);
                byte[] description = Convert.toBytes(this.description);

                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte)name.length);
                buffer.put(name);
                buffer.put(Convert.toBytes(code));
                buffer.putShort((short) description.length);
                buffer.put(description);
                buffer.put(type);
                buffer.putLong(totalSupplyNQT);
                buffer.putInt(issuanceHeight);
                buffer.putLong(minReservePerUnitNQT);
                buffer.putLong(mintingSlope);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("name", name);
            attachment.put("code", code);
            attachment.put("description", description);
            attachment.put("type", type);
            attachment.put("totalSupplyNQT", totalSupplyNQT);
            attachment.put("issuanceHeight", issuanceHeight);
            attachment.put("minReservePerUnitNQT", minReservePerUnitNQT);
            attachment.put("mintingSlope", mintingSlope);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.CURRENCY_ISSUANCE;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public byte getType() {
            return type;
        }

        public long getTotalSupplyNQT() {
            return totalSupplyNQT;
        }

        public int getIssuanceHeight() {
            return issuanceHeight;
        }

        public long getMinReservePerUnitNQT() {
            return minReservePerUnitNQT;
        }

        public long getMintingSlope() {
            return mintingSlope;
        }

    }

    public final static class MonetarySystemReserveIncrease implements Attachment {

        private final Long currencyId;
        private final long amountNQT;

        public MonetarySystemReserveIncrease(Long currencyId, long amountNQT) {
            this.currencyId = currencyId;
            this.amountNQT = amountNQT;
        }

        @Override
        public int getSize() {
            return 8 + 8;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(Convert.nullToZero(currencyId));
                buffer.putLong(amountNQT);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("amountNQT", amountNQT);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.RESERVE_INCREASE;
        }

        public Long getCurrencyId() {
            return currencyId;
        }

        public long getAmountNQT() {
            return amountNQT;
        }

    }

    public final static class MonetarySystemReserveClaim implements Attachment {

        private final Long currencyId;
        private final long units;

        public MonetarySystemReserveClaim(Long currencyId, long units) {
            this.currencyId = currencyId;
            this.units = units;
        }

        @Override
        public int getSize() {
            return 8 + 8;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(currencyId);
                buffer.putLong(units);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("units", units);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.RESERVE_CLAIM;
        }

        public Long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

    }

    public final static class MonetarySystemMoneyTransfer implements Attachment {

        public static final class Entry {

            private final Long recipientId;
            private final Long currencyId;
            private final long units;

            public Entry(Long recipientId, Long currencyId, long units) {
                this.recipientId = recipientId;
                this.currencyId = currencyId;
                this.units = units;
            }

            public Long getRecipientId() {
                return recipientId;
            }

            public Long getCurrencyId() {
                return currencyId;
            }

            public long getUnits() {
                return units;
            }

        }

        private final List<Entry> entries;
        private final String comment;

        public MonetarySystemMoneyTransfer(List<Entry> entries, String comment) {
            this.entries = entries;
            this.comment = Convert.nullToEmpty(comment);
        }

        @Override
        public int getSize() {
            return 2 + entries.size() * (8 + 8 + 8) + 2 + Convert.toBytes(comment).length;
        }

        @Override
        public byte[] getBytes() {
            try {
                byte[] comment = Convert.toBytes(this.comment);

                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putShort((short)entries.size());
                for (Entry entry : entries) {
                    buffer.putLong(entry.getRecipientId());
                    buffer.putLong(entry.getCurrencyId());
                    buffer.putLong(entry.getUnits());
                }
                buffer.putShort((short)comment.length);
                buffer.put(comment);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            JSONArray entriesArray = new JSONArray();
            for (Entry entry : entries) {
                JSONObject entryObject = new JSONObject();
                entryObject.put("recipient", Convert.toUnsignedLong(entry.getRecipientId()));
                entryObject.put("currency", Convert.toUnsignedLong(entry.getCurrencyId()));
                entryObject.put("units", entry.getUnits());
                entriesArray.add(entryObject);
            }
            attachment.put("transfers", entriesArray);
            attachment.put("comment", comment);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.MONEY_TRANSFER;
        }

        public int getNumberOfEntries() {
            return entries.size();
        }

        public Entry getEntry(int index) {
            return entries.get(index);
        }

        public String getComment() {
            return comment;
        }

    }

    public final static class MonetarySystemExchangeSetting implements Attachment {

        private final Long currencyId;
        private final long buyingRateNQT;
        private final long sellingRateNQT;
        private final long totalBuyingLimitNQT;
        private final long totalSellingLimit;
        private final long initialNXTSupplyNQT;
        private final long initialCurrencySupply;
        private final int expirationHeight;

        public MonetarySystemExchangeSetting(Long currencyId, long buyingRateNQT, long sellingRateNQT, long totalBuyingLimitNQT, long totalSellingLimit, long initialNXTSupplyNQT, long initialCurrencySupply, int expirationHeight) {
            this.currencyId = currencyId;
            this.buyingRateNQT = buyingRateNQT;
            this.sellingRateNQT = sellingRateNQT;
            this.totalBuyingLimitNQT = totalBuyingLimitNQT;
            this.totalSellingLimit = totalSellingLimit;
            this.initialNXTSupplyNQT = initialNXTSupplyNQT;
            this.initialCurrencySupply = initialCurrencySupply;
            this.expirationHeight = expirationHeight;
        }

        @Override
        public int getSize() {
            return 8 + 8 + 8 + 8 + 8 + 8 + 8 + 4;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(currencyId);
                buffer.putLong(buyingRateNQT);
                buffer.putLong(sellingRateNQT);
                buffer.putLong(totalBuyingLimitNQT);
                buffer.putLong(totalSellingLimit);
                buffer.putLong(initialNXTSupplyNQT);
                buffer.putLong(initialCurrencySupply);
                buffer.putInt(expirationHeight);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("buyingRateNQT", buyingRateNQT);
            attachment.put("sellingRateNQT", sellingRateNQT);
            attachment.put("totalBuyingLimitNQT", totalBuyingLimitNQT);
            attachment.put("totalSellingLimit", totalSellingLimit);
            attachment.put("initialNXTSupplyNQT", initialNXTSupplyNQT);
            attachment.put("initialCurrencySupply", initialCurrencySupply);
            attachment.put("expirationHeight", expirationHeight);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.EXCHANGE_SETTING;
        }

        public Long getCurrencyId() {
            return currencyId;
        }

        public long getBuyingRateNQT() {
            return buyingRateNQT;
        }

        public long getSellingRateNQT() {
            return sellingRateNQT;
        }

        public long getTotalBuyingLimitNQT() {
            return totalBuyingLimitNQT;
        }

        public long getTotalSellingLimit() {
            return totalSellingLimit;
        }

        public long getInitialNXTSupplyNQT() {
            return initialNXTSupplyNQT;
        }

        public long getInitialCurrencySupply() {
            return initialCurrencySupply;
        }

        public int getExpirationHeight() {
            return expirationHeight;
        }

    }

    public final static class MonetarySystemMoneyMinting implements Attachment {

        private final Long currencyId;
        private final int units;
        private final int counter;
        private final long nonce;

        public MonetarySystemMoneyMinting(Long currencyId, int units, int counter, long nonce) {
            this.currencyId = currencyId;
            this.units = units;
            this.counter = counter;
            this.nonce = nonce;
        }

        @Override
        public int getSize() {
            return 8 + 4 + 4 + 8;
        }

        @Override
        public byte[] getBytes() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(currencyId);
                buffer.putInt(units);
                buffer.putInt(counter);
                buffer.putLong(nonce);
                return buffer.array();
            } catch (RuntimeException e) {
                Logger.logMessage("Error in getBytes", e);
                return null;
            }
        }

        @Override
        public JSONObject getJSONObject() {
            JSONObject attachment = new JSONObject();
            attachment.put("currency", Convert.toUnsignedLong(currencyId));
            attachment.put("units", units);
            attachment.put("counter", counter);
            attachment.put("nonce", nonce);
            return attachment;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.MonetarySystem.MONEY_MINTING;
        }

        public Long getCurrencyId() {
            return currencyId;
        }

        public int getUnits() {
            return units;
        }

        public int getCounter() {
            return counter;
        }

        public long getNonce() {
            return nonce;
        }

    }

}
