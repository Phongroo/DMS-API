package com.base.model;

public class ActionButton {
    private String label;
    private String action;
    private String color;
    private String value;
    private String type;
    private String position;

    public ActionButton(String label, String action, String color, String value, String type, String position) {
        this.label = label;
        this.action = action;
        this.color = color;
        this.value = value;
        this.type = type;
        this.position = position;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}