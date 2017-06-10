 /**
  * © Copyright IBM Corporation 2015
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  **/

package com.ibm.watson.developer_cloud.android.examples;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Date;
import java.text.DateFormat;

import android.app.ActionBar;
import android.app.Application;
import android.app.FragmentTransaction;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
//import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.app.Fragment;

/*import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;*/

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// IBM Watson SDK
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.SpeechConfiguration;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.ISpeechDelegate;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.android.speech_common.v1.TokenProvider;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IBMSpeechToText extends Activity {

	private static final String TAG = "IBMSpeechToText";
    static FirebaseStorage storage = FirebaseStorage.getInstance();
    private static StorageReference mStorage = storage.getReference();

    ActionBar.Tab tabSTT;
    FragmentTabSTT fragmentTabSTT = new FragmentTabSTT();
    //FragmentTabTTS fragmentTabTTS = new FragmentTabTTS();

    public static class FragmentTabSTT extends FragmentActivity implements ISpeechDelegate {

        private final String FILE_NAME = DateFormat.getDateTimeInstance().format(new Date()) + "_rawtext";
        File tempFile;
        String finalResultText = "Meeting Manager:" + "\n";

        // session recognition results
        private static String mRecognitionResults = "";

        private enum ConnectionState {
            IDLE, CONNECTING, CONNECTED
        }

        ConnectionState mState = ConnectionState.IDLE;
        public View mView = null;
        public Context mContext = null;
        public JSONObject jsonModels = null;
        private Handler mHandler = null;

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            mView = inflater.inflate(R.layout.tab_stt, container, false);
            mContext = getApplicationContext();
            mHandler = new Handler();

            setText();
            if (initSTT() == false) {
                displayResult("Error: no authentication credentials/token available, please enter your authentication information");
                return mView;
            }

            if (jsonModels == null) {
                jsonModels = new STTCommands().doInBackground();
                if (jsonModels == null) {
                    displayResult("Please, check internet connection.");
                    return mView;
                }
            }
            addItemsOnSpinnerModels();

            displayStatus("please, press the button to start speaking");

            Button buttonRecord = (Button) mView.findViewById(R.id.buttonRecord);

            buttonRecord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    /** Getting Cache Directory*/
                    File cDir = getBaseContext().getCacheDir();

                    /** Getting a reference to temporary file, if created earlier*/
                    tempFile = new File(cDir.getPath() + "/" + FILE_NAME + ".txt");

                    if (mState == ConnectionState.IDLE) {
                        mState = ConnectionState.CONNECTING;
                        Log.d(TAG, "onClickRecord: IDLE -> CONNECTING");
                        Spinner spinner = (Spinner) mView.findViewById(R.id.spinnerModels);
                        spinner.setEnabled(false);
                        mRecognitionResults = "";
                        displayResult(mRecognitionResults);
                        ItemModel item = (ItemModel) spinner.getSelectedItem();
                        com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText.sharedInstance().setModel(item.getModelName());
                        displayStatus("connecting to the STT service...");
                        // start recognition
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... none) {
                                com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText.sharedInstance().recognize();
                                return null;
                            }
                        }.execute();
                        setButtonLabel(R.id.buttonRecord, "Connecting...");
                        setButtonState(true);
                    } else if (mState == ConnectionState.CONNECTED) {
                        mState = ConnectionState.IDLE;
                        Log.d(TAG, "onClickRecord: CONNECTED -> IDLE");
                        Spinner spinner = (Spinner) mView.findViewById(R.id.spinnerModels);
                        spinner.setEnabled(true);
                        com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText.sharedInstance().stopRecognition();
                        setButtonState(false);

                        FileWriter writer = null;
                        try {
                            writer = new FileWriter(tempFile);
                            //Saving the contents to the file
                            writer.write(finalResultText);
                            //Closing the writer object
                            writer.close();
                            //Toast.makeText(getActivity().getBaseContext(), "Temporarily saved contents in " + tempFile.getPath(), Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //saveText(finalResultText);
                        uploadFirebase(tempFile.getPath(), FILE_NAME);
                        rankSentences(tempFile.getPath());
                    }
                }
            });

            return mView;
        }

        private String getModelSelected() {

            Spinner spinner = (Spinner) mView.findViewById(R.id.spinnerModels);
            ItemModel item = (ItemModel) spinner.getSelectedItem();
            return item.getModelName();
        }

        public URI getHost(String url) {
            try {
                return new URI(url);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return null;
        }

        // initialize the connection to the Watson STT service
        private boolean initSTT() {

            // DISCLAIMER: please enter your credentials or token factory in the lines below
            String username = "96e73e55-12f9-4df4-ac99-505b20c6a029";
            String password = "ARME0XK3gB0S";

            String tokenFactoryURL = getString(R.string.defaultTokenFactory);
            String serviceURL = "wss://stream.watsonplatform.net/speech-to-text/api";

            SpeechConfiguration sConfig = new SpeechConfiguration(SpeechConfiguration.AUDIO_FORMAT_OGGOPUS);
            //SpeechConfiguration sConfig = new SpeechConfiguration(SpeechConfiguration.AUDIO_FORMAT_DEFAULT);
            sConfig.learningOptOut = false; // Change to true to opt-out

            com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText.sharedInstance().initWithContext(this.getHost(serviceURL), getApplicationContext(), sConfig);

            // token factory is the preferred authentication method (service credentials are not distributed in the client app)
            if (tokenFactoryURL.equals(getString(R.string.defaultTokenFactory)) == false) {
                com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText.sharedInstance().setTokenProvider(new MyTokenProvider(tokenFactoryURL));
            }
            // Basic Authentication
            else if (username.equals(getString(R.string.defaultUsername)) == false) {
                com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText.sharedInstance().setCredentials(username, password);
            } else {
                // no authentication method available
                return false;
            }

            com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText.sharedInstance().setModel(getString(R.string.modelDefault));
            com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText.sharedInstance().setDelegate(this);

            return true;
        }

        protected void setText() {

            Typeface roboto = Typeface.createFromAsset(getApplicationContext().getAssets(), "font/Roboto-Bold.ttf");
            Typeface notosans = Typeface.createFromAsset(getApplicationContext().getAssets(), "font/NotoSans-Regular.ttf");

            // title
            TextView viewTitle = (TextView) mView.findViewById(R.id.title);
            String strTitle = getString(R.string.sttTitle);
            SpannableStringBuilder spannable = new SpannableStringBuilder(strTitle);
            spannable.setSpan(new AbsoluteSizeSpan(47), 0, strTitle.length(), 0);
            spannable.setSpan(new CustomTypefaceSpan("", roboto), 0, strTitle.length(), 0);
            viewTitle.setText(spannable);
            viewTitle.setTextColor(0xFF325C80);

            // instructions
            TextView viewInstructions = (TextView) mView.findViewById(R.id.instructions);
            String strInstructions = getString(R.string.sttInstructions);
            SpannableString spannable2 = new SpannableString(strInstructions);
            spannable2.setSpan(new AbsoluteSizeSpan(20), 0, strInstructions.length(), 0);
            spannable2.setSpan(new CustomTypefaceSpan("", notosans), 0, strInstructions.length(), 0);
            viewInstructions.setText(spannable2);
            viewInstructions.setTextColor(0xFF121212);
        }

        public class ItemModel {

            private JSONObject mObject = null;

            public ItemModel(JSONObject object) {
                mObject = object;
            }

            public String toString() {
                try {
                    return mObject.getString("description");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            public String getModelName() {
                try {
                    return mObject.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        protected void addItemsOnSpinnerModels() {

            Spinner spinner = (Spinner) mView.findViewById(R.id.spinnerModels);
            int iIndexDefault = 0;

            JSONObject obj = jsonModels;
            ItemModel[] items = null;
            try {
                JSONArray models = obj.getJSONArray("models");

                // count the number of Broadband models (narrowband models will be ignored since they are for telephony data)
                Vector<Integer> v = new Vector<>();
                for (int i = 0; i < models.length(); ++i) {
                    if (models.getJSONObject(i).getString("name").indexOf("Broadband") != -1) {
                        v.add(i);
                    }
                }
                items = new ItemModel[v.size()];
                int iItems = 0;
                for (int i = 0; i < v.size(); ++i) {
                    items[iItems] = new ItemModel(models.getJSONObject(v.elementAt(i)));
                    if (models.getJSONObject(v.elementAt(i)).getString("name").equals(getString(R.string.modelDefault))) {
                        iIndexDefault = iItems;
                    }
                    ++iItems;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (items != null) {
                ArrayAdapter<ItemModel> spinnerArrayAdapter = new ArrayAdapter<ItemModel>(this, android.R.layout.simple_spinner_item, items);
                spinner.setAdapter(spinnerArrayAdapter);
                spinner.setSelection(iIndexDefault);
            }
        }

        public void displayResult(final String result) {

            finalResultText = finalResultText + result;

            final Runnable runnableUi = new Runnable() {
                @Override
                public void run() {
                    TextView textResult = (TextView) mView.findViewById(R.id.textResult);
                    textResult.setText(result);
                }
            };

            new Thread() {
                public void run() {
                    mHandler.post(runnableUi);
                }
            }.start();
        }

        public void displayStatus(final String status) {
            /*final Runnable runnableUi = new Runnable(){
                @Override
                public void run() {
                    TextView textResult = (TextView)mView.findViewById(R.id.sttStatus);
                    textResult.setText(status);
                }
            };
            new Thread(){
                public void run(){
                    mHandler.post(runnableUi);
                }
            }.start();*/
        }

        /**
         * Change the button's label
         */
        public void setButtonLabel(final int buttonId, final String label) {
            final Runnable runnableUi = new Runnable() {
                @Override
                public void run() {
                    Button button = (Button) mView.findViewById(buttonId);
                    button.setText(label);
                }
            };
            new Thread() {
                public void run() {
                    mHandler.post(runnableUi);
                }
            }.start();
        }

        /**
         * Change the button's drawable
         */
        public void setButtonState(final boolean bRecording) {

            final Runnable runnableUi = new Runnable() {
                @Override
                public void run() {
                    int iDrawable = bRecording ? R.drawable.button_record_stop : R.drawable.button_record_start;
                    Button btnRecord = (Button) mView.findViewById(R.id.buttonRecord);
                    btnRecord.setBackground(getResources().getDrawable(iDrawable));
                }
            };
            new Thread() {
                public void run() {
                    mHandler.post(runnableUi);
                }
            }.start();
        }

        // delegages ----------------------------------------------

        public void onOpen() {
            Log.d(TAG, "onOpen");
            displayStatus("successfully connected to the STT service");
            setButtonLabel(R.id.buttonRecord, "Stop recording");
            mState = ConnectionState.CONNECTED;
        }

        public void onError(String error) {

            Log.e(TAG, error);
            displayResult(error);
            mState = ConnectionState.IDLE;
        }

        public void onClose(int code, String reason, boolean remote) {
            Log.d(TAG, "onClose, code: " + code + " reason: " + reason);
            displayStatus("connection closed");
            setButtonLabel(R.id.buttonRecord, "Record");
            mState = ConnectionState.IDLE;
        }

        public void onMessage(String message) {

            Log.d(TAG, "onMessage, message: " + message);
            try {
                JSONObject jObj = new JSONObject(message);
                // state message
                if (jObj.has("state")) {
                    Log.d(TAG, "Status message: " + jObj.getString("state"));
                }
                // results message
                else if (jObj.has("results")) {
                    //if has result
                    Log.d(TAG, "Results message: ");
                    JSONArray jArr = jObj.getJSONArray("results");
                    for (int i = 0; i < jArr.length(); i++) {
                        JSONObject obj = jArr.getJSONObject(i);
                        JSONArray jArr1 = obj.getJSONArray("alternatives");
                        String str = jArr1.getJSONObject(0).getString("transcript");
                        // remove whitespaces if the language requires it
                        String model = this.getModelSelected();
                        if (model.startsWith("ja-JP") || model.startsWith("zh-CN")) {
                            str = str.replaceAll("\\s+", "");
                        }
                        String strFormatted = Character.toUpperCase(str.charAt(0)) + str.substring(1);
                        if (obj.getString("final").equals("true")) {
                            String stopMarker = (model.startsWith("ja-JP") || model.startsWith("zh-CN")) ? "。" : ". ";
                            mRecognitionResults += strFormatted.substring(0, strFormatted.length() - 1) + stopMarker;
                            displayResult(mRecognitionResults);
                        } else {
                            displayResult(mRecognitionResults + strFormatted);
                        }
                        break;
                    }
                } else {
                    displayResult("unexpected data coming from stt server: \n" + message);
                }

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON");
                e.printStackTrace();
            }
        }

        /**
         * public void saveText(String text){
         * <p>
         * try {
         * //open file for writing
         * OutputStreamWriter out = new OutputStreamWriter(getActivity().getBaseContext().openFileOutput(FILE_NAME, Context.MODE_APPEND));
         * <p>
         * filePath = getActivity().getBaseContext().getFilesDir().getAbsolutePath();
         * <p>
         * //write information to file
         * out.write(" ");
         * out.write(text);
         * out.write('\n');
         * <p>
         * //close file
         * out.close();
         * Toast.makeText(getActivity().getBaseContext(),"Text Saved",Toast.LENGTH_LONG).show();
         * <p>
         * } catch (FileNotFoundException e) {
         * Toast.makeText(getActivity().getBaseContext(), "File could not be found",Toast.LENGTH_LONG).show();
         * }
         * catch (java.io.IOException e) {
         * //if caught
         * Toast.makeText(getActivity().getBaseContext(), "Text Could not be added",Toast.LENGTH_LONG).show();
         * }
         * }
         */ //saveText()

        public void uploadFirebase(String path, final String fileName) {
            //tempFile.getPath()
            /* Uploading the content in firebase */
            if (path != null) {
                //Toast.makeText(getActivity().getBaseContext(), "File taken from cache", Toast.LENGTH_LONG).show();
                Uri file = Uri.fromFile(new File(path));
                StorageReference riversRef = mStorage.child(fileName + ".txt");

                riversRef.putFile(file)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // Get a URL to the uploaded content
                               // Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                Toast.makeText(getBaseContext(), "File stored in Firebase", Toast.LENGTH_LONG).show();
                                downloadButton(fileName);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Toast.makeText(getBaseContext(), "Unable to stored file in Firebase", Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                Toast.makeText(getBaseContext(), "File not found", Toast.LENGTH_LONG).show();
            }
        }

        public void downloadButton(final String fileName) {
            Button downloadraw = (Button)mView.findViewById(R.id.downloadraw);

            downloadraw.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {

                    // Create a storage reference from our app
                    StorageReference storageRef = storage.getReference();

                    // Create a reference with an initial file path and name
                    StorageReference pathReference = storageRef.child(fileName+".txt");

                    /** //Create a reference to a file from a Google Cloud Storage URI
                    StorageReference gsReference = storage.getReferenceFromUrl("gs://speech-d73b3.appspot.com/"+FILE_NAME);

                    //Create a reference from an HTTPS URL
                    //Note that in the URL, characters are URL escaped!
                    StorageReference httpsReference = storage.getReferenceFromUrl("https://firebasestorage.googleapis.com/b/gs://speech-d73b3.appspot.com/o/"+FILE_NAME);

                    File localFile = null;
                    try {
                        localFile = File.createTempFile(fileName, "txt");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } */
                    File storagePath = new File(Environment.getExternalStorageDirectory(), "Meeting_Manager");
                    // Create direcorty if not exists
                    if(!storagePath.exists()) {
                        storagePath.mkdirs();
                        Log.i(TAG, "directory made"+storagePath.toString());
                    }
                    final File localFile = new File(storagePath, "Meeting_Manager.txt");
                    pathReference.getFile(localFile)
                            .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    // Local temp file has been created
                                    Toast.makeText(getBaseContext(), "File downloaded", Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                    Toast.makeText(getBaseContext(), "File could not be downloaded", Toast.LENGTH_LONG).show();
                                }
                            });
                }
            });
            downloadraw.setVisibility(View.VISIBLE);
        }

        public void rankSentences(String path) {
                int k;
                File file1 = new File(path);
                String fileContents1 = "";
                try {
                      FileReader fr1 = new FileReader(file1);

                    fileContents1 = "";
                    while((k =  fr1.read())!=-1){
                        char ch = (char)k;
                        fileContents1 = fileContents1 + ch;
                    }
                } catch(IOException e){

                }

                String sentence=fileContents1.replace('.','\n');
                String [] sentences=sentence.split("\n");
                for(int i=0; i<sentences.length; ++i)
                    System.out.println("i="+i+sentences[i]);

                String[][] words=new String[sentences.length][];

                for(int i=0; i<sentences.length; ++i)
                {
                    String x[]=sentences[i].split(" ");
                    words[i]=new String[x.length];
                    for(int j=0; j<x.length; ++j)
                    {
                        words[i][j]=x[j].toLowerCase().trim();
                    }
                }

                //words 2-D array has words of each sentence stored in it.
                Double similarity[][]=new Double[words.length][words.length];
                Double rank[]=new Double[words.length];
                double sum=0, avg=0;
                for(int i=0; i<words.length; ++i)
                {   rank[i]=0.0;
                    for(int j=0; j<words.length; ++j)
                    {   similarity[i][j]=0.0;
                        double y = (((double)words[i].length+(double)words[j].length)/2);

                        for(int c1=0; c1<words[i].length; ++c1)
                        {for(int c2=0; c2<words[j].length; ++c2)
                        {   if((words[i][c1]).equals(words[j][c2]))
                            similarity[i][j]+=(1/y);
                        }
                        }
                        sum+=similarity[i][j];
                        rank[i]+=similarity[i][j];
                    }
                }

                Object num[];

                ArrayList<Integer> list2 = new ArrayList<>();
                list2.add(0);
                int pos=0;
                k=0;
                double max;
                for(int i=0; i<words.length; ++i)
                { max=rank[0];
                    for(int j=0; j<words.length; ++j)
                    {
                        if(max<rank[j])
                        { max=rank[j];
                            pos=j;}
                    }
                    if(!list2.contains(pos))
                    {
                        list2.add(pos);
                        rank[pos]=0.0;
                        for(int j=0; j<words.length; ++j)
                        {rank[j]-=(similarity[j][pos]);
                            rank[j]=(rank[j]>=1.0)? rank[j]:0.0;
                        }
                    }
                }

                list2.add(sentences.length-1);
                num = list2.toArray();

            for(int i=0; i<num.length; ++i)
                System.out.println("i="+num[i]);

            /** Getting Cache Directory*/
            File cDir = getBaseContext().getCacheDir();
            /** Getting a reference to temporary file, if created earlier*/
            File summaryFile = new File(cDir.getPath() + "/" + FILE_NAME + "_summary.txt");
            FileWriter fw = null;
            try {
                fw = new FileWriter(summaryFile);
                for (k = 0; k < num.length; ++k) {
                    fw.write(sentences[(Integer) num[k]] + '\n');
                    fw.append(System.getProperty("line.separator"));
                }
                fw.close();
            }catch (IOException e) {
                    e.printStackTrace();
            }
            if(summaryFile!=null) {
                Toast.makeText(getBaseContext(), "Summary generated", Toast.LENGTH_LONG).show();
                uploadFirebase(summaryFile.getPath(), FILE_NAME + "_summary");
                downloadSummaryButton(FILE_NAME+"_summary");
            }
        }

        public void downloadSummaryButton(final String fileName) {
            Button downloadsummary = (Button)mView.findViewById(R.id.downloadsummary);

            downloadsummary.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {

                    // Create a storage reference from our app
                    StorageReference storageRef = storage.getReference();

                    // Create a reference with an initial file path and name
                    StorageReference pathReference = storageRef.child(fileName+".txt");

                    File storagePath = new File(Environment.getExternalStorageDirectory(), "Meeting_Manager");
                    // Create direcorty if not exists
                    if(!storagePath.exists()) {
                        storagePath.mkdirs();
                        Log.i(TAG, "directory made"+storagePath.toString());
                    }
                    final File localFile = new File(storagePath, "Meeting_Manager_Summary.txt");
                    pathReference.getFile(localFile)
                            .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    // Local temp file has been created
                                    Toast.makeText(getBaseContext(), "File downloaded", Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                    Toast.makeText(getBaseContext(), "File could not be downloaded", Toast.LENGTH_LONG).show();
                                }
                            });
                }
            });
            downloadsummary.setVisibility(View.VISIBLE);
        }

        public void onAmplitude(double amplitude, double volume) {
            //Logger.e(TAG, "amplitude=" + amplitude + ", volume=" + volume);
        }
    }

    /** public static class FragmentTabTTS extends Fragment {

        public View mView = null;
        public Context mContext = null;
        public JSONObject jsonVoices = null;
        private Handler mHandler = null;

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Log.d(TAG, "onCreateTTS");
            mView = inflater.inflate(R.layout.tab_tts, container, false);
            mContext = getActivity().getApplicationContext();

            setText();
            if (initTTS() == false) {
                TextView viewPrompt = (TextView) mView.findViewById(R.id.prompt);
                viewPrompt.setText("Error: no authentication credentials or token available, please enter your authentication information");
                return mView;
            }

            if (jsonVoices == null) {
                jsonVoices = new TTSCommands().doInBackground();
                if (jsonVoices == null) {
                    return mView;
                }
            }
            addItemsOnSpinnerVoices();
            updatePrompt(getString(R.string.voiceDefault));

            Spinner spinner = (Spinner) mView.findViewById(R.id.spinnerVoices);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                    Log.d(TAG, "setOnItemSelectedListener");
                    final Runnable runnableUi = new Runnable() {
                        @Override
                        public void run() {
                            FragmentTabTTS.this.updatePrompt(FragmentTabTTS.this.getSelectedVoice());
                        }
                    };
                    new Thread() {
                        public void run() {
                            mHandler.post(runnableUi);
                        }
                    }.start();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }
            });

            mHandler = new Handler();
            return mView;
        }

        public URI getHost(String url){
            try {
                return new URI(url);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return null;
        }

        private boolean initTTS() {

            // DISCLAIMER: please enter your credentials or token factory in the lines below

            String username = getString(R.string.TTSUsername);
            String password = getString(R.string.TTSPassword);
            String tokenFactoryURL = getString(R.string.defaultTokenFactory);
            String serviceURL = "https://stream.watsonplatform.net/text-to-speech/api";

            TextToSpeech.sharedInstance().initWithContext(this.getHost(serviceURL));

            // token factory is the preferred authentication method (service credentials are not distributed in the client app)
            if (tokenFactoryURL.equals(getString(R.string.defaultTokenFactory)) == false) {
                TextToSpeech.sharedInstance().setTokenProvider(new MyTokenProvider(tokenFactoryURL));
            }
            // Basic Authentication
            else if (username.equals(getString(R.string.defaultUsername)) == false) {
                TextToSpeech.sharedInstance().setCredentials(username, password);
            } else {
                // no authentication method available
                return false;
            }
            TextToSpeech.sharedInstance().setLearningOptOut(false); // Change to true to opt-out

            TextToSpeech.sharedInstance().setVoice(getString(R.string.voiceDefault));

            return true;
        }

        protected void setText() {

            Typeface roboto = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), "font/Roboto-Bold.ttf");
            Typeface notosans = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), "font/NotoSans-Regular.ttf");

            TextView viewTitle = (TextView)mView.findViewById(R.id.title);
            String strTitle = getString(R.string.ttsTitle);
            SpannableString spannable = new SpannableString(strTitle);
            spannable.setSpan(new AbsoluteSizeSpan(47), 0, strTitle.length(), 0);
            spannable.setSpan(new CustomTypefaceSpan("", roboto), 0, strTitle.length(), 0);
            viewTitle.setText(spannable);
            viewTitle.setTextColor(0xFF325C80);

            TextView viewInstructions = (TextView)mView.findViewById(R.id.instructions);
            String strInstructions = getString(R.string.ttsInstructions);
            SpannableString spannable2 = new SpannableString(strInstructions);
            spannable2.setSpan(new AbsoluteSizeSpan(20), 0, strInstructions.length(), 0);
            spannable2.setSpan(new CustomTypefaceSpan("", notosans), 0, strInstructions.length(), 0);
            viewInstructions.setText(spannable2);
            viewInstructions.setTextColor(0xFF121212);
        }

        public class ItemVoice {

            public JSONObject mObject = null;

            public ItemVoice(JSONObject object) {
                mObject = object;
            }

            public String toString() {
                try {
                    return mObject.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        public void addItemsOnSpinnerVoices() {

            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerVoices);
            int iIndexDefault = 0;

            JSONObject obj = jsonVoices;
            ItemVoice [] items = null;
            try {
                JSONArray voices = obj.getJSONArray("voices");
                items = new ItemVoice[voices.length()];
                for (int i = 0; i < voices.length(); ++i) {
                    items[i] = new ItemVoice(voices.getJSONObject(i));
                    if (voices.getJSONObject(i).getString("name").equals(getString(R.string.voiceDefault))) {
                        iIndexDefault = i;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (items != null) {
		        ArrayAdapter<ItemVoice> spinnerArrayAdapter = new ArrayAdapter<ItemVoice>(getActivity(), android.R.layout.simple_spinner_item, items);
		        spinner.setAdapter(spinnerArrayAdapter);
		        spinner.setSelection(iIndexDefault);
            }
        }

        // return the selected voice
        public String getSelectedVoice() {

            // return the selected voice
            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerVoices);
            ItemVoice item = (ItemVoice)spinner.getSelectedItem();
            String strVoice = null;
            try {
                strVoice = item.mObject.getString("name");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return strVoice;
        }

        // update the prompt for the selected voice
        public void updatePrompt(final String strVoice) {

            TextView viewPrompt = (TextView)mView.findViewById(R.id.prompt);
            if (strVoice.startsWith("en-US") || strVoice.startsWith("en-GB")) {
                viewPrompt.setText(getString(R.string.ttsEnglishPrompt));
            } else if (strVoice.startsWith("es-ES")) {
                viewPrompt.setText(getString(R.string.ttsSpanishPrompt));
            } else if (strVoice.startsWith("fr-FR")) {
                viewPrompt.setText(getString(R.string.ttsFrenchPrompt));
            } else if (strVoice.startsWith("it-IT")) {
                viewPrompt.setText(getString(R.string.ttsItalianPrompt));
            } else if (strVoice.startsWith("de-DE")) {
                viewPrompt.setText(getString(R.string.ttsGermanPrompt));
            } else if (strVoice.startsWith("ja-JP")) {
                viewPrompt.setText(getString(R.string.ttsJapanesePrompt));
            }
        }
    }*/

    public class MyTabListener implements ActionBar.TabListener {

        FragmentTabSTT fragment;
        public MyTabListener(FragmentTabSTT fragment) {
            this.fragment = fragment;
        }

        @Override
        public void onTabSelected(android.app.ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            //fragmentTransaction.replace(R.id.fragment_container, fragment);
        }

        @Override
        public void onTabUnselected(android.app.ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            //fragmentTransaction.remove(fragment);
        }

        @Override
        public void onTabReselected(android.app.ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            //nothing
        }
    }


    public static class STTCommands extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... none) {

            return com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText.sharedInstance().getModels();
        }
    }

   /** public static class TTSCommands extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... none) {

            return TextToSpeech.sharedInstance().getVoices();
        }
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        StorageReference mStorageRef;
        mStorageRef = FirebaseStorage.getInstance().getReference();

		// Strictmode needed to run the http/wss request for devices > Gingerbread
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
				
		//setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_tab_text);

        ActionBar action = getActionBar();
        action.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        tabSTT = action.newTab().setText("Meeting Recorder");
       // tabTTS = actionBar.newTab().setText("Text to Speech");

       tabSTT.setTabListener(new MyTabListener(fragmentTabSTT));
       // tabTTS.setTabListener(new MyTabListener(fragmentTabTTS));

        action.addTab(tabSTT);
      //  actionBar.addTab(tabTTS);

        //actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.parseColor("#B5C0D0")));
	}

    static class MyTokenProvider implements TokenProvider {

        String m_strTokenFactoryURL = null;

        public MyTokenProvider(String strTokenFactoryURL) {
            m_strTokenFactoryURL = strTokenFactoryURL;
        }

        public String getToken() {

            Log.d(TAG, "attempting to get a token from: " + m_strTokenFactoryURL);
            try {
                // DISCLAIMER: the application developer should implement an authentication mechanism from the mobile app to the
                // server side app so the token factory in the server only provides tokens to authenticated clients
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(m_strTokenFactoryURL);
                HttpResponse executed = httpClient.execute(httpGet);
                InputStream is = executed.getEntity().getContent();
                StringWriter writer = new StringWriter();
                IOUtils.copy(is, writer, "UTF-8");
                String strToken = writer.toString();
                Log.d(TAG, strToken);
                return strToken;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /** public void playTTS(View view) throws JSONException {

        TextToSpeech.sharedInstance().setVoice(fragmentTabTTS.getSelectedVoice());
        Log.d(TAG, fragmentTabTTS.getSelectedVoice());

		//Get text from text box
		textTTS = (TextView)fragmentTabTTS.mView.findViewById(R.id.prompt);
		String ttsText=textTTS.getText().toString();
		Log.d(TAG, ttsText);
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(textTTS.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);

		//Call the sdk function
		TextToSpeech.sharedInstance().synthesize(ttsText);
	} */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}