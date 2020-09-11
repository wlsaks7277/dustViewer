package com.example.dustviewer2;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.google.firebase.database.*;

public class SettingActivity extends AppCompatActivity {
    private static final String TAG = "SettingActivity";
    /*Firebase Database*/
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference doorStatus = database.getReference("door_status");
    DatabaseReference autoMode = database.getReference("auto_mode");
    DatabaseReference dustIn = database.getReference("dust_in");
    DatabaseReference dustSet = database.getReference("dust_set");
    //DatabaseReference smartMode= database.getReference("smart_mode");
    DatabaseReference rainMode= database.getReference("rain_mode");

    int dust_in_val;
    int dust_set_val;
    String img_name = "";

    boolean edit_stat = false;
    String str="";
    int end;
    String color;

    String smart_mode_stat="";
    String auto_mode_stat="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        final String packageName = this.getPackageName();

        TextView logo = (TextView) findViewById(R.id.logo);//로고
        final TextView dust_in = (TextView) findViewById(R.id.dust_in);//실내 미세먼지 수치
        final TextView dust_set = (TextView) findViewById(R.id.dust_set);//사용자 설정 값
        final TextView dust_in_stat = (TextView) findViewById(R.id.dust_in_stat);//실내 미세먼지 상태
        final ImageView dust_in_img = (ImageView) findViewById(R.id.dust_in_img);//실내 미세먼지 상태 이미지
        final Button edit_btn = (Button) findViewById(R.id.edit_btn);//사용자 설정 값 변경 버튼
        final EditText input_dust_set = (EditText) findViewById(R.id.input_dust_set);
        final TextView auto_mode= (TextView)findViewById(R.id.auto_mode);//Auto모드 상태
        final Switch smart_mode= (Switch)findViewById(R.id.smart_mode);//스마트 모드
        final Switch rain_mode= (Switch)findViewById(R.id.rain_mode);//우천시 자동개폐 모드
        /*1. Auto 모드 확인
          2. 미세먼지 측정값, 사용자 설정값 비교
          3. door_status 변경
         */
        //실내 미세먼지 수치(아두이노에서 측정된 값)
        Typeface ubuntu = ResourcesCompat.getFont(this, R.font.ubuntu_bold);
        logo.setTypeface(ubuntu);//로고 폰트 설정

       /* smart_mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {//스마트 모드
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    smartMode.setValue("active");
                }else{
                    smartMode.setValue("inactive");
                }
            }
        });*/

