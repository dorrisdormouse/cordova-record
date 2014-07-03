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
import com.example.voiceeffect.filters.LowPassFilter;
import com.example.voiceeffect.filters.PitchShifterEffect;
import com.example.voiceeffect.filters.UnderWaterEffect;
import com.example.voiceeffect.filters.VocoderEffect;

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
    private String rpIntent = "testfile";
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
            copyWaveFile(getTempFilename(),getFilename(),0);
	}
    private String getFilename(){
         String filepath = Environment.getExternalStorageDirectory().getPath();
         File file = new File(filepath,AUDIO_RECORDER_FOLDER);
         
         if(!file.exists()){
                 file.mkdirs();
         }
         
         return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
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
     private void copyWaveFile(String inFilename,String outFilename, int effect){
		 FileInputStream in = null;
		FileOutputStream out = null;
		File f = new File(outFilename);
		    try{
		    	f.createNewFile();
		    }catch(Exception e){
		    	
		    }
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = RECORDER_SAMPLERATE;
		int channels = 2;
		long byteRate = 16 * RECORDER_SAMPLERATE * channels/8;
		
		byte[] data = new byte[bufferSize];
         
         
         try {
        	 in = new FileInputStream(inFilename);
 			out = new FileOutputStream(f);
 			totalAudioLen = in.getChannel().size();
 			totalDataLen = totalAudioLen + 36;
 			
 			short[] shorts = new short[data.length/2];
 			
 			AACEncoder aac=new AACEncoder();//Do not change the package of this class
                 String outputFile=Environment.getExternalStorageDirectory().getPath()+"/"+AUDIO_RECORDER_FOLDER+"/"+rpIntent+"_temp.3gp";
                 aac.init(64000, 1, RECORDER_SAMPLERATE, 16, outputFile);
                 PitchShifterEffect ps = null;
                 VocoderEffect ve = null;
                 UnderWaterEffect uwe=null;
                 
                 LowPassFilter lpf=null;
                 switch (effect){
                 case 0:
                	 break;
                 case 1:
                 	//init PitchShift
                 	 ps=new PitchShifterEffect(shorts.length);
                     ps.setFftFrameSize(1024);//8);
                     
                     ps.setOversampling(4);//Increasing this value increases the sound quality but greatly reduces productivity
                     /*
                       The pitchShift factor is a value between 0.5 (one octave down) and 2. (one octave up).
                       A value of exactly 1 does not change the pitch.
                      */
                     ps.setPitchShift(2);
                     ps.setSampleRate(RECORDER_SAMPLERATE);
                     break;
                 case 2:
                	//init PitchShift
                 	 ps=new PitchShifterEffect(shorts.length);
                     ps.setFftFrameSize(1024);//8);
                     
                     ps.setOversampling(4);//Increasing this value increases the sound quality but greatly reduces productivity
                     /*
                       The pitchShift factor is a value between 0.5 (one octave down) and 2. (one octave up).
                       A value of exactly 1 does not change the pitch.
                      */
                     ps.setPitchShift(0.5);
                     ps.setSampleRate(RECORDER_SAMPLERATE);
                     break;
                 case 3:
                 	/*Constructs a Vocoder.

                 	Parameters:
                 	windowSize int: the number of sample frames to use for each FFT analysis. Smaller window sizes will have better performance, but lower sound quality. the window size must also be a power of two, which is a requirement for using an FFT.
                 	windowCount int: the number of overlapping windows to use. this must be at least 1 with larger values causing the analysis windows to overlap with each other to a greater degree. For instance, with a windowSize of 1024 and a windowCount of 2, a 1024 sample frame FFT will be calculated every 512 sample frames. With 3 windows, every 341 samples, and so forth. More windows generally equates to better quality.
                 	audioSampleRate 
                 	*/
                 	
                 	  ve=new VocoderEffect(1024,4,44.1f);
                      ve.init();
                      break;
                 case 4:
                 	//open underwater.wav. (16bit,mono, 44100,pcm)---it is very important
                     /*
                 	AssetFileDescriptor descriptor = getAssets().openFd("underwater.wav");
                 	File file = new File(Environment.getExternalStorageDirectory().getPath()+"/"+AUDIO_RECORDER_FOLDER+"/underwater.wav");
                 	if(!file.exists()) { 
                 		file.createNewFile();
	                    	FileInputStream fis = descriptor.createInputStream();
	                        FileOutputStream fos = new FileOutputStream(file);
	                        int c;
	
	                        while ((c = fis.read()) != -1) {
	                           fos.write(c);
	                        }
	
	                        fis.close();
	                        fos.close();
                 	}
                    */
                 	//File file = createFileFromInputStream(descriptor.createInputStream());
                     //String filepath = Environment.getExternalStorageDirectory().getPath()+"/"+AUDIO_RECORDER_FOLDER+"/underwater.wav";                        
                     //File file = new File(filepath);
                     /*
                	   * Constructs a underwater effect 
                	   * 
                	   * @param File  UwWav
                	   *          this file will be created from background
                	   * @param double normCoef
                	   *          normalization coef. the result of superposition of two tracks. If normCoef==1 normalization is not done
                	   * @param double volChangeCoef
                	   *          coefficient of variation of the volume for voice shaking.
                	   * @param int volChangeStep
                	   *          step change voice volume in milliseconds
                	   * @param float UwWavVolCoef 
                	   *          coef. for the volume change background
                	   *        
                	   */
                      uwe=new UnderWaterEffect(file,0.7,0,0,0.8f);
                      
                      /**
                	   * Constructs a low pass filter with a cutoff frequency of <code>freq</code>
                	   * that will be used to filter audio recorded at <code>sampleRate</code>.
                	   * 
                	   * @param freq
                	   *          the cutoff frequency
                	   * @param sampleRate
                	   *          the sample rate of the audio that will be filtered
                	   */
                      lpf=new LowPassFilter(1000,RECORDER_SAMPLERATE);
                     
                     break;
                 }
                 Log.i("dave","File size: " + totalDataLen);
     			
     			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
     					longSampleRate, channels, byteRate);
     			
     			while(in.read(data) != -1){
     				out.write(data);
     				ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
     				float tempSample;
              		double[] indata;
              		double[] outdata;
     				switch (effect){
              		case 0:
              			break;
              		case 1:
              			indata=new double[shorts.length];
                      	outdata=new double[shorts.length];
                      	
                      	for (int i=0;i<shorts.length;i++){
                      		indata[i]=shorts[i]/32768.0;
                      	}
              			ps.smbPitchShift(indata, outdata, 0, shorts.length);
              			for (int i=0;i<outdata.length;i++){
              				shorts[i]=(short) (outdata[i]*32768.0);
              			}
              			break;
              		case 2:
              			indata=new double[shorts.length];
                      	outdata=new double[shorts.length];
                      	
                      	for (int i=0;i<shorts.length;i++){
                      		indata[i]=shorts[i]/32768.0;
                      	}
              			ps.smbPitchShift(indata, outdata, 0, shorts.length);
              			for (int i=0;i<outdata.length;i++){
              				shorts[i]=(short) (outdata[i]*32768.0);
              			}
              			break;
              		case 3:
              			
              			for (int i=0;i<shorts.length;i++){
              				tempSample=ve.transform((float) (shorts[i]/32768.0));
              				shorts[i]=(short) (tempSample*32768);
              			}
              			break;
              		case 4:
              			
              			for(int i=0;i<shorts.length;i++){
              				//if you want to give voice shake, then you should call this method to overlay tracks
              				//shorts[i]=uwe.volumeChange(shorts[i]);
              				tempSample=lpf.transform((float) (shorts[i]/32768.0));
              				shorts[i]=(short) (tempSample*32768);   
              				try{
              				shorts[i]=uwe.addTrack(shorts[i], 0.7f);   
              				}catch(Exception e){
              					 Log.e("DAVE","ERROR ADDING TRACK"+i);
              				}
              			}
              		}
     				
     				byte[] bytes2 = new byte[shorts.length * 2];
                  	ByteBuffer.wrap(bytes2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);//shorts);                    	
                     //out.write(bytes2);//write to wav file
                     aac.encode(bytes2);//encode and write to 3gp file
                 }
                 f = new File(outFilename);
                 f.delete();
                 aac.uninit();
                 
                 in.close();
                 out.close();
         } catch (FileNotFoundException e) {
                 e.printStackTrace();
         } catch (IOException e) {
                 e.printStackTrace();
         }
 }
     private void WriteWaveFileHeader(
                 FileOutputStream out, long totalAudioLen,
                 long totalDataLen, long longSampleRate, int channels,
                 long byteRate) throws IOException {
         
         byte[] header = new byte[44];
         
         header[0] = 'R';  // RIFF/WAVE header
         header[1] = 'I';
         header[2] = 'F';
         header[3] = 'F';
         header[4] = (byte) (totalDataLen & 0xff);
         header[5] = (byte) ((totalDataLen >> 8) & 0xff);
         header[6] = (byte) ((totalDataLen >> 16) & 0xff);
         header[7] = (byte) ((totalDataLen >> 24) & 0xff);
         header[8] = 'W';
         header[9] = 'A';
         header[10] = 'V';
         header[11] = 'E';
         header[12] = 'f';  // 'fmt ' chunk
         header[13] = 'm';
         header[14] = 't';
         header[15] = ' ';
         header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
         header[17] = 0;
         header[18] = 0;
         header[19] = 0;
         header[20] = 1;  // format = 1
         header[21] = 0;
         header[22] = (byte) 1;
         header[23] = 0;
         header[24] = (byte) (longSampleRate & 0xff);
         header[25] = (byte) ((longSampleRate >> 8) & 0xff);
         header[26] = (byte) ((longSampleRate >> 16) & 0xff);
         header[27] = (byte) ((longSampleRate >> 24) & 0xff);
         header[28] = (byte) (byteRate & 0xff);
         header[29] = (byte) ((byteRate >> 8) & 0xff);
         header[30] = (byte) ((byteRate >> 16) & 0xff);
         header[31] = (byte) ((byteRate >> 24) & 0xff);
         header[32] = (byte) (2 * 16 / 8);  // block align
         header[33] = 0;
         header[34] = RECORDER_BPP;  // bits per sample
         header[35] = 0;
         header[36] = 'd';
         header[37] = 'a';
         header[38] = 't';
         header[39] = 'a';
         header[40] = (byte) (totalAudioLen & 0xff);
         header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
         header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
         header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

         out.write(header, 0, 44);
 }
}