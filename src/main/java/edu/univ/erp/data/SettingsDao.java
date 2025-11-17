package edu.univ.erp.data;

/** DAO for app-wide settings (like maintenance flag). */
public interface SettingsDao {

    /** @return true if maintenance mode is ON */
    boolean isMaintenanceOn();

    /** Update the maintenance flag. */
    boolean setMaintenance(boolean on);
}
