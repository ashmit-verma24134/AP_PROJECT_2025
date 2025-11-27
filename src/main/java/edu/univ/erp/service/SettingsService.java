package edu.univ.erp.service;

/**
 * Simple service abstraction for system-wide settings,
 * mainly maintenance mode ON/OFF.
 */
public interface SettingsService {

    /** @return true if maintenance mode is ON */
    boolean isMaintenanceOn();

    /**
     * Enable or disable maintenance mode.
     * @return true if update succeeded
     */
    boolean setMaintenance(boolean on);
}
