package edu.univ.erp.domain;

import java.time.LocalDateTime;

/**
 * Global maintenance mode tracker
 */
public class MaintenanceMode {
    private boolean enabled;
    private LocalDateTime activatedAt;

    public MaintenanceMode() {
        this.enabled = false;
        this.activatedAt = null;
    }

    public boolean isEnabled() { return enabled; }
    public LocalDateTime getActivatedAt() { return activatedAt; }

    public void enable() {
        this.enabled = true;
        this.activatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.enabled = false;
        this.activatedAt = null;
    }
}
