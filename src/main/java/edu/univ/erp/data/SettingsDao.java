package edu.univ.erp.data;

/** DAO interface for small app-wide settings (like maintenance flag). */
public interface SettingsDao {
    /** @return true if maintenance mode is ON */
    boolean isMaintenanceOn();
}
