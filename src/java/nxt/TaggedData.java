package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.PrunableDbTable;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Logger;
import nxt.util.Search;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TaggedData {

    public static final DbKey.LongKeyFactory<TaggedData> taggedDataKeyFactory = new DbKey.LongKeyFactory<TaggedData>("id") {

        @Override
        public DbKey newKey(TaggedData taggedData) {
            return taggedData.dbKey;
        }

    };

    public static final PrunableDbTable<TaggedData> taggedDataTable = new PrunableDbTable<TaggedData>(
            "tagged_data", taggedDataKeyFactory, "name,description,tags") {

        @Override
        protected TaggedData load(Connection con, ResultSet rs) throws SQLException {
            return new TaggedData(rs);
        }

        @Override
        protected void save(Connection con, TaggedData taggedData) throws SQLException {
            taggedData.save(con);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY block_timestamp DESC, db_id DESC ";
        }

        @Override
        public void trim(int height) {
            if (Constants.ENABLE_PRUNING) {
                try (Connection con = db.getConnection();
                     PreparedStatement pstmtSelect = con.prepareStatement("SELECT parsed_tags FROM tagged_data WHERE transaction_timestamp < ?");
                     PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM tagged_data WHERE transaction_timestamp < ?")) {
                    int expiration = Nxt.getEpochTime() - Constants.MAX_PRUNABLE_LIFETIME;
                    pstmtSelect.setInt(1, expiration);
                    Map<String,Integer> expiredTags = new HashMap<>();
                    try (ResultSet rs = pstmtSelect.executeQuery()) {
                        while (rs.next()) {
                            Object[] array = (Object[])rs.getArray("parsed_tags").getArray();
                            for (Object tag : array) {
                                Integer count = expiredTags.get(tag);
                                if (count == null) {
                                    expiredTags.put((String)tag, 1);
                                } else {
                                    expiredTags.put((String)tag, count + 1);
                                }
                            }
                        }
                    }
                    Tag.delete(expiredTags);
                    pstmtDelete.setInt(1, expiration);
                    int deleted = pstmtDelete.executeUpdate();
                    if (deleted > 0) {
                        Logger.logDebugMessage("Deleted " + deleted + " expired prunable data from " + table);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }
        }

    };


    public static final class Tag {

        private static final DbKey.StringKeyFactory<Tag> tagDbKeyFactory = new DbKey.StringKeyFactory<Tag>("tag") {
            @Override
            public DbKey newKey(Tag tag) {
                return tag.dbKey;
            }
        };

        private static final VersionedEntityDbTable<Tag> tagTable = new VersionedEntityDbTable<Tag>("data_tag", tagDbKeyFactory) {

            @Override
            protected Tag load(Connection con, ResultSet rs) throws SQLException {
                return new Tag(rs);
            }

            @Override
            protected void save(Connection con, Tag tag) throws SQLException {
                tag.save(con);
            }

            @Override
            public String defaultSort() {
                return " ORDER BY tag_count DESC, tag ASC ";
            }

        };

        public static int getTagCount() {
            return tagTable.getCount();
        }

        public static DbIterator<Tag> getAllTags(int from, int to) {
            return tagTable.getAll(from, to);
        }

        public static DbIterator<Tag> getTagsLike(String prefix, int from, int to) {
            DbClause dbClause = new DbClause.LikeClause("tag", prefix);
            return tagTable.getManyBy(dbClause, from, to, " ORDER BY tag ");
        }

        private static void init() {}

        private static void add(TaggedData taggedData) {
            for (String tagValue : taggedData.getParsedTags()) {
                Tag tag = tagTable.get(tagDbKeyFactory.newKey(tagValue));
                if (tag == null) {
                    tag = new Tag(tagValue);
                }
                tag.count += 1;
                tagTable.insert(tag);
            }
        }

        private static void delete(Map<String,Integer> expiredTags) {
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("UPDATE data_tag SET tag_count = tag_count - ? WHERE tag = ?");
                 PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM data_tag WHERE tag_count <= 0")) {
                for (Map.Entry<String,Integer> entry : expiredTags.entrySet()) {
                    pstmt.setInt(1, entry.getValue());
                    pstmt.setString(2, entry.getKey());
                    pstmt.executeUpdate();
                    Logger.logDebugMessage("Reduced tag count for " + entry.getKey() + " by " + entry.getValue());
                }
                int deleted = pstmtDelete.executeUpdate();
                if (deleted > 0) {
                    Logger.logDebugMessage("Deleted " + deleted + " tags");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        private final String tag;
        private final DbKey dbKey;
        private int count;

        private Tag(String tag) {
            this.tag = tag;
            this.dbKey = tagDbKeyFactory.newKey(this.tag);
        }

        private Tag(ResultSet rs) throws SQLException {
            this.tag = rs.getString("tag");
            this.dbKey = tagDbKeyFactory.newKey(this.tag);
            this.count = rs.getInt("tag_count");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO data_tag (tag, tag_count, height, latest) "
                    + "KEY (tag, height) VALUES (?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setString(++i, this.tag);
                pstmt.setInt(++i, this.count);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public String getTag() {
            return tag;
        }

        public int getCount() {
            return count;
        }

    }


    public static int getCount() {
        return taggedDataTable.getCount();
    }

    public static DbIterator<TaggedData> getAll(int from, int to) {
        return taggedDataTable.getAll(from, to);
    }

    public static TaggedData getData(long transactionId) {
        return taggedDataTable.get(taggedDataKeyFactory.newKey(transactionId));
    }

    public static DbIterator<TaggedData> getData(long accountId, int from, int to) {
        return taggedDataTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public static DbIterator<TaggedData> searchAccountData(String query, long accountId, int from, int to) {
        return taggedDataTable.search(query, new DbClause.LongClause("account_id", accountId), from, to,
                " ORDER BY ft.score DESC, tagged_data.block_timestamp DESC, tagged_data.db_id DESC ");
    }

    public static DbIterator<TaggedData> searchData(String query, int from, int to) {
        return taggedDataTable.search(query, DbClause.EMPTY_CLAUSE, from, to,
                " ORDER BY ft.score DESC, tagged_data.block_timestamp DESC, tagged_data.db_id DESC ");
    }


    static void init() {
        Tag.init();
    }

    private final long id;
    private final DbKey dbKey;
    private final long accountId;
    private final String name;
    private final String description;
    private final String tags;
    private final String[] parsedTags;
    private final byte[] data;
    private final String type;
    private final boolean isText;
    private final String filename;
    private final int transactionTimestamp;
    private final int blockTimestamp;

    private TaggedData(Transaction transaction, Attachment.TaggedDataUpload attachment) {
        this.id = transaction.getId();
        this.dbKey = taggedDataKeyFactory.newKey(this.id);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.tags = attachment.getTags();
        this.parsedTags = Search.parseTags(tags, 3, 20, 5);
        this.data = attachment.getData();
        this.type = attachment.getType();
        this.isText = attachment.isText();
        this.filename = attachment.getFilename();
        this.blockTimestamp = Nxt.getBlockchain().getLastBlockTimestamp();
        this.transactionTimestamp = transaction.getTimestamp();
    }

    private TaggedData(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = taggedDataKeyFactory.newKey(this.id);
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.tags = rs.getString("tags");
        Object[] array = (Object[])rs.getArray("parsed_tags").getArray();
        this.parsedTags = Arrays.copyOf(array, array.length, String[].class);
        this.data = rs.getBytes("data");
        this.type = rs.getString("type");
        this.isText = rs.getBoolean("is_text");
        this.filename = rs.getString("filename");
        this.blockTimestamp = rs.getInt("block_timestamp");
        this.transactionTimestamp = rs.getInt("transaction_timestamp");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO tagged_data (id, account_id, name, description, tags, parsed_tags, "
                + "type, data, is_text, filename, block_timestamp, transaction_timestamp, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.accountId);
            pstmt.setString(++i, this.name);
            pstmt.setString(++i, this.description);
            pstmt.setString(++i, this.tags);
            pstmt.setObject(++i, this.parsedTags);
            pstmt.setString(++i, this.type);
            pstmt.setBytes(++i, this.data);
            pstmt.setBoolean(++i, this.isText);
            pstmt.setString(++i, this.filename);
            pstmt.setInt(++i, this.blockTimestamp);
            pstmt.setInt(++i, this.transactionTimestamp);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getTags() {
        return tags;
    }

    public String[] getParsedTags() {
        return parsedTags;
    }

    public byte[] getData() {
        return data;
    }

    public String getType() {
        return type;
    }

    public boolean isText() {
        return isText;
    }

    public String getFilename() {
        return filename;
    }

    public int getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    static void add(Transaction transaction, Attachment.TaggedDataUpload attachment) {
        if (Nxt.getEpochTime() - transaction.getTimestamp() < Constants.MAX_PRUNABLE_LIFETIME) {
            TaggedData taggedData = taggedDataTable.get(taggedDataKeyFactory.newKey(transaction.getId()));
            if (taggedData == null) {
                taggedData = new TaggedData(transaction, attachment);
                taggedDataTable.insert(taggedData);
            }
            Tag.add(taggedData);
        }
    }

}
