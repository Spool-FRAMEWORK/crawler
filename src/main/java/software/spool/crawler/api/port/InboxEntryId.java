package software.spool.crawler.api.port;

public record InboxEntryId(String value) {
    public static InboxEntryId of(String id) {
        return new InboxEntryId(id);
    }
}
