package com.example.android_terminal;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

class PopupAdapter implements GoogleMap.InfoWindowAdapter {
    private View popup=null;
    private LayoutInflater inflater=null;

    //create custom info window
    PopupAdapter(LayoutInflater inflater) {
        this.inflater=inflater;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return(null);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getInfoContents(Marker marker) {
        if (popup == null) {
            popup=inflater.inflate(R.layout.popup, null);
        }

        TextView tv= popup.findViewById(R.id.title);

        tv.setText(marker.getTitle()); //set title of the ifo window
        tv= popup.findViewById(R.id.snippet);
        tv.setText(marker.getSnippet()); //set info part
        if(marker.getTitle().equals("Real Position")){ //set the image in the ifo window for real or estimated position
            ((ImageView) popup.findViewById(R.id.badge)).setImageResource(R.drawable.car_red);
        }else{
            marker.setInfoWindowAnchor(0.5f, 1f);
            ((ImageView) popup.findViewById(R.id.badge)).setImageResource(R.drawable.car_blue);
        }

        return(popup);
    }
}