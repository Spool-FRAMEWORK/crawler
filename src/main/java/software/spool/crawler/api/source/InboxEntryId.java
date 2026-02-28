package software.spool.crawler.api.source;

public record InboxEntryId(String value) {
    public static InboxEntryId of(String id) {
        return new InboxEntryId(id);
    }
}