        rain_mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {//우천시 모드
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    rainMode.setValue("active");
                }else{
                    rainMode.setValue("inactive");
                }
            }
        });

        dustIn.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                float value = dataSnapshot.getValue(Float.class);
                dust_in_val = Math.round(value);
                dust_in.setText(dust_in_val + "");//반올림
                if (dust_in_val >= 0 && dust_in_val <= 30) {//좋음
                    dust_in_stat.setText("좋음");
                    dust_in_stat.setTextColor(Color.parseColor("#32a1ff"));
                    img_name = "@drawable/vgood_face";
                } else if (dust_in_val >= 30 && dust_in_val <= 80) {//좋음
                    dust_in_stat.setText("보통");
                    dust_in_stat.setTextColor(Color.parseColor("#00c73c"));
                    img_name = "@drawable/good_face";
                } else if (dust_in_val >= 80 && dust_in_val <= 1500) {//좋음
                    dust_in_stat.setText("나쁨");
                    dust_in_stat.setTextColor(Color.parseColor("#fd9b5a"));
                    img_name = "@drawable/bad_face";
                } else if (dust_in_val >= 151) {//좋음
                    dust_in_stat.setText("매우나쁨");
                    dust_in_stat.setTextColor(Color.parseColor("#ff5959"));
                    img_name = "@drawable/vbad_face";
                } else {//범위를 벗어남
                    dust_in_stat.setText("ERROR");
                    dust_in_stat.setTextColor(Color.parseColor("#ff5959"));
                    img_name = "@drawable/vbad_face";
                }
                int img = getResources().getIdentifier(img_name, "drawable", packageName);
                dust_in_img.setImageResource(img);//창문 상태 이미지 변경
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Failed to read value
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });
        dust_set_val= Integer.parseInt(dust_set.getText().toString());
        dustSet.addValueEventListener(new ValueEventListener() {//사용자 설정 값
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int value = dataSnapshot.getValue(Integer.class);
                dust_set_val= value;
                Log.e(TAG,"value"+value+"dust_set_val"+dust_set_val);
                dust_set.setText(value+"");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Auto 모드
        autoMode.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                auto_mode_stat= value;
                if (value.equals("active")) {//Auto 모드 활성화 상태
                    str= "현재 Auto모드가 활성화 되어있습니다";
                    end=14;
                    color="#03cf5d";
                       if (compareDust(dust_set_val)) {//미세먼지 수치&사용자 설정 값 비교
                           Log.e(TAG,"1");
                           doorStatus.setValue("open");
                       } else {
                           doorStatus.setValue("close");
                       }
                } else {//Auto 모드 비활성화 상태
                    end= 15;
                    color="#e94d50";
                    str= "현재 Auto모드가 비활성화 되어있습니다";
                }
                //일부 문자열 색상변경
                SpannableStringBuilder ssb= new SpannableStringBuilder(str);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), 11, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ForegroundColorSpan(Color.parseColor(color)), 11, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                auto_mode.setText(ssb);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Failed to read value
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });

        //Smart 모드
       /* smartMode.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                Log.e(TAG,"smartMode>value "+value);
                smart_mode_stat= value;
                if (value.equals("active")) {//Smart 모드 활성화 상태
                    smart_mode.setChecked(true);
                } else {//Smart 모드 비활성화 상태
                    smart_mode.setChecked(false);
                }

              *//*  if(auto_mode_stat.equals("active")){
                    if(compareDust(dust_set_val)){
                        doorStatus.setValue("open");
                    }else{
                        doorStatus.setValue("close");
                    }
                }else{
                }*//*
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Failed to read value
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });*/

        //우천시 모드
        rainMode.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                if (value.equals("active")) {//Smart 모드 활성화 상태
                    rain_mode.setChecked(true);
                } else {//Smart 모드 비활성화 상태
                    rain_mode.setChecked(false);
                }

              /*  if(auto_mode_stat.equals("active")){
                    if(compareDust(dust_set_val)){
                        doorStatus.setValue("open");
                    }else{
                        doorStatus.setValue("close");
                    }
                }else{
                }*/
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Failed to read value
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });

        edit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit_stat == false) {
                    edit_btn.setText("완료");
                    dust_set.setVisibility(View.INVISIBLE);
                    input_dust_set.setVisibility(View.VISIBLE);
                    input_dust_set.setText(dust_set.getText().toString());

                    edit_stat = true;
                } else {
                    edit_btn.setText("변경");
                    dust_set.setVisibility(View.VISIBLE);
                    input_dust_set.setVisibility(View.INVISIBLE);
                    dust_set_val= Integer.parseInt(input_dust_set.getText().toString());
                    dustSet.setValue(Integer.parseInt(input_dust_set.getText().toString()));
                    if(auto_mode_stat.equals("active")){
                        if (compareDust(dust_set_val)) {//미세먼지 수치&사용자 설정 값 비교
                            Log.e(TAG,"2");
                            doorStatus.setValue("open");
                        } else {
                            doorStatus.setValue("close");
                        }
                    }else{

                    }
                    edit_stat = false;
                }
            }
        });
    }
    public boolean compareDust(int dust_set_val) {
            Log.e(TAG,"dust_in_val"+dust_in_val+"dust_set_val"+dust_set_val);
            if (dust_in_val >= dust_set_val) {//실내측정값>기준값
                return true;
            } else {//실내측정값<기준값
                return false;
            }
    }
   /* public boolean compareDust(int dust_set_val) {
        Log.e(TAG,"스마트 모드"+smart_mode_stat);
        int dustNum= ((MainActivity)MainActivity.context).dustNum;
        Log.e(TAG,"미세먼지 수치: "+dustNum);
        if(smart_mode_stat.equals("active")){
            Log.e(TAG, "AUTO모드 활성화! 스마트모드 활성화!");
            if (dust_in_val >= dust_set_val) {//실내측정값>기준값
                return true;
            } else{//실내측정값<기준값
                if(dustNum<dust_in_val){//실외<실내=>open
                    return true;
                }else{
                    return false;
                }
            }
        }else{
            Log.e(TAG, "AUTO모드 활성화! 스마트모드 비활성화!");
            if (dust_in_val >= dust_set_val) {//실내측정값>기준값
                return true;
            } else {
                return false;
            }
        }
    }*/
}
