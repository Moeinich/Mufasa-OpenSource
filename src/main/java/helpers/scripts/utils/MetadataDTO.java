package helpers.scripts.utils;

import helpers.ScriptCategory;
import helpers.ScriptMetadata;

import java.util.List;

public class MetadataDTO {
    private ScriptMetadata metadata;
    private String jar_url;
    private String author;

    public MetadataDTO(ScriptMetadata metadata, String jar_url, String author) {
        this.metadata = metadata;
        this.jar_url = jar_url;
        this.author = author;
    }

    // Getters
    public ScriptMetadata getMetadata() {
        return metadata;
    }

    public String getName() {
        return metadata.getName();
    }

    public List<ScriptCategory> getCategories() {
        return metadata.getCategories();
    }

    public String getGuideLink() {return metadata.getGuideLink();}

    public String getJarUrl() {
        return jar_url;
    }
    public String getAuthor() {return author;}

    @Override
    public String toString() {
        return "ScriptMetadataDTO{" +
                "metadata=" + metadata +
                ", jar_url='" + jar_url + '\'' +
                ", author='" + author + '\'' +
                '}';
    }
}
