package com.organizer.platform.service.Scraper;

import com.organizer.platform.model.ScraperDTO.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for scraping and extracting content from websites.
 * Uses JSoup library to parse HTML and extract various components including
 * text, metadata, scripts, styles, lists, tables, and forms.
 */
@Service
public class WebContentScraperService {

    /**
     * Main entry point for scraping a website.
     * Connects to the URL and extracts all relevant content.
     *
     * @param url The URL of the website to scrape
     * @return WebsiteContent object containing all extracted content
     * @throws IOException If connection fails or content cannot be retrieved
     */
    public WebsiteContent scrapeWebsite(String url) throws IOException {
        Document doc = connectToWebsite(url);
        return extractWebsiteContent(doc);
    }

    /**
     * Establishes connection to the website with configured parameters.
     * Sets up user agent, timeout, and other connection settings.
     *
     * @param url The URL to connect to
     * @return JSoup Document object representing the webpage
     * @throws IOException If connection fails
     */
    private Document connectToWebsite(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .followRedirects(true)
                .maxBodySize(0) // unlimited size
                .get();
    }

    /**
     * Extracts all content components from the webpage.
     * Coordinates the extraction of various elements including metadata,
     * text blocks, scripts, styles, lists, tables, and forms.
     *
     * @param doc The JSoup Document to extract content from
     * @return WebsiteContent containing all extracted components
     */
    private WebsiteContent extractWebsiteContent(Document doc) {
        return WebsiteContent.builder()
                .title(doc.title())
                .description(getMetaDescription(doc))
                .metadata(extractMetadata(doc))
                .textBlocks(extractTextBlocks(doc))
                .scripts(extractScripts(doc))
                .styles(extractStyles(doc))
                .lists(extractLists(doc))
                .tables(extractTables(doc))
                .forms(extractForms(doc))
                .build();
    }

    /**
     * Extracts meta description from the webpage.
     *
     * @param doc The Document to extract from
     * @return The meta description content or empty string if not found
     */
    private String getMetaDescription(Document doc) {
        Element descriptionMeta = doc.select("meta[name=description]").first();
        return descriptionMeta != null ? descriptionMeta.attr("content") : "";
    }

    /**
     * Extracts all metadata tags from the webpage.
     * Includes both 'name' and 'property' based meta tags.
     *
     * @param doc The Document to extract from
     * @return Map of metadata key-value pairs
     */
    private Map<String, String> extractMetadata(Document doc) {
        Map<String, String> metadata = new HashMap<>();
        doc.select("meta").forEach(meta -> {
            String name = meta.attr("name");
            String property = meta.attr("property");
            String content = meta.attr("content");

            if (!name.isEmpty()) {
                metadata.put(name, content);
            } else if (!property.isEmpty()) {
                metadata.put(property, content);
            }
        });
        return metadata;
    }

    /**
     * Extracts visible text blocks from the webpage.
     * Filters out script, style, and other non-content elements.
     * Includes information about element hierarchy and visibility.
     *
     * @param doc The Document to extract from
     * @return List of TextBlock objects containing text content and metadata
     */
    private List<TextBlock> extractTextBlocks(Document doc) {
        List<TextBlock> blocks = new ArrayList<>();

        // Remove invisible/non-content elements
        doc.select("script, style, meta, link, noscript").remove();

        // Process remaining elements
        Elements elements = doc.getAllElements();
        for (Element element : elements) {
            String text = element.ownText().trim();
            if (!text.isEmpty()) {
                blocks.add(TextBlock.builder()
                        .text(text)
                        .tag(element.tagName())
                        .cssClass(element.className())
                        .id(element.id())
                        .depth(getElementDepth(element))
                        .parentTags(getParentTags(element))
                        .isVisible(!element.hasAttr("hidden") &&
                                !element.hasClass("hidden") &&
                                !element.hasClass("d-none"))
                        .build());
            }
        }
        return blocks;
    }

