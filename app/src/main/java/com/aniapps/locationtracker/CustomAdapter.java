package com.aniapps.locationtracker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/*********
 * Adapter class extends with BaseAdapter and implements with OnClickListener
 ************/
public class CustomAdapter extends ArrayAdapter<String> {
    private final Activity activity;
    private final List<String> data;
    LayoutInflater inflater;
    Context context;
    int flag;
    ArrayList<Boolean> al_cities_all = new ArrayList<>();
    boolean coming_from_personal_detials;

    public CustomAdapter(Activity activitySpinner, int textViewResourceId,
                         List<String> objects) {
        super(activitySpinner, textViewResourceId, objects);

        activity = activitySpinner;
        data = objects;

        inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public CustomAdapter(Activity activitySpinner, int textViewResourceId,
                         List<String> objects, int flag) {
        super(activitySpinner, textViewResourceId, objects);

        activity = activitySpinner;
        data = objects;
        this.flag = flag;
        inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public CustomAdapter(Activity activitySpinner, int textViewResourceId,
                         List<String> objects, int flag, ArrayList<Boolean> al_cities_all) {
        super(activitySpinner, textViewResourceId, objects);

        activity = activitySpinner;
        data = objects;
        this.flag = flag;
        this.al_cities_all = al_cities_all;
        inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = null;
        v = inflater.inflate(R.layout.spinner_rows, parent, false);// //
        TextView label = (TextView) v.findViewById(R.id.company);

        if (al_cities_all.size() != 0) {
            if (al_cities_all.get(position)) {
                label.setBackgroundColor(Color.parseColor("#09bddc"));
                label.setTextColor(Color.WHITE);
            } else {
                label.setTextColor(Color.BLACK);
            }

        } else {
            if (position == 0) {
                label.setBackgroundColor(Color.parseColor("#09bddc"));
                label.setTextColor(Color.WHITE);
            } else
                label.setTextColor(Color.BLACK);

        }
        if (data.size() != 0) {

            label.setText(data.get(position));
        }
        return v;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {

        View v = null;


       /* if (flag == 0) {*/
            v = inflater.inflate(R.layout.spinner_rows, parent, false);// //
     /*   } else {
            v = inflater.inflate(R.layout.spinner_rows_unreg, parent, false);// //
        }*/
        TextView label = (TextView) v.findViewById(R.id.company);


        if (coming_from_personal_detials) {
            if (position == 0) {
                label.setTextColor(Color.parseColor("#808080"));
            } else {
                label.setTextColor(Color.BLACK);
            }

        }

        if (data.size() != 0) {
            if (flag == 2) {
                label.setTextSize(14);
            }
            label.setText(data.get(position));
        }


        return v;

    }
}
