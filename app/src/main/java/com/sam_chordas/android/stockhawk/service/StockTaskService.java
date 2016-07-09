package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private ArrayList mStoredSybolsBeforeUpdate;
  private boolean isUpdate;
  private final String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }
  String fetchData(String url) throws IOException{
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }

  @Override
  public int onRunTask(TaskParams params){
    Cursor initQueryCursor;
    if (mContext == null){
      mContext = this;
    }
    StringBuilder stockQuotesUrlStringBuilder = new StringBuilder();
    StringBuilder stockHistoryUrlStringBuilder = new StringBuilder();
    try{
      // Base URL for the Yahoo query
      stockQuotesUrlStringBuilder.append(BASE_URL);
      stockHistoryUrlStringBuilder.append(BASE_URL);
      stockQuotesUrlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
        + "in (", "UTF-8"));
      stockHistoryUrlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol "
        + "in (", "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    if (params.getTag().equals("init") || params.getTag().equals("periodic")){
      isUpdate = true;
      initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
          new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
          null, null);
      if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
        // Init task. Populates DB with quotes for the symbols seen below
        try {
          stockQuotesUrlStringBuilder.append(
              URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
          stockHistoryUrlStringBuilder.append(
              URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      } else if (initQueryCursor != null){

        initQueryCursor.moveToFirst();
        mStoredSybolsBeforeUpdate = new ArrayList();
        for (int i = 0; i < initQueryCursor.getCount(); i++){
          mStoredSybolsBeforeUpdate.add(initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")));
          mStoredSymbols.append("\""+
              initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))+"\",");
          initQueryCursor.moveToNext();
        }
        mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
        try {
          stockQuotesUrlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
          stockHistoryUrlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    } else if (params.getTag().equals("add")){
      isUpdate = false;
      // get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString("symbol");
      try {
        stockQuotesUrlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
        stockHistoryUrlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
      } catch (UnsupportedEncodingException e){
        e.printStackTrace();
      }
    }
    // finalize the URL for the API query.
    try {
      Log.d(LOG_TAG,"Date before 1 Year " + Utils.getDateFromNow("yyyy-MM-dd",-365));
      stockHistoryUrlStringBuilder.append("%20and%20startDate%20%3D%20%22" + Utils.getDateFromNow("yyyy-MM-dd",-365) + "%22%20and%20endDate%20%3D%20%22" + Utils.getDateFromNow("yyyy-MM-dd",0) + "%22");

    } catch (Exception e){
      Log.e(LOG_TAG,"Unable to Encode date for History data Properly");
    }
    stockQuotesUrlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
        + "org%2Falltableswithkeys&callback=");
    stockHistoryUrlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
        + "org%2Falltableswithkeys&callback=");


    String stockQuotesUrlString;
    String stockQuotesHistoryUrlString;
    String getResponseQuotes;
    String getResponseQuotesHistory;
    int result = GcmNetworkManager.RESULT_FAILURE;

    if (stockQuotesUrlStringBuilder != null){
      stockQuotesUrlString = stockQuotesUrlStringBuilder.toString();
      stockQuotesHistoryUrlString = stockHistoryUrlStringBuilder.toString();
      Log.d(LOG_TAG,stockQuotesHistoryUrlString);
      try{
        getResponseQuotes = fetchData(stockQuotesUrlString);
        getResponseQuotesHistory = fetchData(stockQuotesHistoryUrlString);
        result = GcmNetworkManager.RESULT_SUCCESS;
        try {
          ContentValues contentValues = new ContentValues();
          // update ISCURRENT to 0 (false) so new data is current
          if (isUpdate){
            contentValues.put(QuoteColumns.ISCURRENT, 0);
            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                null, null);
          }
          try{
            JSONObject jsonObject = new JSONObject(getResponseQuotes);

            mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                    Utils.quoteJsonToContentVals(jsonObject));
            jsonObject = new JSONObject(getResponseQuotesHistory);
            if(mStoredSybolsBeforeUpdate != null && mStoredSybolsBeforeUpdate.size()>0){
              for (int i =0;i< mStoredSybolsBeforeUpdate.size();i++){
                String symbol = mStoredSybolsBeforeUpdate.get(i).toString();
                Log.d(LOG_TAG,"Deleting stored symbol " + symbol);
                mContext.getContentResolver().delete(QuoteProvider.ArchivedQuotes.withSymbol(symbol),null,null);
              }
            }
            mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                    Utils.archivedQuoteJsonToContentVals(Utils.getQuoteJsonArray(jsonObject)));
          }catch (JSONException e){
            Log.e(LOG_TAG,"Invalid Response");
          }

        }catch (RemoteException | OperationApplicationException e){
          Log.e(LOG_TAG, "Error applying batch insert", e);
        }
      } catch (IOException e){
        e.printStackTrace();
      }
    }

    return result;
  }



}

//https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20in%20(%20%22YHOO%22%2C%22AAPL%22)%20and%20startDate%20%3D%20%222015-06-11%22%20and%20endDate%20%3D%20%222016-06-10%22&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=
//https://query.yahooapis.com/v1/public/yql?q=select+*+from+yahoo.finance.quotes+where+symbol+in+%28%22YHOO%22%2C%22AAPL%22%2C%22GOOG%22%2C%22MSFT%22%29&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=