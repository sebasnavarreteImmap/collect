package org.odk.collect.onic.activities;

import org.odk.collect.onic.provider.InstanceProviderAPI;

import static org.odk.collect.onic.utilities.ApplicationConstants.SortingOrder.BY_DATE_ASC;
import static org.odk.collect.onic.utilities.ApplicationConstants.SortingOrder.BY_DATE_DESC;
import static org.odk.collect.onic.utilities.ApplicationConstants.SortingOrder.BY_NAME_ASC;
import static org.odk.collect.onic.utilities.ApplicationConstants.SortingOrder.BY_NAME_DESC;
import static org.odk.collect.onic.utilities.ApplicationConstants.SortingOrder.BY_STATUS_ASC;
import static org.odk.collect.onic.utilities.ApplicationConstants.SortingOrder.BY_STATUS_DESC;

abstract class InstanceListActivity extends AppListActivity {
    protected String getSortingOrder() {
        String sortingOrder = InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " COLLATE NOCASE ASC, " + InstanceProviderAPI.InstanceColumns.STATUS + " DESC";
        switch (getSelectedSortingOrder()) {
            case BY_NAME_ASC:
                sortingOrder = InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " COLLATE NOCASE ASC, " + InstanceProviderAPI.InstanceColumns.STATUS + " DESC";
                break;
            case BY_NAME_DESC:
                sortingOrder = InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " COLLATE NOCASE DESC, " + InstanceProviderAPI.InstanceColumns.STATUS + " DESC";
                break;
            case BY_DATE_ASC:
                sortingOrder = InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE + " ASC";
                break;
            case BY_DATE_DESC:
                sortingOrder = InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE + " DESC";
                break;
            case BY_STATUS_ASC:
                sortingOrder = InstanceProviderAPI.InstanceColumns.STATUS + " ASC, " + InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " COLLATE NOCASE ASC";
                break;
            case BY_STATUS_DESC:
                sortingOrder = InstanceProviderAPI.InstanceColumns.STATUS + " DESC, " + InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " COLLATE NOCASE ASC";
                break;
        }
        return sortingOrder;
    }
}