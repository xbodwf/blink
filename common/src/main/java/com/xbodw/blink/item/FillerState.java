package com.xbodw.blink.item;

public enum FillerState {
    SELECTING_POS1("选择位置1", "右键选择第一个位置"),
    SELECTING_POS2("选择位置2", "右键选择第二个位置"),
    READY_TO_FILL("准备填充", "右键开始填充");
    
    private final String displayName;
    private final String description;
    
    FillerState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static FillerState fromString(String name) {
        for (FillerState state : values()) {
            if (state.name().equals(name)) {
                return state;
            }
        }
        return SELECTING_POS1;
    }
}