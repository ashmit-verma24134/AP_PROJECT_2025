package edu.univ.erp.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Implementation of SettingsDao for your settings table:
 * key | value | updated_at
 */
public class SettingsDaoImpl implements SettingsDao {

    private final Connection conn;

    public SettingsDaoImpl(Connection conn) {
        this.conn = conn;
    }

    /**
     * Returns TRUE if maintenance mode is active.
     * Accepts: "true", "1", "yes" (case-insensitive)
     */
    @Override
    public boolean isMaintenanceOn() {
        final String sql = "SELECT value FROM settings WHERE `key` = 'maintenance_on' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String v = rs.getString("value");
                if (v == null) return false;

                v = v.trim().toLowerCase();

                // Support all common truthy values
                return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y");
            }
        } catch (SQLException e) {
            System.err.println("[SettingsDaoImpl] Error reading maintenance flag: " + e.getMessage());
        }
        return false;
    }

    /**
     * Sets maintenance mode to ON/OFF.
     * Saves: "true" or "false"
     */
    @Override
    public boolean setMaintenance(boolean on) {
        final String sql = "UPDATE settings SET value = ?, updated_at = NOW() WHERE `key` = 'maintenance_on'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, on ? "true" : "false");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[SettingsDaoImpl] Error updating maintenance flag: " + e.getMessage());
            return false;
        }
    }
}
