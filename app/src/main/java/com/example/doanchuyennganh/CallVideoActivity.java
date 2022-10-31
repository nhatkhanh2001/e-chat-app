package com.example.doanchuyennganh;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.doanchuyennganh.adapters.AdapterPosts;
import com.example.doanchuyennganh.adapters.AdapterUsers;
import com.example.doanchuyennganh.models.ModelPosts;
import com.example.doanchuyennganh.models.ModelUser;
import com.example.doanchuyennganh.network.ApiClient;
import com.example.doanchuyennganh.network.ApiService;
import com.example.doanchuyennganh.notifications.Contants;
import com.example.doanchuyennganh.notifications.Date;
import com.example.doanchuyennganh.notifications.FirebaseMessaging;
import com.example.doanchuyennganh.notifications.Sender;
import com.example.doanchuyennganh.notifications.Token;
import com.google.android.gms.common.internal.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallVideoActivity extends AppCompatActivity {
    ImageView imageTV, videoTv;
    TextView nameTv;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference userRefdb;
    String hisUrl, hisProf, hisName, hisToken, hisUID, senderUid;
    FloatingActionButton endCallVideoTv;
    ConstraintLayout constraintL;
    private PreferenceManager preferenceManager ;
    private String inviterToken = null;
    private String mettingRoom = null;
    private String meetingType=null;



    List<ModelUser> userList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_video);

        imageTV = findViewById(R.id.imageTV);
        videoTv = findViewById(R.id.videoTv);
        endCallVideoTv = findViewById(R.id.endCallVideoTv);
        nameTv = findViewById(R.id.nameTv);
        constraintL = findViewById(R.id.constraintL);

        firebaseAuth= FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        userRefdb = firebaseDatabase.getReference("Tokens");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        senderUid = user.getUid();



        meetingType = getIntent().getStringExtra("type");


        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        Intent intent = getIntent();
        hisUID = intent.getStringExtra("uid");
        if(meetingType !=null){
            if(meetingType.equals("video")){
                Picasso.get().load(R.drawable.ic_default_img_foreground).into(imageTV);
            }
        }

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if(task.isSuccessful() && task.getResult() != null){
                    inviterToken = task.getResult().getToken();
                }
            }
        });

        Query userQuery = ref.orderByChild("uid").equalTo(hisUID);
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds: snapshot.getChildren()){
                    String name =""+ ds.child("name").getValue();
                    String hisImage =""+ ds.child("image").getValue();

                    nameTv.setText(name);
                    try{
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_img_foreground).into(imageTV);
                    }catch (Exception e){
                        Picasso.get().load(R.drawable.ic_default_img_foreground).into(imageTV);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        
        DatabaseReference tole = FirebaseDatabase.getInstance().getReference("Tokens");
        endCallVideoTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                Toast.makeText(CallVideoActivity.this, "Kết thúc cuộc gọi...", Toast.LENGTH_SHORT).show();
            }
        });

        if(meetingType !=null){
            initiateMeeting(meetingType, hisToken);
        }

    }

    private void initiateMeeting(String meetingType, String receiverToken){
        try{
            JSONArray tokes = new JSONArray();
            tokes.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Contants.REMOTE_MSG_TYPE, Contants.REMOTE_MSG_INVITATION);
            data.put(Contants.REMOTE_MSG_MEETING_TYPE, meetingType);
            data.put(Contants.REMOTE_MSG_INVITATION_TOKEN, inviterToken);

            body.put(Contants.REMOTE_MSG_DATA, data);
            body.put(Contants.REMOTE_MSG_REGISTRATION_IDS, tokes);

            sendRemoteMessage(body.toString(), Contants.REMOTE_MSG_INVITATION);
        }catch (Exception e){
            Toast.makeText(this, "..."+e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private void sendRemoteMessage(String remoteMessageBody, String type){
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
                Contants.getRemoteMessageHeader(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if(response.isSuccessful()){
                    if(type.equals(Contants.REMOTE_MSG_INVITATION)){
                        Toast.makeText( CallVideoActivity.this, "success", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(CallVideoActivity.this, "fail"+response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }

            }

            @Override
            public void onFailure(@NonNull Call<String> call,@NonNull Throwable t) {
                Toast.makeText(CallVideoActivity.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }


}