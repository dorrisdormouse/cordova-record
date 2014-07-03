package com.abstr.togg;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Random;
import com.todoroo.aacenc.AACEncoder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRecorder.AudioSource;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class ToggRec extends CordovaPlugin {
    public String fileName = "";
    public CallbackContext cllBack;
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "talkTag";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 16000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    public int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private int volChangeCount=0;
    private static int[] mSampleRates = new int[] { RECORDER_SAMPLERATE };
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	
    	//callbackContext.success("execute");
    	//Log.d("audiorec","execute");
        if (action.equals("srec")) {
            //fileName = args.getString(0);
        	Log.d("audiorec","record");
        	//callbackContext.error("record");
            cllBack = callbackContext;
            this.startRecording();
            return true;
        }else if (action.equals("stoprecord")) {
        	this.stopRecording();
        	return true;
        }
        return false;
        
        /*
        callbackContext.success(action);
        return true;
        */
    }
    private String getTempFilename(){
		 String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
        
        if(!file.exists()){
                file.mkdirs();
        }
        
        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);
        
        if(tempFile.exists())
                tempFile.delete();
        
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
}
     public AudioRecord findAudioRecord() {
	        for (int rate : mSampleRates) {
	            for (short audioFormat : new short[] { RECORDER_AUDIO_ENCODING }) {
	                for (short channelConfig : new short[] {RECORDER_CHANNELS}) {
	                    try {
	                        bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
                            
	                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
	                            // check if we can instantiate and have a success
	                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, channelConfig, audioFormat, bufferSize);

	                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
	                                return recorder;
	                        }
	                    } catch (Exception e) {
	                        
	                    }
	                }
	            }
	        }
	        return null;
	        //, AudioFormat.CHANNEL_IN_STEREO }
	    }
	    private void startRecording(){
            bufferSize= AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
	    	try{
			    	recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);
			    	if (recorder.getState() != AudioRecord.STATE_INITIALIZED){
			    		recorder=findAudioRecord();
			    	}
			    	if (recorder.getState() == AudioRecord.STATE_INITIALIZED){
			    	    recorder.startRecording();
			    	    isRecording = true;
			    	    recordingThread = new Thread(new Runnable() {
			    	        public void run() {
			    	            writeAudioDataToFile();
			    	        }
			    	    }, "AudioRecorder Thread");
			    	    recordingThread.start();
                        Log.d("audiorec","START RECORDING");
			    	    cllBack.success("RECORDING");
	    		}else{
	    			cllBack.error("NOT INIT");
	    		}
	    		
	    	}catch(Exception e){
                Log.d("audiorec",e.getMessage()+":"+e.getStackTrace());
	    		cllBack.error(e.getMessage()+":"+e.getStackTrace());
	    		//cllBack.error("no rec");
	    	}
	    	
	}
	    private void stopRecording(){
	    	if (null != recorder) {
                Log.d("audiorec","STOP RECORDING");
	            isRecording = false;
	            recorder.stop();
	            recorder.release();
	            recorder = null;
	            recordingThread = null;
	            cllBack.success("STOP RECORDING");
            }
	}
    private void writeAudioDataToFile(){
		
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        
        File file = new File(filename);
    	if(!file.exists()) { 
    		try{
    		file.createNewFile();
    		}catch(Exception e){
    		
    		
    		}
    	}
        
        FileOutputStream os = null;
        
        try {
                os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        }
        
        int read = 0;
        
        if(null != os){
        	
                while(isRecording){
                        read = recorder.read(data, 0, bufferSize);
                        
                        if(AudioRecord.ERROR_INVALID_OPERATION != read){
                                try {
                                     Log.i("audiorec","WRITING TO DISK");
                                	    os.write(data);
                                } catch (IOException e) {
                                        e.printStackTrace();
                                }
                        }
                }
                
                try {
                        os.close();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }
}
}