/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.onic.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.odk.collect.onic.logic.HierarchyElement;
import org.odk.collect.onic.views.HierarchyElementView;

import java.util.ArrayList;
import java.util.List;

public class HierarchyListAdapter extends BaseAdapter {

    private Context context;
    private List<HierarchyElement> items = new ArrayList<HierarchyElement>();


    public HierarchyListAdapter(Context context) {
        this.context = context;
    }


    @Override
    public int getCount() {
        return items.size();
    }


    @Override
    public Object getItem(int position) {
        return items.get(position);
    }


    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        HierarchyElementView hev;
        if (convertView == null) {
            hev = new HierarchyElementView(context, items.get(position));
        } else {
            hev = (HierarchyElementView) convertView;
            hev.setPrimaryText(items.get(position).getPrimaryText());
            hev.setSecondaryText(items.get(position).getSecondaryText());
            hev.setIcon(items.get(position).getIcon());
            hev.setColor(items.get(position).getColor());
        }

        if (items.get(position).getSecondaryText() == null
                || items.get(position).getSecondaryText().equals("")) {
            hev.showSecondary(false);
        } else {
            hev.showSecondary(true);
        }
        return hev;

    }


    /**
     * Sets the list of items for this adapter to use.
     */
    public void setListItems(List<HierarchyElement> it) {
        items = it;
    }

}
