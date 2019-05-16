package com.example.nfcatester;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.nfc.tech.NfcA;
import android.nfc.Tag;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import com.loopj.android.http.*;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private NfcA nfca;

    private TextView tv;
    private EditText editText;

    /*READ DATASHEET FOR INFO DATASHEET*/
    private final byte READ = (byte) 0x30;
    private final byte WRITE = (byte) 0xA2;

    private byte[] tag_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        tv = (TextView) findViewById(R.id.textView);
        tv.setText("waiting for tag\n");

        editText = (EditText) findViewById(R.id.editText);
        editText.setText("09/19/19 12:26:16");

        performTagOperations(getIntent());
    }

    /*Listen for tag*/
    @Override
    protected void onNewIntent(Intent intent) {
        performTagOperations(intent);
    }

    private void performTagOperations(Intent intent) {
        String action = intent.getAction();
        //DOCUMENTATION: https://developer.android.com/reference/android/nfc/NfcAdapter -> ACTION_TECH_DISCOVERED
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            tv.append(action);
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            tag_id = tag.getId();
            String[] techList = tag.getTechList();
            for (int i = 0; i < techList.length; i++)
                tv.append(techList[i]);

            nfca = NfcA.get(tag);
            try {
                nfca.connect();
                tv.setText("connected to tag\n");

                tv.append("tag ID: \n");
                tv.append(bytesToString(tag_id) + "\n");
            } catch (IOException e) {
                tv.append(e.toString());
            }
        }
    }

    /*button methods*/
    public void newTag(View v) throws JSONException {
        RequestParams params = new RequestParams();
        params.put("tag_id", bytesToStringNoSpace(tag_id));
        TagRestClient.client.post(TagRestClient.getAbsoluteUrl("new_tag/"), params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    byte block = (byte) 0x10;
                    byte[] data = stringToByte(response.get("pass").toString());
                    byte[] send = {WRITE, block};

                    int block_length = 4;
                    byte[] temp = new byte[send.length + block_length];
                    System.arraycopy(send, 0, temp, 0, send.length);
                    System.arraycopy(data, 0, temp, send.length, block_length);
                    tv.append(bytesToString(nfca.transceive(temp)));
                    System.out.println(bytesToString(temp));

                    //change block to avoid ovewriting on same block
                    block = (byte) 0x11;
                    send[1] = block;
                    System.arraycopy(send, 0, temp, 0, send.length);
                    System.arraycopy(data, block_length, temp, send.length, block_length);
                    tv.append(bytesToString(nfca.transceive(temp)));
                    System.out.println(bytesToString(temp));
                } catch (Exception e) {System.out.println(e);}
                tv.append(response.toString() + "\n");
            }
        });
    }

    public void addDate(View v) {
        RequestParams params = new RequestParams();
        params.put("tag_id", bytesToStringNoSpace(tag_id));
        String tag_pass = readPassword();
        params.put("tag_pass", tag_pass);
        tv.append("password read: " + addSpace(tag_pass) + "\n");
        params.put("tag_datetime", editText.getText());
        TagRestClient.client.post(TagRestClient.getAbsoluteUrl("new_date/"), params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                tv.append(response.toString() + "\n");
            }
        });
    }

    public void checkValidity(View v) {
        RequestParams params = new RequestParams();
        params.put("tag_id", bytesToStringNoSpace(tag_id));
        String tag_pass = readPassword();
        params.put("tag_pass", tag_pass);
        tv.append("password read: " + addSpace(tag_pass) + "\n");
        TagRestClient.client.post(TagRestClient.getAbsoluteUrl("get_validity/"), params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                tv.append(response.toString() + "\n");
            }
        });
    }

    public void readBlocks(View v) {
        String tag_pass = readPassword();
        tv.append("password read: " + addSpace(tag_pass) + "\n");
    }

    public void rmTag(View v) {
        RequestParams params = new RequestParams();
        params.put("tag_id", bytesToStringNoSpace(tag_id));
        String tag_pass = readPassword();
        params.put("tag_pass", tag_pass);
        tv.append("password read: " + addSpace(tag_pass) + "\n");
        TagRestClient.client.post(TagRestClient.getAbsoluteUrl("rm_tag/"), params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                tv.append(response.toString() + "\n");
            }
        });
    }

    private String readPassword() {
        byte block = (byte) 0x10;
        byte[] send = {READ, block};
        try{
            return bytesToStringNoSpace(nfca.transceive(send)).substring(0,16);
        }
        catch(IOException e){
            return "";
        }
    }

    private static String bytesToString(byte[] received) {
        return addSpace(bytesToStringNoSpace(received));
    }

    private static String bytesToStringNoSpace(byte[] received) {
        StringBuilder tempText = new StringBuilder();
        for (byte byte_received : received) {
            tempText.append(String.format("%02X", byte_received));
        }
        return tempText.toString();
    }

    private static String addSpace(String received) {
        return received.replaceAll("..", "$0 ");
    }

    private static byte[] stringToByte(String s) {
        byte data[] = new byte[s.length()/2];
        for(int i=0;i < s.length();i+=2) {
            data[i/2] = (Integer.decode("0x"+s.charAt(i)+s.charAt(i+1))).byteValue();
        }
        return data;
    }

}