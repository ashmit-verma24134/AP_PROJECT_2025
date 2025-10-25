package edu.univ.erp.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Implementation of SettingsDao for your current schema:
 * table `settings` with columns `key` and `value`.
 */
public class SettingsDaoImpl implements SettingsDao {

    private final Connection conn;

    public SettingsDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public boolean isMaintenanceOn() {
        final String sql = "SELECT value FROM settings WHERE `key` = 'maintenance_on'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String val = rs.getString("value");
                return val != null && val.trim().equalsIgnoreCase("true");
            }
        } catch (SQLException e) {
            System.err.println("[SettingsDaoImpl] Error reading maintenance flag: " + e.getMessage());
        }
        return false;
    }

    /**
     * Optional helper to toggle the maintenance flag.
     * Returns true if an update row was modified.
     */
    public boolean setMaintenance(boolean on) {
        final String sql = "UPDATE settings SET value = ? WHERE `key` = 'maintenance_on'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, on ? "true" : "false");
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            System.err.println("[SettingsDaoImpl] Error updating maintenance flag: " + e.getMessage());
            return false;
        }
    }
}