    /**
     * Calculates the depth of an element in the DOM tree.
     *
     * @param element The element to calculate depth for
     * @return The depth of the element (number of parent elements)
     */
    private int getElementDepth(Element element) {
        int depth = 0;
        Element parent = element.parent();
        while (parent != null) {
            depth++;
            parent = parent.parent();
        }
        return depth;
    }

    /**
     * Builds a string representing the hierarchy of parent tags.
     *
     * @param element The element to get parent tags for
     * @return String of parent tags joined by " > "
     */
    private String getParentTags(Element element) {
        List<String> parentTags = new ArrayList<>();
        Element parent = element.parent();
        while (parent != null) {
            parentTags.add(parent.tagName());
            parent = parent.parent();
        }
        return String.join(" > ", parentTags);
    }

    /**
     * Extracts all script content from the webpage.
     *
     * @param doc The Document to extract from
     * @return List of script contents (excluding empty scripts)
     */
    private List<String> extractScripts(Document doc) {
        return doc.select("script").stream()
                .map(script -> script.html().trim())
                .filter(script -> !script.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Extracts all style content from the webpage.
     *
     * @param doc The Document to extract from
     * @return List of style contents (excluding empty styles)
     */
    private List<String> extractStyles(Document doc) {
        return doc.select("style").stream()
                .map(style -> style.html().trim())
                .filter(style -> !style.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Extracts all ordered and unordered lists from the webpage.
     * Generates unique identifiers for lists without IDs.
     *
     * @param doc The Document to extract from
     * @return Map of list identifiers to list items
     */
    private Map<String, List<String>> extractLists(Document doc) {
        Map<String, List<String>> lists = new HashMap<>();

        // Extract ordered lists
        doc.select("ol").forEach(ol -> {
            String key = "ol_" + (ol.id().isEmpty() ? UUID.randomUUID().toString() : ol.id());
            lists.put(key, ol.select("li").stream()
                    .map(Element::text)
                    .collect(Collectors.toList()));
        });

        // Extract unordered lists
        doc.select("ul").forEach(ul -> {
            String key = "ul_" + (ul.id().isEmpty() ? UUID.randomUUID().toString() : ul.id());
            lists.put(key, ul.select("li").stream()
                    .map(Element::text)
                    .collect(Collectors.toList()));
        });

        return lists;
    }

    /**
     * Extracts all tables from the webpage.
     * Includes headers, rows, and captions.
     *
     * @param doc The Document to extract from
     * @return List of TableInfo objects containing table structure and content
     */
    private List<TableInfo> extractTables(Document doc) {
        return doc.select("table").stream()
                .map(table -> {
                    List<String> headers = table.select("th").stream()
                            .map(Element::text)
                            .collect(Collectors.toList());

                    List<List<String>> rows = table.select("tr")
                            .stream()
                            .map(row -> row.select("td").stream()
                                    .map(Element::text)
                                    .collect(Collectors.toList()))
                            .filter(row -> !row.isEmpty())
                            .collect(Collectors.toList());

                    String caption = Optional.ofNullable(table.select("caption").first())
                            .map(Element::text)
                            .orElse("");

                    return TableInfo.builder()
                            .headers(headers)
                            .rows(rows)
                            .caption(caption)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Extracts all forms from the webpage.
     * Includes form attributes and field information.
     *
     * @param doc The Document to extract from
     * @return List of FormInfo objects containing form structure and fields
     */
    private List<FormInfo> extractForms(Document doc) {
        return doc.select("form").stream()
                .map(form -> FormInfo.builder()
                        .id(form.id())
                        .action(form.attr("action"))
                        .method(form.attr("method"))
                        .fields(extractFormFields(form))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Extracts field information from a form element.
     * Includes input, textarea, and select elements.
     *
     * @param form The form element to extract fields from
     * @return List of FormField objects containing field properties
     */
    private List<FormField> extractFormFields(Element form) {
        return form.select("input, textarea, select").stream()
                .map(field -> FormField.builder()
                        .name(field.attr("name"))
                        .type(field.attr("type"))
                        .value(field.attr("value"))
                        .required(field.hasAttr("required"))
                        .build())
                .collect(Collectors.toList());
    }
}