package com.organizer.platform.model.organizedDTO;

public class MessageTypeCount {
    private final String name;
    private final String icon;
    private final Long count;

    public MessageTypeCount(String name, String icon, Long count) {
        this.name = name;
        this.icon = icon;
        this.count = count;
    }

    // Getters
    public String getName() { return getHebrewName(name); }
    public String getIcon() { return icon; }
    public Long getCount() { return count; }


    // Helper method for icon mapping
    public static String getIconForType(String type) {
        String typeStr = type.toLowerCase();
        switch (typeStr) {
            case "text":
                return "bi bi-chat-text-fill";
            case "image":
                return "bi bi-image-fill";
            case "document":
                return "bi bi-file-earmark-text-fill";
            case "audio":
                return "bi bi-file-music-fill";
            default:
                return "bi bi-question-circle-fill";
        }
    }

    // Helper method for Hebrew names
    private static String getHebrewName(String type) {
        String typeStr = type.toLowerCase();
        switch (typeStr) {
            case "text":
                return "טקסט";
            case "image":
                return "תמונה";
            case "document":
                return "מסמך";
            case "audio":
                return "הקלטה";
            default:
                return "אחר";
        }
    }
}
