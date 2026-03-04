package software.spool.crawler.example.filesystem;

import software.spool.core.exception.SpoolException;
import software.spool.crawler.api.source.PollSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FileSystemSource implements PollSource<String> {
    @Override
    public String poll() throws SpoolException {
        String resourcePath = "/part-000000.json";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assert is != null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String sourceId() {
        return "filesystem-products";
    }
}
