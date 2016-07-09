package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.sam_chordas.android.stockhawk.R;


public class StockDetail extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.

//            Bundle arguments = new Bundle();
//            arguments.putParcelable(DetailFragment.DETAIL_URI, getIntent().getData());

//            StockDetailFragment fragment = new StockDetailFragment();
//            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction().add(R.id.stock_detail_container,new StockDetailFragment()).commit();
        }
    }


}
