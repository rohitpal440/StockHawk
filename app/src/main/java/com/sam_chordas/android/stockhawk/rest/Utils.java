package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.ArchivedQuoteColumn;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;

  public static ArrayList archivedQuoteJsonToContentVals(JSONArray quote){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    try {
      if(quote != null && quote.length() != 0){
        for (int i =0;i<quote.length();i++){
          JSONObject jsonObject =   quote.getJSONObject(i);
          batchOperations.add(buildArchivedQuoteBatchOperation(jsonObject));
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG,"Error in Parsing Json Array Quotes");
    }
    return batchOperations;
  }


  public static JSONArray getQuoteJsonArray(JSONObject jsonObject){
    JSONArray quoteJsonArray = null;
    Log.d(LOG_TAG," "+jsonObject);
    try{
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query").getJSONObject("results");
        quoteJsonArray = jsonObject.getJSONArray("quote");
      }
    }catch (JSONException e){
      Log.d(LOG_TAG,"Unable to parse Json Quote Array");
    }
    return quoteJsonArray;
  }

  public static ArrayList quoteJsonToContentVals(JSONObject jsonObject){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONArray resultsArray = null;
    try{
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");
          if(jsonObject.getString("Name") == null){

          }
          batchOperations.add(buildQuoteBatchOperation(jsonObject));
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildQuoteBatchOperation(jsonObject));
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuffer changeBuffer = new StringBuffer(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }

  public static ContentProviderOperation buildQuoteBatchOperation(JSONObject jsonObject){
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        QuoteProvider.Quotes.CONTENT_URI);
    try {
      String change = jsonObject.getString("Change");
      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString("ChangeinPercent"), true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }
      builder.withValue(QuoteColumns.NAME,jsonObject.getString("Name"));
      builder.withValue(QuoteColumns.LOW,jsonObject.getString("DaysLow"));
      builder.withValue(QuoteColumns.HIGH,jsonObject.getString("DaysHigh"));
    } catch (JSONException e){
      e.printStackTrace();
    }
    return builder.build();
  }

  public static ContentProviderOperation buildArchivedQuoteBatchOperation(JSONObject jsonObject){
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            QuoteProvider.ArchivedQuotes.CONTENT_URI);
//    Log.d(LOG_TAG,"Archived Json array " + jsonObject);
    long date = 0;
    try{
      date = getMillisFromDateString(jsonObject.getString("Date"));

      builder.withValue(ArchivedQuoteColumn.SYMBOL,jsonObject.getString("Symbol"));
      builder.withValue(ArchivedQuoteColumn.DATE,date);
      builder.withValue(ArchivedQuoteColumn.OPEN,jsonObject.getString("Open"));
      builder.withValue(ArchivedQuoteColumn.HIGH,jsonObject.getString("High"));
      builder.withValue(ArchivedQuoteColumn.LOW,jsonObject.getString("Low"));
      builder.withValue(ArchivedQuoteColumn.CLOSE,jsonObject.getString("Close"));
    }catch (JSONException e){
      Log.e(LOG_TAG,"Unable to Parse individual object");
    }
    return builder.build();
  }


  public static String formatDate(String dateString, String givenFormat, String requiredFormat){

    SimpleDateFormat fromSrc = new SimpleDateFormat(givenFormat);
    SimpleDateFormat myFormat = new SimpleDateFormat(requiredFormat);

    if(dateString != null){
      try {
        dateString = myFormat.format(fromSrc.parse(dateString));
        return dateString;
      } catch (ParseException e) {
        Log.e(LOG_TAG,"Error in Parsing the date");
      }
    }else {
      Log.e(LOG_TAG,"Null Date Value Given to the Function");
    }
    return dateString;
  }

  public static String getDateFromNow(String dateFormat, int days) {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat s = new SimpleDateFormat(dateFormat);
    cal.add(Calendar.DAY_OF_YEAR, days);
    return s.format(new Date(cal.getTimeInMillis()));
  }

  public static String getDateFromMillis(long dateInMillis,String requiredFormat){
    SimpleDateFormat formatter = new SimpleDateFormat(requiredFormat);
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(dateInMillis);
    return formatter.format(calendar.getTime());
  }

  public static long getMillisFromDateString(String dateString){
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    try
    {
      Date mDate = sdf.parse(dateString);
      long timeInMilliseconds = mDate.getTime();
//      Log.d(LOG_TAG,"Date in milli :: " + timeInMilliseconds);
      return timeInMilliseconds;
    }
    catch (ParseException e)
    {
      e.printStackTrace();
    }
    return 0;
  }
}
