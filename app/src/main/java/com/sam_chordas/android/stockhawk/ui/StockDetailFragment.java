package com.sam_chordas.android.stockhawk.ui;

import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.db.chart.view.Tooltip;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.ArchivedQuoteColumn;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class StockDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String LOG_TAG = StockDetailFragment.class.getSimpleName();
    private String mSymbol;
    private String[] labels = new String[]{};
    private float[] values;
    private final int ARCHIVED_QUOTE_LOADER = 0;
    private TextView mSymbolTv;
    private TextView mNameTv;
    private TextView mBidTv;
    private TextView mChangeTv;
    private TextView mDateTv;
    private TextView mHighTv;
    private TextView mOpenTv;
    private TextView mCloseTv;
    private TextView mLowTv;
    private LineChart lineChart;
    LineData lineData;
    public StockDetailFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(R.layout.fragment_stock_detail, container, false);
        if( savedInstanceState == null){
            Bundle arguments = getArguments();
            mSymbolTv = (TextView)rootView.findViewById(R.id.linechart);
            mNameTv = (TextView)rootView.findViewById(R.id.company_name);
            mBidTv = (TextView)rootView.findViewById(R.id.bid_price);
            mChangeTv = (TextView)rootView.findViewById(R.id.change);
            mDateTv = (TextView)rootView.findViewById(R.id.stock_detail_date);
            mHighTv = (TextView) rootView.findViewById(R.id.day_high);
            mLowTv = (TextView) rootView.findViewById(R.id.day_low);
            mOpenTv = (TextView) rootView.findViewById(R.id.open);
            mCloseTv = (TextView) rootView.findViewById(R.id.close);
            lineChart = (LineChart)rootView.findViewById(R.id.linechart);

            mSymbol = arguments.getString("symbol");
            mSymbolTv.setText(mSymbol);
            mNameTv.setText(arguments.getString("name"));
            mBidTv.setText(arguments.getString("todayBid"));
            mChangeTv.setText(arguments.getString("todayChange"));
            mHighTv.setText(arguments.getString("currentLow"));
            mLowTv.setText(arguments.getString("currentHigh"));
        }
        Log.d(LOG_TAG,"Got Called with Symbol :"+mSymbol);
//        getLoaderManager().restartLoader(ARCHIVED_QUOTE_LOADER, null, this);
        return rootView;
    }


//    @Override
//    public void onSaveInstanceState(Bundle saveState){
//        super.onSaveInstanceState(saveState);
//        //check if movieOldClassArrayList is not empty,in this way we don't get null pointer exception when activity is recreated
//        if(movieArrayList !=null) saveState.putParcelableArrayList(SAVED_MOVIE_LIST, movieArrayList);
//        saveState.putString(SAVED_SORT_PREF,sortBy);
//        saveState.putInt(SAVED_PAGE_NO,page);
//        saveState.putBoolean(SAVED_FAV_STATE, favoriteState);
//    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(ARCHIVED_QUOTE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = ArchivedQuoteColumn.DATE + " ASC";
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(getActivity(), QuoteProvider.ArchivedQuotes.withSymbol(mSymbol),
                new String[]{ArchivedQuoteColumn._ID, ArchivedQuoteColumn.SYMBOL,ArchivedQuoteColumn.DATE,
                        ArchivedQuoteColumn.HIGH,ArchivedQuoteColumn.LOW,ArchivedQuoteColumn.OPEN,
                        ArchivedQuoteColumn.CLOSE},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null)
            Log.d(LOG_TAG,"Non Empty Cursor Received");
        else Log.d(LOG_TAG,"EMPTY Cursor Received ");

        LineSet dataset = new LineSet();
        String[] l = new String[data.getCount()];
        float[] v = new float[data.getCount()];
//        ArrayList<String> l = new ArrayList<>();
//        ArrayList<Float> v = new ArrayList<>();
        String date;
//        DatabaseUtils.dumpCursor(data);
        for (int i=0; i<data.getCount() && data!=null; i++){
            data.moveToPosition(i);
            date = Utils.getDateFromMillis(data.getLong(data.getColumnIndex("date")),"MM dd,yyyy");
            l[i] = date;
            v[i] = Float.parseFloat(data.getString(data.getColumnIndex("close")));
        }
        labels = l;
        values = v;
        lineData = new LineData(l,v);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
