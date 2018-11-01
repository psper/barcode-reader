/*
 * Copyright (C) 2013 Surviving with Android (http://www.survivingwithandroid.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.easy.barcodereader;

import java.util.ArrayList;
import java.util.List;



import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class SimpleAdapter extends ArrayAdapter<Contact> implements Filterable {
	
	private List<Contact> contactListG;
	private Context context;
	private Filter ContactFilter;
	List<Contact> origContactList;
		
	public SimpleAdapter(List<Contact> itemList, Context ctx) {
		super(ctx, android.R.layout.simple_list_item_1, itemList);
		this.contactListG = itemList;
		this.context = ctx;	
		this.origContactList = itemList;
	}
	
	public int getCount() {
		if (contactListG != null)
			return contactListG.size();
		return 0;
	}

	public Contact getItem(int position) {
		if (contactListG != null)
			return contactListG.get(position);
		return null;
	}

	public long getItemId(int position) {
		if (contactListG != null)
			return contactListG.get(position).hashCode();
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View v = convertView;
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.list_item, null);
		}
		
		Contact c = contactListG.get(position);
		TextView text = (TextView) v.findViewById(R.id.name);
		text.setText(c.getName());

		TextView text2 = (TextView) v.findViewById(R.id.companyNumber);
		text2.setText(c.getCompanyNumber());

		TextView text3 = (TextView) v.findViewById(R.id.confirmed);
		text3.setText(c.getIpod());

		TextView text4 = (TextView) v.findViewById(R.id.accommodation);
		text4.setText(c.getSenhas());	

		TextView text5 = (TextView) v.findViewById(R.id.state);
		text5.setText(c.getMesa());

		TextView text6 = (TextView) v.findViewById(R.id.userCode);
		text6.setText(c.getState());		
		return v;	
	}

	public void resetData() {
		contactListG = origContactList;
	}
	public List<Contact> getItemList() {
		return contactListG;
	}

	public void setItemList(List<Contact> itemList) {
		int i = 0;
		this.origContactList = itemList;
		contactListG = itemList;
	}
	/*
	 * We create our filter	
	 */
	
	@Override
	public Filter getFilter() {
		if (ContactFilter == null)
			ContactFilter = new ContactFilter();
		
		return ContactFilter;
	}

	private class ContactFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			// We implement here the filter logic
			if (constraint == null || constraint.length() == 0) {
				// No filter implemented we return all the list
				results.values = origContactList;
				results.count = origContactList.size();
			}
			else {
				// We perform filtering operation
				List<Contact> nContactList = new ArrayList<Contact>();
				
				for (Contact p : contactListG) {
					if (p.getName().toUpperCase().startsWith(constraint.toString().toUpperCase()))
						nContactList.add(p);
				}
				
				results.values = nContactList;
				results.count = nContactList.size();

			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			
			// Now we have to inform the adapter about the new list filtered
			if (results.count == 0)
				notifyDataSetInvalidated();
			else {
				contactListG = (List<Contact>) results.values;
				notifyDataSetChanged();
			}
			
		}
		
	}
	
}


