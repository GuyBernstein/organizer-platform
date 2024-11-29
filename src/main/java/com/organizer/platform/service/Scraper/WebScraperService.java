package com.organizer.platform.service.Scraper;

import com.organizer.platform.model.ScraperDTO.*;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jsoup.nodes.Element;

@Service
public class WebScraperService {
    private static final int TIMEOUT_MS = 10000;

    public WebPageContent scrapeWebPage(String url) throws IOException {
        Document doc = connectToUrl(url);
        return extractContent(doc);
    }

    private Document connectToUrl(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .get();
    }

    private WebPageContent extractContent(Document doc) {
        return WebPageContent.builder()
                .title(doc.title())
                .mainContent(extractMainContent(doc))
                .navigation(extractNavigation(doc))
                .headings(extractHeadings(doc))
                .links(extractLinks(doc))
                .metadata(extractMetadata(doc))
                .build();
    }

    private List<String> extractMainContent(Document doc) {
        // Try multiple common selectors for main content
        Elements mainElements = doc.select("main, #main, .main, article, .content, #content");
        if (mainElements.isEmpty()) {
            // Fallback: get body text excluding navigation and footer
            mainElements = doc.select("body");
            mainElements.select("nav, header, footer").remove();
        }

        return mainElements.stream()
                .map(Element::text)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private NavigationInfo extractNavigation(Document doc) {
        Elements navElements = doc.select("nav, header");
        List<NavItem> navItems = navElements.select("a").stream()
                .map(a -> new NavItem(a.text(), a.attr("href")))
                .filter(item -> StringUtils.isNotBlank(item.getText()))
                .collect(Collectors.toList());

        return new NavigationInfo(navItems);
    }

    private List<HeadingInfo> extractHeadings(Document doc) {
        return doc.select("h1, h2, h3, h4, h5, h6").stream()
                .map(h -> new HeadingInfo(
                        h.text(),
                        Integer.parseInt(h.tagName().substring(1)),
                        h.parents().stream()
                                .map(Element::className)
                                .collect(Collectors.joining(" "))
                ))
                .filter(h -> StringUtils.isNotBlank(h.getText()))
                .collect(Collectors.toList());
    }

    private List<LinkInfo> extractLinks(Document doc) {
        return doc.select("a[href]").stream()
                .map(a -> new LinkInfo(
                        a.text(),
                        a.attr("href"),
                        a.attr("title"),
                        !a.attr("rel").contains("nofollow")
                ))
                .filter(l -> StringUtils.isNotBlank(l.getText()))
                .collect(Collectors.toList());
    }

    private Map<String, String> extractMetadata(Document doc) {
        Map<String, String> metadata = new HashMap<>();
        // Extract meta tags
        doc.select("meta").forEach(meta -> {
            String name = meta.attr("name");
            String content = meta.attr("content");
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(content)) {
                metadata.put(name, content);
            }
        });
        return metadata;
    }
}
