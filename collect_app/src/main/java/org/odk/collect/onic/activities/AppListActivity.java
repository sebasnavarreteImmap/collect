/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.onic.activities;

import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
//import android.support.annotation.Nullable;
//import android.support.design.widget.BottomSheetDialog;
//import android.support.v4.view.MenuItemCompat;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.DefaultItemAnimator;
//import android.support.v7.widget.LinearLayoutManager;
//import android.support.v7.widget.RecyclerView;
//import android.support.v7.widget.SearchView;
//import android.support.v7.widget.Toolbar;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
//import android.widget.SearchView;

//import android.widget.Toolbar;

import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.odk.collect.onic.R;
import org.odk.collect.onic.adapters.SortDialogAdapter;
import org.odk.collect.onic.application.Collect;
import org.odk.collect.onic.database.ActivityLogger;
import org.odk.collect.onic.listeners.RecyclerViewClickListener;
import org.odk.collect.onic.provider.InstanceProviderAPI;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import timber.log.Timber;

import static org.odk.collect.onic.utilities.ApplicationConstants.SortingOrder.BY_NAME_ASC;

abstract class AppListActivity extends AppCompatActivity {
    private static final String SELECTED_INSTANCES = "selectedInstances";
    private static final String IS_SEARCH_BOX_SHOWN = "isSearchBoxShown";
    private static final String IS_BOTTOM_DIALOG_SHOWN = "isBottomDialogShown";
    private static final String SEARCH_TEXT = "searchText";

    protected final ActivityLogger logger = Collect.getInstance().getActivityLogger();
    protected SimpleCursorAdapter listAdapter;
    protected LinkedHashSet<Long> selectedInstances = new LinkedHashSet<>();
    protected String[] sortingOptions;
    protected Integer selectedSortingOrder;
    protected Toolbar toolbar;
    protected ListView listView;
    private BottomSheetDialog bottomSheetDialog;
    private boolean isBottomDialogShown;

    private String filterText;
    private String savedFilterText;
    private boolean isSearchBoxShown;

    private SearchView searchView;

    // toggles to all checked or all unchecked
    // returns:
    // true if result is all checked
    // false if result is all unchecked
    //
    // Toggle behavior is as follows:
    // if ANY items are unchecked, check them all
    // if ALL items are checked, uncheck them all
    public static boolean toggleChecked(ListView lv) {
        // shortcut null case
        if (lv == null) {
            return false;
        }

        boolean newCheckState = lv.getCount() > lv.getCheckedItemCount();
        setAllToCheckedState(lv, newCheckState);
        return newCheckState;
    }

    public static void setAllToCheckedState(ListView lv, boolean check) {
        // no-op if ListView null
        if (lv == null) {
            return;
        }

        for (int x = 0; x < lv.getCount(); x++) {
            lv.setItemChecked(x, check);
        }
    }

    // Function to toggle button label
    public static void toggleButtonLabel(Button toggleButton, ListView lv) {
        if (lv.getCheckedItemCount() != lv.getCount()) {
            toggleButton.setText(R.string.select_all);
        } else {
            toggleButton.setText(R.string.clear_all);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener((AdapterView.OnItemClickListener) this);

        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        listView.setEmptyView(emptyView);

        initToolbar();
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreSelectedSortingOrder();
        setupBottomSheet();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(SELECTED_INSTANCES, selectedInstances);
        outState.putBoolean(IS_BOTTOM_DIALOG_SHOWN, bottomSheetDialog.isShowing());

        if (searchView != null) {
            outState.putBoolean(IS_SEARCH_BOX_SHOWN, !searchView.isIconified());
            outState.putString(SEARCH_TEXT, String.valueOf(searchView.getQuery()));
        } else {
            Timber.e("Unexpected null search view (issue #1412)");
        }

        if (bottomSheetDialog.isShowing()) {
            bottomSheetDialog.dismiss();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        selectedInstances = (LinkedHashSet<Long>) state.getSerializable(SELECTED_INSTANCES);
        isSearchBoxShown = state.getBoolean(IS_SEARCH_BOX_SHOWN);
        isBottomDialogShown = state.getBoolean(IS_BOTTOM_DIALOG_SHOWN);
        savedFilterText = state.getString(SEARCH_TEXT);

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_menu, menu);

        final MenuItem sortItem = menu.findItem(R.id.menu_sort);
        final MenuItem searchItem = menu.findItem(R.id.menu_filter);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem); //comentadoJorge

        //final SearchView searchView = (SearchView) searchItem.getActionView(); //creado Jorge

        searchView.setQueryHint(getResources().getString(R.string.search));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterText = query;
                updateAdapter();
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterText = newText;
                updateAdapter();
                return false;
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                sortItem.setVisible(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                sortItem.setVisible(true);
                return true;
            }
        });

        if (isSearchBoxShown) {
            searchItem.expandActionView();
            searchView.setQuery(savedFilterText, false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort:
                bottomSheetDialog.show();
                isBottomDialogShown = true;
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performSelectedSearch(int position) {
        saveSelectedSortingOrder(position);
        updateAdapter();
    }

    protected void checkPreviouslyCheckedItems() {
        listView.clearChoices();
        List<Integer> selectedPositions = new ArrayList<>();
        int listViewPosition = 0;
        Cursor cursor = listAdapter.getCursor();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long instanceId = cursor.getLong(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns._ID));
                if (selectedInstances.contains(instanceId)) {
                    selectedPositions.add(listViewPosition);
                }
                listViewPosition++;
            } while (cursor.moveToNext());
        }

        for (int position : selectedPositions) {
            listView.setItemChecked(position, true);
        }
    }

    protected abstract void updateAdapter();

    protected abstract String getSortingOrderKey();

    protected boolean areCheckedItems() {
        return getCheckedCount() > 0;
    }

    protected int getCheckedCount() {
        return listView.getCheckedItemCount();
    }

    private void saveSelectedSortingOrder(int selectedStringOrder) {
        selectedSortingOrder = selectedStringOrder;
        PreferenceManager.getDefaultSharedPreferences(Collect.getInstance())
                .edit()
                .putInt(getSortingOrderKey(), selectedStringOrder)
                .apply();
    }

    protected void restoreSelectedSortingOrder() {
        selectedSortingOrder = PreferenceManager
                .getDefaultSharedPreferences(Collect.getInstance())
                .getInt(getSortingOrderKey(), BY_NAME_ASC);
    }

    protected int getSelectedSortingOrder() {
        if (selectedSortingOrder == null) {
            restoreSelectedSortingOrder();
        }
        return selectedSortingOrder;
    }

    protected CharSequence getFilterText() {
        return filterText != null ? filterText : "";
    }

    private void setupBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(this, R.style.MaterialDialogSheet);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet, null);
        final RecyclerView recyclerView = (RecyclerView) sheetView.findViewById(R.id.recyclerView);

        final SortDialogAdapter adapter = new SortDialogAdapter(this, recyclerView, sortingOptions, getSelectedSortingOrder(), new RecyclerViewClickListener() {
            @Override
            public void onItemClicked(SortDialogAdapter.ViewHolder holder, int position) {
                holder.updateItemColor(selectedSortingOrder);
                performSelectedSearch(position);
                bottomSheetDialog.dismiss();
                isBottomDialogShown = false;
            }
        });
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        bottomSheetDialog.setContentView(sheetView);

        if (isBottomDialogShown) {
            bottomSheetDialog.show();
        }
    }
}
