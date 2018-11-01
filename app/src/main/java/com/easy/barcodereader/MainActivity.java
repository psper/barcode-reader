/*
 * Copyright (C) The Android Open Source Project
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.string;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main activity demonstrating how to pass extra parameters to an activity that
 * reads barcodes.
 */
 public class MainActivity extends Activity implements View.OnClickListener {

    // use a compound button so either checkbox or switch widgets work.
    private CompoundButton autoFocus;
    private CompoundButton useFlash;
    private TextView statusMessage;
    private TextView barcodeValue;

    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final String TAG = "BarcodeMain";

    private SimpleAdapter adpt;
    List<Contact> contactList = new ArrayList<Contact>();

    // Configutarion
    String conf_serverIp = "0";
    String conf_serverPort = "";
    String FileName = "configuration.txt";
    String conf_postId= "2";
    String conf_pingTimes= "10";
    String SecurityCode= "xxx";
    String clientIp = getLocalIpAddress(true);
    String conf_inout = "IN";

    private static final int DIALOG_CONFIG = 0;
    private static final int DIALOG_LOG = 1;
    private EditText editTextserverip;
    private EditText editTextserverport;
    private EditText editTextpostId;
    private TextView txtWait;

    public Activity dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Set window fullscreen and remove title bar, and force landscape orientation
//	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);

//		 Get App configuration

        try {

            int Ret = ReadConfig();
            if(Ret == 1)
                ReadConfig();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
		//conf_serverIp="192.168.10.87";
		//conf_serverPort="3334";

        initList();

        adpt  = new SimpleAdapter(contactList, this);
        final ListView lView = (ListView) findViewById(R.id.listview);

        lView.setAdapter(adpt);

         //Initialize StaticText
        final TextView StaticText1 = (TextView) findViewById(R.id.textName);

        // Initialize TextToSearch Edit
        final EditText editTextToSearch = (EditText) findViewById(R.id.textToSearch);
        editTextToSearch.setText(conf_serverIp);
        editTextToSearch.setText("");

        editTextToSearch.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

                //adpt  = new SimpleAdapter(contactList,getApplicationContext());
                if (count < before) {
                    // We're deleting char so we need to reset the adapter data
                      adpt.resetData();
                }
                EditText TextToSearch = (EditText) findViewById(R.id.textToSearch);
                adpt.getFilter().filter(TextToSearch.getText().toString());

            }        });

        // Initialize UserToCheckin Edit
        final TextView TextViewUserToCheckin = (TextView) findViewById(R.id.userToCheckin);

        final TextView TextViewWait = (TextView) findViewById(R.id.textWait);

        // Initialize Show Checkin  button
        Button button1 = (Button) findViewById(R.id.main_button_Checkin);
        button1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                int UserSelected = lView.getCheckedItemPosition();
                Contact OneContact = (Contact)lView.getItemAtPosition(UserSelected);
                if(OneContact != null)
                {
                    TextViewUserToCheckin.setText(OneContact.getUserCode().toString());
                    StaticText1.setText(OneContact.getName());
                    conf_inout = "IN";
                    CheckUser("2",TextViewUserToCheckin.getText().toString() );
                    FazRefresh(1);

                }
            }

        });
        // Initialize Show Checkout  button
        Button button4 = (Button) findViewById(R.id.main_button_Checkout);
        button4.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                int UserSelected = lView.getCheckedItemPosition();
                Contact OneContact = (Contact)lView.getItemAtPosition(UserSelected);
                if(OneContact != null)
                {
                    TextViewUserToCheckin.setText(OneContact.getUserCode().toString());
                    StaticText1.setText(OneContact.getName());
                    conf_inout = "OUT";
                    CheckUser("2",TextViewUserToCheckin.getText().toString() );
                    FazRefresh(1);

                }
            }

        });
        // Initialize Show Configuration  button
        Button button2 = (Button) findViewById(R.id.main_button_Config);
        button2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(0);

            }
        });
        // Initialize Show Print  button
        Button button3 = (Button) findViewById(R.id.main_button_Print);
        button3.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                int UserSelected = lView.getCheckedItemPosition();
                Contact OneContact = (Contact)lView.getItemAtPosition(UserSelected);
                if(OneContact != null)
                {
                    TextViewUserToCheckin.setText(OneContact.getUserCode().toString());
                    StaticText1.setText(OneContact.getName());
                    PrintUser(TextViewUserToCheckin.getText().toString() );
                }
            }
        });


        // Initialize Show Search  button
        Button button5 = (Button) findViewById(R.id.main_button_Search);
        button5.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                adpt  = new SimpleAdapter(contactList,getApplicationContext());
                final ListView lView = (ListView) findViewById(R.id.listview);
                lView.setAdapter(adpt);
                FazRefresh(2);
            }
        });
        // Initialize Show Print  button

        lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {

                int UserSelected = lView.getCheckedItemPosition();
                Contact OneContact = (Contact)lView.getItemAtPosition(UserSelected);
                if(OneContact != null)
                {
                    TextViewUserToCheckin.setText(OneContact.getUserCode().toString());
                    StaticText1.setText(OneContact.getName());

                }
//			Toast.makeText(MainActivity.this, "Item with id ["+id+"] - Position ["+position+"] - Planet ["+()+"]", Toast.LENGTH_SHORT).show();
            }

        });












    statusMessage = (TextView)findViewById(R.id.textWait);
        barcodeValue = (TextView)findViewById(R.id.textWait);

        autoFocus = (CompoundButton) findViewById(R.id.auto_focus);
        useFlash = (CompoundButton) findViewById(R.id.use_flash);

        findViewById(R.id.read_barcode).setOnClickListener(this);
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.read_barcode) {
            // launch barcode activity.
            Intent intent = new Intent(this, BarcodeCaptureActivity.class);
            intent.putExtra(BarcodeCaptureActivity.AutoFocus, autoFocus.isChecked());
            intent.putExtra(BarcodeCaptureActivity.UseFlash, useFlash.isChecked());

            startActivityForResult(intent, RC_BARCODE_CAPTURE);
        }

    }

    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * {@link #RESULT_CANCELED} if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     * <p/>
     * <p>You will receive this call immediately before onResume() when your
     * activity is re-starting.
     * <p/>
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @see #startActivityForResult
     * @see #createPendingResult
     * @see #setResult(int)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    statusMessage.setText(R.string.barcode_success);
                    barcodeValue.setText(barcode.displayValue);
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                    //CheckUser("1",barcode.displayValue );
                } else {
                    statusMessage.setText(R.string.barcode_failure);
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                statusMessage.setText(String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    private void initList() {
        FazRefresh(2);

    }
    //
    //   Usado para listar USERS
    //
    private class AsyncListViewLoader extends AsyncTask<String, Void, List<Contact>>
    {
        private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPostExecute(List<Contact> result) {

            try
            {
                contactList = result;
                final TextView TextViewWait = (TextView) findViewById(R.id.textWait);
                if (result.size() > 0)
                {
                    super.onPostExecute(result);
                    dialog.dismiss();
                    adpt.setItemList(result);
                    adpt.notifyDataSetChanged();

                    TextViewWait.setText(Integer.toString(contactList.size()));
                }
                else
                {
                    TextViewWait.setText("Vazio");
                }
            }
            catch(Exception ex) {
                final TextView TextViewWait = (TextView) findViewById(R.id.textWait);
                TextViewWait.setText("ERRO");
                ex.printStackTrace();
            }
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
//			dialog.setMessage("A procurar...");
//			dialog.show();
        }

        @Override
        protected List<Contact> doInBackground(String... params) {
            List<Contact> result = new ArrayList<Contact>();
            contactList.clear();

            try {

                HttpGet request = new HttpGet(params[0]);

                HttpParams httpParameters = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParameters, 600000);
                HttpConnectionParams.setSoTimeout(httpParameters, 600000);

                HttpClient client = new DefaultHttpClient(httpParameters);
                HttpResponse httpResponse;

                httpResponse = client.execute(request);
                int responseCode = httpResponse.getStatusLine().getStatusCode();
                String message = httpResponse.getStatusLine().getReasonPhrase();

                HttpEntity entity = httpResponse.getEntity();
                String response = "";
                if (entity != null) {

                    InputStream instream = entity.getContent();
                    response = convertStreamToString(instream);

                    instream.close();
                }


                String JSONResp = response;
//				String JSONResp = new String(baos.toByteArray());
                JSONResp = JSONResp.replaceAll("\\\\", "");
// 			    Log.v("=======", Integer.toString(JSONResp.length()));
// 			    Log.v("=======", JSONResp);
//    				JSONResp = JSONResp.replaceAll("\"null\"", "\\\"null\"\\");
//    				JSONResp = JSONResp.replaceAll("[^\\p{Print}]","");
//    				JSONResp = JSONResp.replaceAll("\\u00", "");
//    				String hh = "[{\"UserCode\":\"5566\",\"Email\":\"MCAZEVEDO@SONAE.PT\",\"Name\":\"MARTA CRISTINA BRITO AZEVEDO\",\"CompanyNumber\":\"94216\",\"Confirmed\":\"sim\",\"Accommodation\":\"sim\",\"State\":\"null\"}]";
                JSONResp = JSONResp.substring(1,JSONResp.length()-1);
                //JSONResp = JSONResp.replaceAll("[^\\p{Print}]","");
//    				JSONArray arr = new JSONArray(JSONResp.substring(1,JSONResp.length()-1));
                JSONArray arr = new JSONArray(JSONResp);
                for (int i=0; i < arr.length(); i++) {
                    result.add(convertContact(arr.getJSONObject(i)));
                }
                int a = 1;
                return result;

            }
            catch(IOException ex){
                ex.printStackTrace();
                Log.v("MEUERRO", "IOException");
            }
            catch(Exception exa){
                exa.printStackTrace();
            }

                return null;

        }

        private Contact convertContact(JSONObject obj) throws JSONException {
            String userCode = obj.getString("UserCode");
            String name = obj.getString("Name");
            String companyNumber = obj.getString("CompanyNumber");
            String senhas = obj.getString("Senhas");
            String ipod = obj.getString("Ipod");
            String mesa = obj.getString("Mesa");
            String state = obj.getString("State");

            return new Contact(userCode, name, companyNumber, senhas,ipod,mesa,   state);
        }

    }
    private class AsyncPingLoader extends AsyncTask<String, Void, String>
    {
//    	private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPostExecute(String result)
        {
            try {
                super.onPostExecute(result);

                EditText TextFim = (EditText) findViewById(R.id.textToSearch);
                Long tsLongFim = System.currentTimeMillis()/1000;
                Editable aa = TextFim.getText();
                long tsLongInicio = Long.parseLong(aa.toString());

                tsLongFim = (tsLongFim - tsLongInicio);
                TextFim.setText(String.valueOf(tsLongFim) + " ms");
            }catch(Exception ex) {
                ex.printStackTrace();
            }

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            EditText TextFim = (EditText) findViewById(R.id.textToSearch);
            Long tsLong = System.currentTimeMillis()/1000;
            TextFim.setText(String.valueOf(tsLong));
        }

        @Override
        protected String doInBackground(String... params) {

            try {
                URL u = new URL(params[0]);

                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("GET");

                conn.connect();
                InputStream is = conn.getInputStream();

                // Read the stream
                byte[] b = new byte[100024];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                while ( is.read(b) != -1 )
                {
                    baos.write(b);
                }
                String JSONResp = new String(baos.toByteArray());

                return JSONResp;
            }
            catch(Throwable t) {
                t.printStackTrace();
            }
            return null;

        }

    }
    public String getLocalIpAddress(boolean useIPv4)
    {
//        try {
//            String ipv4="";
//            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
//                NetworkInterface intf = en.nextElement();
//                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
//                    InetAddress inetAddress = enumIpAddr.nextElement();
//                    if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4 = inetAddress.getHostAddress())) {
//
//                        String ip = inetAddress.getHostAddress().toString();
//                        System.out.println("ip---::" + ip);
//
//                        return ipv4;
//                    }
//                }
//            }
//        } catch (Exception ex) {
//            Log.e("IP Address", ex.toString());
//        }

        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex)
        {
            return ex.getMessage();

        }
        return "";


    }
    private int ReadConfig() {

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        DataInputStream dis = null;
        byte[] buffer = new byte[300];
        try {


            fis = new FileInputStream(getFilesDir()+File.separator+FileName);
            BufferedReader r = new BufferedReader(new InputStreamReader(fis,"ISO-8859-1"));
            StringBuilder total = new StringBuilder();


            String Linha;
            while ((Linha = r.readLine()) != null) {
//					String Linha = dis.readLine();
                if(Linha.toUpperCase().contains("SERVERIP"))
                {
                    conf_serverIp=Linha.substring(Linha.indexOf(":")+1, Linha.length());
                }
                if(Linha.toUpperCase().contains("SERVERPORT"))
                {
                    conf_serverPort=Linha.substring(Linha.indexOf(":")+1, Linha.length());
                }
                if(Linha.toUpperCase().contains("POSTO"))
                {
                    conf_postId=Linha.substring(Linha.indexOf(":")+1, Linha.length());
                }
                if(Linha.toUpperCase().contains("PINGTIMES"))
                {
                    conf_pingTimes=Linha.substring(Linha.indexOf(":")+1, Linha.length());
                }


            }
        } catch (FileNotFoundException e) {

            WriteConfig("INIT");
            return 1;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }
    private void WriteConfig(String Type) {

        BufferedWriter bufferedWriter;

        if(Type.equals("INIT")) {
            try {

                bufferedWriter = new BufferedWriter(new OutputStreamWriter( new FileOutputStream( new File(getFilesDir()+File.separator+FileName)), "ISO-8859-1"));

                bufferedWriter.write("ServerIP               :192.168.0.105"+System.getProperty ("line.separator"));
                bufferedWriter.write("ServerPort             :3334"+System.getProperty ("line.separator"));
                bufferedWriter.write("Posto                  :0001"+System.getProperty ("line.separator"));
                bufferedWriter.write("PingTimes              :10"+System.getProperty ("line.separator"));

                bufferedWriter.close();

            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        if(Type.equals("UPDATE")) {
            try {

                bufferedWriter = new BufferedWriter(new OutputStreamWriter( new FileOutputStream( new File(getFilesDir()+File.separator+FileName)), "ISO-8859-1"));
                bufferedWriter.write("ServerIP               :" + conf_serverIp + System.getProperty ("line.separator"));
                bufferedWriter.write("ServerPort             :" + conf_serverPort + System.getProperty ("line.separator"));
                bufferedWriter.write("Posto                  :" + conf_postId + System.getProperty ("line.separator"));
                bufferedWriter.write("PingTimes              :" + conf_pingTimes + System.getProperty ("line.separator"));

                bufferedWriter.close();

            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }
    private void FazRefresh(int Tipo) {
        // Pesquisa no servidor os que fazem match com a string
        if(Tipo == 1)
        {
            final EditText editTextToSearch = (EditText) findViewById(R.id.textToSearch);
            final TextView TextViewWait = (TextView) findViewById(R.id.textWait);
            if(editTextToSearch.length()> 2)
            {
                //    	conf_serverIp="192.168.0.101";
                TextViewWait.setText("WAIT");
                String Comando = "http://"+ conf_serverIp +":" + conf_serverPort + "/PostService/ul?securityCode=" + SecurityCode + "&textToSearch=" + editTextToSearch.getText().toString() + "&postId=3434&clientIp=192.168.56.1";
                // Por causa dos espaçoes que dava problemas na recepção dos resultados > mudar para "_". No Webservice volta a meter os espaços
                Comando = Comando.replaceAll(" ", "_");
                (new AsyncListViewLoader()).execute(Comando);
            }
        }
        // Todos os registos
        if(Tipo == 2)
        {
            final TextView TextViewWait = (TextView) findViewById(R.id.textWait);
            TextViewWait.setText("WAIT");
            String Comando = "http://"+ conf_serverIp +":" + conf_serverPort + "/PostService/ul?securityCode=" + SecurityCode + "&textToSearch=" + "&postId=3434&clientIp=192.168.56.1";
            // Por causa dos espaçoes que dava problemas na recepção dos resultados > mudar para "_". No Webservice volta a meter os espaços
            Comando = Comando.replaceAll(" ", "_");
            (new AsyncListViewLoader()).execute(Comando);
        }
    }
    private void FazPing()
    {
//    	conf_serverIp="192.168.0.101";
        String Comando = "http://"+ conf_serverIp +":" + conf_serverPort + "/PostService/Ping";
        // Por causa dos espaçoes que dava problemas na recepção dos resultados > mudar para "_". No Webservice volta a meter os espaços
        Comando = Comando.replaceAll(" ", "_");
        (new AsyncPingLoader()).execute(Comando);




    }
    public void CheckUser(String TokenType, String Code )  {

        String url = "http://" + conf_serverIp + ":" + conf_serverPort + "/PostService/uc?securityCode=" + SecurityCode + "&userCode=" + Code + "&tokenType=" + TokenType + "&postId=" + conf_postId + "&clientIp=" + clientIp + "&inout=" + conf_inout;
        new MyAsyncTask(this).execute(url);

    }
    private void PrintUser(String Code )  {

        String url = "http://" + conf_serverIp + ":" + conf_serverPort + "/PostService/pl?securityCode=" + SecurityCode + "&userCode=" + Code + "&postId=" + conf_postId + "&clientIp=" + clientIp;
        new MyAsyncTask(this).execute(url);

    }
    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
    public class MyAsyncTask extends AsyncTask<String, Void, String>
    {
        private static final int REGISTRATION_TIMEOUT = 3 * 1000;
        private static final int WAIT_TIMEOUT = 30 * 1000;
        private final HttpClient httpclient = new DefaultHttpClient();

        final HttpParams params = httpclient.getParams();
        HttpResponse response;
        private String content =  null;
        private boolean error = false;

        private Context mContext;
        private int NOTIFICATION_ID = 1;
        private Notification mNotification;
        private NotificationManager mNotificationManager;

        //
        //   Usado para fazer PRINT e USERCHECK
        //
        public MyAsyncTask(Context context)
        {
            this.mContext = context;
            //Get the notification manager
            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        protected void onPreExecute()
        {
//		 createNotification("Data download is in progress","");
        }
        //	 @Override
        protected String doInBackground(String... url)
        {

            try {
                HttpGet request = new HttpGet(url[0]);

                HttpParams httpParameters = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParameters, 600000);
                HttpConnectionParams.setSoTimeout(httpParameters, 600000);

                HttpClient client = new DefaultHttpClient(httpParameters);
                HttpResponse httpResponse;

                httpResponse = client.execute(request);
                int responseCode = httpResponse.getStatusLine().getStatusCode();
                String message = httpResponse.getStatusLine().getReasonPhrase();

                HttpEntity entity = httpResponse.getEntity();

                if (entity != null) {

                    InputStream instream = entity.getContent();
                    String response = convertStreamToString(instream);

//	     			createNotification("Resposta", response);

                    // Closing the input stream will trigger connection release
                    instream.close();
                    return response;
                }
            }

            catch (ClientProtocolException e)
            {
                final TextView TextViewWait = (TextView) findViewById(R.id.textWait);
                //TextViewWait.setText("Http:ClientP");
                Log.w("HTTP2:",e );
                content = e.getMessage();
                error = true;
                cancel(true);
                createNotification("RespostaErro", content);
            }
            catch (ConnectTimeoutException e) {
                final TextView TextViewWait = (TextView) findViewById(R.id.textWait);
                //TextViewWait.setText("Http:TimeOut");
            }
            catch (IOException e)
            {
                final TextView TextViewWait = (TextView) findViewById(R.id.textWait);
                //TextViewWait.setText("Http:IOException");
                Log.w("HTTP3:",e );
                content = e.getMessage();
                error = true;
                cancel(true);
                createNotification("RespostaErro", content);
            }
            catch (Exception e)
            {
                final TextView TextViewWait = (TextView) findViewById(R.id.textWait);
                // TextViewWait.setText("Http:Exception");
                Log.w("HTTP4:",e );
                content = e.getMessage();
                error = true;
                cancel(true);
                createNotification("RespostaErro", content);
            }
            return content;
        }
        protected void onCancelled()
        {
//		createNotification("Error occured during data download",content);
        }
        protected void onPostExecute(String content)
        {
            super.onPostExecute(content);

            try {
//			MainActivity.this.checkResponseForIntent(content);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }


        private void createNotification(String contentTitle, String contentText)
        {

            //Build the notification using Notification.Builder
            Notification.Builder builder = new Notification.Builder(mContext)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setAutoCancel(true)
                    .setContentTitle(contentTitle)
                    .setContentText(contentText);

            //Get current notification
            mNotification = builder.getNotification();
            //Show the notification
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }


    }
    @Override
    protected Dialog onCreateDialog(int id) {

        LayoutInflater inflater;
        final View layout;
        AlertDialog.Builder builder;
        AlertDialog dialog=null;

        switch (id) {


            case DIALOG_CONFIG:


                inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                layout = inflater.inflate(R.layout.config_dialog,(ViewGroup) findViewById(R.id.config_view));

                builder = new AlertDialog.Builder(this);
                builder.setView(layout);
                builder.setTitle("Configuration");
                builder.setPositiveButton("Save",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                conf_serverIp= editTextserverip.getText().toString();
                                conf_serverPort = editTextserverport.getText().toString();
                                conf_postId = editTextpostId.getText().toString();

                                WriteConfig("UPDATE");

                                dialog.dismiss();
                            }
                        });
                builder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                dialog = builder.create();

                // Hide input window
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                break;

        }

        return dialog;
    }
    protected void onPrepareDialog(int id, Dialog dialog) {

        switch (id) {
            case DIALOG_CONFIG:

                // Inicializa edit text s das configurações

                editTextserverip = (EditText) dialog.findViewById(R.id.config_dialog_edit_text_serverip);
                editTextserverport = (EditText) dialog.findViewById(R.id.config_dialog_edit_text_serverport);
                editTextpostId = (EditText) dialog.findViewById(R.id.config_dialog_edit_text_postid);

                editTextserverip.setText(conf_serverIp);
                editTextserverport.setText(conf_serverPort);
                editTextpostId.setText(conf_postId);

//	            // Initialize Show Ping  button

                Button button = (Button) dialog.findViewById(R.id.config_dialog_button_ping);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FazPing();
                    }
                });

        }

    }


}
