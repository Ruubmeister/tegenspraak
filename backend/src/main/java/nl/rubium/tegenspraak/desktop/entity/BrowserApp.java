package nl.rubium.tegenspraak.desktop.entity;

import java.util.List;

/** A web browser. Its pages are the browsing history; each visit can be opened to read the page. */
public record BrowserApp(String id, String title, List<BrowserPage> pages) implements DesktopApp {

    public BrowserApp {
        pages = pages == null ? List.of() : List.copyOf(pages);
        UniqueIds.require(pages.stream().map(BrowserPage::id).toList(), "page", id);
    }

    @Override
    public List<BrowserPage> items() {
        return pages;
    }
}
