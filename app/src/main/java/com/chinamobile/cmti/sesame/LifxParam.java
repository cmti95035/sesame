package com.chinamobile.cmti.sesame;

class LifxParam {
    private final String color;
    private final String brightness;

    LifxParam(String brightness, String color) {
        this.color = color;
        this.brightness = brightness;
    }

    public String getBrightness() {
        return brightness;
    }

    public String getColor() {
        return color;
    }
}