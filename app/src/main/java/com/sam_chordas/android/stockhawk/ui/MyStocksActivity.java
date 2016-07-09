package com.sam_chordas.android.stockhawk.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;

public class MyStocksActivity extends AppCompatActivity implements MyStocksActivityFragment.Callback{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_stocks);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
    }

    @Override
    public void onItemSelected(Cursor cursor, int position) {
        if(cursor.moveToPosition(position)){
            Toast.makeText(this,"Got the valid Cursor",Toast.LENGTH_SHORT).show();
//            DatabaseUtils.dumpCurrentRow(cursor);
            Intent intent = new Intent(this,StockDetail.class);
            intent.putExtra("symbol","Test" );
            startActivity(intent);

        } else {
            Toast.makeText(this,"Got the Invalid Cursor",Toast.LENGTH_SHORT).show();
        }
    }
}
