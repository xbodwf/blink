package com.xbodw.blink.item;

public enum FillMode {
    FILL("创建", "填充区域内空白"),
    REPLACE("覆盖", "用选中方块替换掉区域内所有方块"),
    REMOVE("破坏", "破坏区域内选中方块");
    
    private final String displayName;
    private final String description;
    
    FillMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static FillMode fromString(String name) {
        for (FillMode mode : values()) {
            if (mode.name().equals(name)) {
                return mode;
            }
        }
        return FILL;
    }
}