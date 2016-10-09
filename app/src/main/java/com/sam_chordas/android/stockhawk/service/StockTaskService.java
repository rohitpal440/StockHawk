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
import com.sam_chordas.android.stockhawk.data.ArchivedQuoteColumn;
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
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private ArrayList mStoredSybolsBeforeUpdate;
  private boolean isUpdate;
  int result;

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }

  @Override
  public int onRunTask(TaskParams params){
    Cursor initQueryCursor;
    if (mContext == null){
      mContext = this;
    }
    StringBuilder stockQuotesUrlStringBuilder = new StringBuilder();
    try{
      // Base URL for the Yahoo query
      stockQuotesUrlStringBuilder.append(Utils.BASE_URL);
      stockQuotesUrlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
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

          mStoredSybolsBeforeUpdate = new ArrayList();
          mStoredSybolsBeforeUpdate.add("YHOO");
          mStoredSybolsBeforeUpdate.add("AAPL");
          mStoredSybolsBeforeUpdate.add("GOOG");
          mStoredSybolsBeforeUpdate.add("MSFT");

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
      } catch (UnsupportedEncodingException e){
        e.printStackTrace();
      }
    }
    // finalize the URL for the API query.

    stockQuotesUrlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
        + "org%2Falltableswithkeys&callback=");

    String stockQuotesUrlString;
    String getResponseQuotes;

    result = GcmNetworkManager.RESULT_FAILURE;

    if (stockQuotesUrlStringBuilder != null){
      stockQuotesUrlString = stockQuotesUrlStringBuilder.toString();
      try{
        getResponseQuotes = Utils.fetchData(stockQuotesUrlString);
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
            if (params.getTag().equals("add")){
              mStoredSybolsBeforeUpdate = new ArrayList();
              mStoredSybolsBeforeUpdate.add(params.getExtras().getString("symbol"));
            }
            if(mStoredSybolsBeforeUpdate != null && mStoredSybolsBeforeUpdate.size()>0){
              for (int i =0;i< mStoredSybolsBeforeUpdate.size();i++){
                String symbol = mStoredSybolsBeforeUpdate.get(i).toString();
                String response = Utils.fetchArchivedQuoteWithSymbol(symbol);
                if (response!= null){
                  if(isUpdate){
                    String selectionArgs[] = new String[]{" "};
                    selectionArgs[0]=symbol;
                    mContext.getContentResolver().update(QuoteProvider.ArchivedQuotes.CONTENT_URI,contentValues,
                            ArchivedQuoteColumn.SYMBOL+ " = ?",selectionArgs);
                  }

                  jsonObject = new JSONObject(response);
                  mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                          Utils.archivedQuoteJsonToContentVals(Utils.getQuoteJsonArray(jsonObject)));

                  if(isUpdate){
                    Log.d(LOG_TAG,"Deleting stored symbol From Archived Database : " + symbol);
                    mContext.getContentResolver().delete(QuoteProvider.ArchivedQuotes.withSymbol(symbol),
                            ArchivedQuoteColumn.ISCURRENT + " = ?",new String[]{"0"});
                  }
                } else {
                  Log.e(LOG_TAG,"Invalid Response for archived Quote");
                }
              }
            }
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