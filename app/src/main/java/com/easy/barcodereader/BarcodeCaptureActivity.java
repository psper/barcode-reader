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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.easy.barcodereader.ui.camera.CameraSource;
import com.easy.barcodereader.ui.camera.CameraSourcePreview;

import com.easy.barcodereader.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Activity for the multi-tracker app.  This app detects barcodes and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and ID of each barcode.
 */
public final class BarcodeCaptureActivity extends AppCompatActivity {
    private static final String TAG = "Barcode-reader";

    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String BarcodeObject = "Barcode";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

    // helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

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

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.barcode_capture);

        Button button = (Button) findViewById(R.id.btnContinue);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Button button1 = (Button) findViewById(R.id.btnContinue);
                button1.setVisibility(View.INVISIBLE);
                TextView txtUser = (TextView) findViewById(R.id.textMsg);
                txtUser.setText("");

                LinearLayout li=(LinearLayout) findViewById(R.id.topLayout);
                li.setBackgroundColor(Color.parseColor("#000000"));
            }
        });

        /////////////////////////////////////////////////////////////

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

        FazRefresh(2);


        //////////////////////////////////////////////////////////

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<BarcodeGraphic>) findViewById(R.id.graphicOverlay);

        // read parameters from the intent used to launch the activity.
        boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
        boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraPermission();
        }

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        Snackbar.make(mGraphicOverlay, "Tap to capture. Pinch/Stretch to zoom",
                Snackbar.LENGTH_LONG)
                .show();
    }


    //////////////////////////////////////////////////////////
    public String getLocalIpAddress(boolean useIPv4)
    {
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


    private void FazRefresh(int Tipo) {

                String Comando = "http://"+ conf_serverIp +":" + conf_serverPort + "/PostService/ul?securityCode=" + SecurityCode + "&textToSearch=" + " " + "&postId=3434&clientIp=192.168.56.1";
                // Por causa dos espaçoes que dava problemas na recepção dos resultados > mudar para "_". No Webservice volta a meter os espaços
                Comando = Comando.replaceAll(" ", "_");
                (new AsyncListViewLoader()).execute(Comando);
    }
    public void getUser(String Code ) {
        //contactList.indexOf()

    }
    public void CheckUser(String TokenType, String Code )  {

        String url = "http://" + conf_serverIp + ":" + conf_serverPort + "/PostService/ucqrcode?securityCode=" + SecurityCode + "&userQRCode=" + Code + "&tokenType=" + TokenType + "&postId=" + conf_postId + "&clientIp=" + clientIp + "&inout=" + conf_inout;
        new BarcodeCaptureActivity.MyAsyncTask(this).execute(url);

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

            //WriteConfig("INIT");
            return 1;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    //////////////////////////////////////////////////////////


    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        findViewById(R.id.topLayout).setOnClickListener(listener);
        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean b = scaleGestureDetector.onTouchEvent(e);

        boolean c = gestureDetector.onTouchEvent(e);

        //return b || c || super.onTouchEvent(e);

        return true;
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay);
        barcodeDetector.setProcessor(
                new MultiProcessor.Builder<>(barcodeFactory).build());

        if (!barcodeDetector.isOperational()) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f);

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(
                    autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        }

        mCameraSource = builder
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            boolean autoFocus = getIntent().getBooleanExtra(AutoFocus,false);
            boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
            createCameraSource(autoFocus, useFlash);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * onTap returns the tapped barcode result to the calling Activity.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the activity is ending.
     */
    private boolean onTap(float rawX, float rawY) {

        Button button1 = (Button) findViewById(R.id.btnContinue);
        if(button1.getVisibility() == View.VISIBLE)
        {
            return false;
        }
        // Find tap point in preview frame coordinates.
        int[] location = new int[2];
        mGraphicOverlay.getLocationOnScreen(location);
        float x = (rawX - location[0]) / mGraphicOverlay.getWidthScaleFactor();
        float y = (rawY - location[1]) / mGraphicOverlay.getHeightScaleFactor();

        // Find the barcode whose center is closest to the tapped point.
        Barcode best = null;
        float bestDistance = Float.MAX_VALUE;
        for (BarcodeGraphic graphic : mGraphicOverlay.getGraphics()) {
            Barcode barcode = graphic.getBarcode();
            if (barcode.getBoundingBox().contains((int) x, (int) y)) {
                // Exact hit, no need to keep looking.
                best = barcode;
                break;
            }
            float dx = x - barcode.getBoundingBox().centerX();
            float dy = y - barcode.getBoundingBox().centerY();
            float distance = (dx * dx) + (dy * dy);  // actually squared distance
            if (distance < bestDistance) {
                best = barcode;
                bestDistance = distance;
            }
        }

        if (best != null) {
            Intent data = new Intent();
            data.putExtra(BarcodeObject, best);
            setResult(CommonStatusCodes.SUCCESS, data);
            CheckUser("3", best.displayValue );
            getUser( best.displayValue );
            //final TextView TextViewWait = (TextView) findViewById(R.id.textMsg);
            //TextViewWait.setText(best.displayValue);
            //finish();
            return true;
        }
        return false;
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }
    }
    private class AsyncListViewLoader extends AsyncTask<String, Void, List<Contact>>
    {
        private final ProgressDialog dialog = new ProgressDialog(BarcodeCaptureActivity.this);

        @Override
        protected void onPostExecute(List<Contact> result) {

            try
            {

                contactList = result;
                //final TextView TextViewWait = (TextView) findViewById(R.id.textWait);
                if (result.size() > 0)
                {
                    super.onPostExecute(result);
                    dialog.dismiss();
                    adpt.setItemList(result);
                    adpt.notifyDataSetChanged();

                    //TextViewWait.setText(Integer.toString(contactList.size()));
                }
                else
                {
                    //TextViewWait.setText("Vazio");
                }
            }
            catch(Exception ex) {
               // final TextView TextViewWait = (TextView) findViewById(R.id.textWait);
                //TextViewWait.setText("ERRO");
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

	     			//createNotification("Resposta", response);


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
                String jjson = content.replace("\"", "'");
                jjson = jjson.replace("\\", " ").substring(1, jjson.length()-2);

                JSONObject wrapperObject = new JSONObject(jjson.replace(" ", ""));
                String ResponseCode= wrapperObject .getString("ResponseCode").toLowerCase();
                String ResponseUser= wrapperObject .getString("ResponseUser").toLowerCase();

                final TextView TextViewWait = (TextView) findViewById(R.id.textMsg);
                TextViewWait.setText(ResponseUser);

                LinearLayout li=(LinearLayout) findViewById(R.id.topLayout);
                Button btnContinue = (Button)findViewById(R.id.btnContinue);

                switch (ResponseCode) {
                    case "0":
                        //Sucesso
                        li.setBackgroundColor(Color.parseColor("#7fff00"));
                        TextViewWait.setTextColor(Color.parseColor("#000000"));
                        btnContinue.setVisibility(View.VISIBLE);
                        break;
                    case "-1":
                        //User Inexistente
                        li.setBackgroundColor(Color.parseColor("#ff0000"));
                        TextViewWait.setTextColor(Color.parseColor("#FFFFFF"));
                        TextViewWait.setText("Utilizador inexistente");
                        btnContinue.setVisibility(View.VISIBLE);
                        break;
                    case "-2":
                        //Já entrou
                        li.setBackgroundColor(Color.parseColor("#0000ff"));
                        TextViewWait.setTextColor(Color.parseColor("#FFFFFF"));
                        btnContinue.setVisibility(View.VISIBLE);
                        break;
                }


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
}
