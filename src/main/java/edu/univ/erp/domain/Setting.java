package edu.univ.erp.domain;

public class Setting {
    private final String key; // e.g., "maintenance_on"
    private final String value; // e.g., "true" or "false"

    public Setting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return this.key; }
    public String getValue() { return this.value; }
    
    public boolean booleanValue() {
        return "true".equalsIgnoreCase(value); //converts string to bool.
    }
}