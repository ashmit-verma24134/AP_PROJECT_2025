package edu.univ.erp.service;

import edu.univ.erp.data.SettingsDao;
import edu.univ.erp.data.SettingsDaoImpl;
import edu.univ.erp.util.DBConnection;

import java.sql.Connection;

public class SettingsServiceImpl implements SettingsService {

    @Override
    public boolean isMaintenanceOn() {
        try (Connection conn = DBConnection.getErpConnection()) {
            SettingsDao dao = new SettingsDaoImpl(conn);
            return dao.isMaintenanceOn();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean setMaintenance(boolean on) {
        try (Connection conn = DBConnection.getErpConnection()) {
            SettingsDao dao = new SettingsDaoImpl(conn);
            return dao.setMaintenance(on);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
