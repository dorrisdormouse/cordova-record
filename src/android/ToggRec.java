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
public class ToggRec extends CordovaPlugin {
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	/*
    	//callbackContext.success("execute");
    	//Log.d("audiorec","execute");
        if (action.equals("record")) {
            //fileName = args.getString(0);
        	Log.d("audiorec","record");
        	callbackContext.error("record");
            cllBack = callbackContext;
            //this.startRecording();
            return true;
        }else if (action.equals("stoprecord")) {
        	//this.stopRecording();
        	return true;
        }
        return false;
        */
        callbackContext.success(action);
        return true;
    }
}