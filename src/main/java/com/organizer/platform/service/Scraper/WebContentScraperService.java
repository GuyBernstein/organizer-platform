package com.organizer.platform.service.Scraper;

import com.organizer.platform.model.ScraperDTO.*;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.jsoup.nodes.Element;

@Service
public class WebContentScraperService {
    public WebsiteContent scrapeWebsite(String url) throws IOException {
        Document doc = connectToWebsite(url);
        return extractWebsiteContent(doc);
    }

    private Document connectToWebsite(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .followRedirects(true)
                .maxBodySize(0) // unlimited
                .get();
    }

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

    private String getMetaDescription(Document doc) {
        Element descriptionMeta = doc.select("meta[name=description]").first();
        return descriptionMeta != null ? descriptionMeta.attr("content") : "";
    }

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

    private List<TextBlock> extractTextBlocks(Document doc) {
        List<TextBlock> blocks = new ArrayList<>();

        // Remove invisible elements
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

    private int getElementDepth(Element element) {
        int depth = 0;
        Element parent = element.parent();
        while (parent != null) {
            depth++;
            parent = parent.parent();
        }
        return depth;
    }

    private String getParentTags(Element element) {
        List<String> parentTags = new ArrayList<>();
        Element parent = element.parent();
        while (parent != null) {
            parentTags.add(parent.tagName());
            parent = parent.parent();
        }
        return String.join(" > ", parentTags);
    }

    private List<String> extractScripts(Document doc) {
        return doc.select("script").stream()
                .map(script -> script.html().trim())
                .filter(script -> !script.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> extractStyles(Document doc) {
        return doc.select("style").stream()
                .map(style -> style.html().trim())
                .filter(style -> !style.isEmpty())
                .collect(Collectors.toList());
    }

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
