package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.dashboard.ApplianceCount;
import com.undercontroll.domain.model.dashboard.ComponentUsage;
import com.undercontroll.domain.model.dashboard.CustomerTypePoint;
import com.undercontroll.domain.model.dashboard.RevenuePoint;
import com.undercontroll.domain.model.dashboard.StatusCount;
import com.undercontroll.domain.model.market.RepairCatalogRow;

import java.sql.Date;
import java.time.LocalDate;

final class OrderReportRows {

    private OrderReportRows() {
    }

    static RevenuePoint revenue(Object[] row) {
        return new RevenuePoint(toLocalDate(row[0]), toDouble(row[1]), toDouble(row[2]), toLong(row[3]));
    }

    static CustomerTypePoint customerType(Object[] row) {
        return new CustomerTypePoint(toLocalDate(row[0]), toLong(row[1]), toLong(row[2]));
    }

    static StatusCount statusCount(Object[] row) {
        return new StatusCount(OrderStatus.valueOf((String) row[0]), toLong(row[1]));
    }

    static ApplianceCount appliance(Object[] row) {
        return new ApplianceCount((String) row[0], (String) row[1], toLong(row[2]));
    }

    static ComponentUsage componentUsage(Object[] row) {
        return new ComponentUsage(toInteger(row[0]), (String) row[1], (String) row[2], (String) row[3], toLong(row[4]));
    }

    static RepairCatalogRow repairCatalog(Object[] row) {
        return new RepairCatalogRow(asString(row, 0), asString(row, 1), asString(row, 2), toLong(row.length > 3 ? row[3] : null));
    }

    private static String asString(Object[] row, int index) {
        if (index >= row.length || row[index] == null) {
            return null;
        }
        return row[index].toString();
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return null;
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        return 0L;
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
