package com.example.dustviewer2;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.firebase.database.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    long mNow;
    Date mDate;
    SimpleDateFormat mFormat= new SimpleDateFormat("yyyy-MM-dd hh:mm");
    TextView time;

    private GpsTracker gpsTracker;
    public static final int GPS_ENABLE_REQUEST_CODE= 2001;
    public static final int PERMISSIONS_REQUEST_CODE= 100;
    String[] REQUIRED_PERMISSIONS= {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    TextView location;

    String location_num="02465102";//지역코드(죽전동default)
    boolean is_gpsOk=false;

    /*Firebase Database*/
    FirebaseDatabase database= FirebaseDatabase.getInstance();
    DatabaseReference doorStatus= database.getReference("door_status");
    DatabaseReference autoMode= database.getReference("auto_mode");
    DatabaseReference dustIn = database.getReference("dust_in");
    DatabaseReference dustSet = database.getReference("dust_set");

    String door_str="";//창문 개폐여부
    String dimg_name="";//창문 이미지
    String wimg_name="@drawable/";//날씨 이미지

    int dust_in_val;
    int dust_set_val;

    //SettingActivity에 변수를 넘기기 위함
    public static Context context;
    public int dustNum;
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final String packageName= this.getPackageName();
        context= this;

        time = (TextView) findViewById(R.id.time);
        time.setText(getTime());

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        } else {
            checkRunTimePermission();
        }
        //위치
        location = (TextView) findViewById(R.id.location);
        String[] location_arr = getLocation().split(" ");
        Log.e("위치 가져오기", is_gpsOk + "");
        if (is_gpsOk) {//위치 표시가 성공한 경우
            location.setText(location_arr[1] + " " + location_arr[2] + " " + location_arr[3]);

            //위치에 따라 지역코드 설정//야매
            String loc= location_arr[4];
            Log.e("지역", loc);
            if (loc.equals("죽전1동")) location_num = "02465540";
            else if (loc.equals("보정동")) location_num = "02463118";
            else if (loc.equals("죽전2동")) location_num = "02465550";
            else if (loc.equals("죽전동")) location_num = "02465102";
            else {
                Toast.makeText(MainActivity.this, "지역 불분명!", Toast.LENGTH_LONG).show();
                location_num = "02465102";//죽전동 지역코드로 임시설정
            }
        } else {
            location.setText(getLocation());
        }
        ImageView setting= (ImageView)findViewById(R.id.setting);//설정 아이콘
        TextView logo= (TextView)findViewById(R.id.logo);//로고

        TextView dust_num = (TextView) findViewById(R.id.dust_num);//미세먼지 수치
        TextView dust_status = (TextView) findViewById(R.id.dust_status);//미세먼지 상태
        ImageView dust_img = (ImageView) findViewById(R.id.dust_img);//미세먼지 상태 이미지
        TextView vdust_num = (TextView) findViewById(R.id.vdust_num);//초미세먼지 수치
        TextView vdust_status = (TextView) findViewById(R.id.vdust_status);//초미세먼지 상태
        ImageView vdust_img = (ImageView) findViewById(R.id.vdust_img);//미세먼지 상태 이미지

        TextView weather = (TextView) findViewById(R.id.weather);//날씨
        TextView temp = (TextView) findViewById(R.id.temp); //온도
        TextView wetness = (TextView) findViewById(R.id.wetness); //습도
        ImageView weather_img= (ImageView)findViewById(R.id.weather_img);//날씨 이미지

        final Switch remote_mode = (Switch) findViewById(R.id.remote_mode);//원격 제어 스위치
        final Switch auto_mode = (Switch) findViewById(R.id.auto_mode);//오토모드 스위치
        final TextView door_status= (TextView)findViewById(R.id.door_status);//창문 상태 메세지
        final ImageView door_icon= (ImageView)findViewById(R.id.door_icon);//창문 상태 이미지

        Typeface ubuntu= ResourcesCompat.getFont(this, R.font.ubuntu_bold);
        logo.setTypeface(ubuntu);//로고 폰트 설정

        //설정
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), SettingActivity.class);//this 대신에 v.getContext로 오류(intent cannot resolve constructor)해결
                startActivity(intent);
            }
        });

        //원격제어
        remote_mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {//on일때
                        doorStatus.setValue("open"); //db값 변경(close->open)
                    } else {//off일때
                        doorStatus.setValue("close"); //db값 변경(open->close)
                    }
            }
        });

        //Auto 모드
        auto_mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){//on일때
                    autoMode.setValue("active"); //db값 변경(inactive->active)
                    remote_mode.setEnabled(false);//원격제어 hold
                }else{//off일때
                    autoMode.setValue("inactive"); //db값 변경(active->inactive)
                    remote_mode.setEnabled(true);
                }
            }
        });
        //read from the database(문상태)
        doorStatus.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value= dataSnapshot.getValue(String.class); //door status
                Log.e(TAG, value);
                if(value.equals("open")) {//열려 있을 때
                    dimg_name="@drawable/opened_window";//창문 이미지
                    door_str="창문이 열려 있습니다";//창문 상태 메세지
                    remote_mode.setChecked(true);

                }else{
                    dimg_name="@drawable/closed_window";
                    door_str="창문이 닫혀 있습니다";
                    remote_mode.setChecked(false);
                }
                door_status.setText(door_str);//창문 상태 메세지 변경
                int door_img = getResources().getIdentifier(dimg_name, "drawable", packageName);
                door_icon.setImageResource(door_img);//창문 상태 이미지 변경
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Failed to read value
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });
        //read from the database(Auto모드)
        autoMode.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String value= dataSnapshot.getValue(String.class);
                Log.e(TAG, value);
                if(value.equals("active")) {//활성화
                    auto_mode.setChecked(true);
                    if(dust_in_val>=dust_set_val){
                        doorStatus.setValue("open");
                    }else{
                        doorStatus.setValue("close");
                    }
                }else{
                    auto_mode.setChecked(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Failed to read value
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });

        WeatherConnection weatherConnection = new WeatherConnection();
        AsyncTask<String, String, String> result = weatherConnection.execute("", "");
        System.out.println("RESULT");

        OtherConnection otherConnection = new OtherConnection();
        AsyncTask<String, String, String> result2= otherConnection.execute("", "");

        try {
            String msg = result.get();
            System.out.println(msg);
            String[] data_arr = msg.toString().split("data"); //미세먼지 수치와 날씨 데이터 분리
            String[] weather_arr = data_arr[0].split("℃");
            String[] dust_arr = data_arr[1].split("㎍/㎥");//미세먼지

            //날씨, 온도, 습도
            temp.setText(weather_arr[0] + "℃");
            weather.setText(weather_arr[1]);

            if(weather_arr[1].equals("맑음")) {
                wimg_name+="sun";
            }else if(weather_arr[1].equals("흐림")){
                wimg_name+="cloud";
            }else if(weather_arr[1].equals("흐리고 비")){
                wimg_name+="rain";
            }else if(weather_arr[1].equals("구름많음")) {
                wimg_name += "lot_cloud";
            }else if(weather_arr[1].equals("비")){
                wimg_name += "rain";
            }else{
                Toast.makeText(MainActivity.this,"현재 날씨는 "+weather_arr[1],Toast.LENGTH_LONG).show();
            }
            int wimg = getResources().getIdentifier(wimg_name, "drawable", packageName);
            weather_img.setImageResource(wimg);

            //미세먼지
            String dstatus = dust_arr[1].split("\\(")[0];
            String img_name = "@drawable/";
            dust_num.setText(dust_arr[0].split(" ")[1]);
            dust_status.setText(dstatus);
            dustNum= Integer.parseInt(dust_arr[0].split(" ")[1]);
            if (dstatus.equals("좋음")) {
                img_name += "vgood_face";
                dust_status.setTextColor(Color.parseColor("#32a1ff"));
            } else if (dstatus.equals("보통")) {
                img_name += "good_face";
                dust_status.setTextColor(Color.parseColor("#00c73c"));
            } else if (dstatus.equals("나쁨")) {
                img_name += "bad_face";
                dust_status.setTextColor(Color.parseColor("#fd9b5a"));
            } else if (dstatus.equals("매우 나쁨")) {
                img_name += "vbad_face";
                dust_status.setTextColor(Color.parseColor("#ff5959"));
            }
            int dimg = getResources().getIdentifier(img_name, "drawable", packageName);
            dust_img.setImageResource(dimg);
        } catch (Exception e) {

        }

        //초미세먼지, 습도
        try {
            String msg2 = result2.get();
            System.out.println(msg2);
            String[] data2_arr= msg2.toString().split("data");
            String[] vdust_arr= data2_arr[0].split(" ");
            String[] vdust= vdust_arr[3].split("㎍/㎥");//초미세먼지


            vdust_num.setText(vdust[0]);
            vdust_status.setText(vdust[1]);
            wetness.setText(data2_arr[1]);

            String img_name="@drawable/";
            String vdstatus= vdust[1];
            if (vdstatus.equals("좋음")) {
                img_name += "vgood_face";
                vdust_status.setTextColor(Color.parseColor("#32a1ff"));
            } else if (vdstatus.equals("보통")) {
                img_name += "good_face";
                vdust_status.setTextColor(Color.parseColor("#00c73c"));
            } else if (vdstatus.equals("나쁨")) {
                img_name += "bad_face";
                vdust_status.setTextColor(Color.parseColor("#fd9b5a"));
            } else if (vdstatus.equals("매우 나쁨")) {
                img_name += "vbad_face";
                vdust_status.setTextColor(Color.parseColor("#ff5959"));
            }
            int dimg = getResources().getIdentifier(img_name, "drawable", packageName);
            vdust_img.setImageResource(dimg);
        }catch (Exception e){

        }

    }
    /*현재 시간 가져오기*/
    private String getTime(){
        mNow= System.currentTimeMillis();
        mDate= new Date(mNow);
        return mFormat.format(mDate);
    }

    /*현재 위치 가져오기*/
    private String getLocation(){
        gpsTracker = new GpsTracker(MainActivity.this);

        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();
        String address = getCurrentAddress(latitude, longitude);
        return address;
    }

    /*
   * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
   */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if ( check_result ) {

                //위치 값을 가져올 수 있음
                ;
            }
            else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();


                }else {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    void checkRunTimePermission(){

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음



        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }


    public String getCurrentAddress(double latitude, double longitude) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
            is_gpsOk=true;
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }



        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString()+"\n";

    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    // 네트워크 작업은 AsyncTask 를 사용해야 한다
    public class WeatherConnection extends AsyncTask<String, String, String>{

        // 백그라운드에서 작업하게 한다
        @Override
        protected String doInBackground(String... params) {

            String path = "http://weather.naver.com/rgn/townWetr.nhn?naverRgnCd="+location_num;
            // Jsoup을 이용한 날씨데이터 Pasing하기.
            try{
                Document document = Jsoup.connect(path).get();
                String text= getParsing(document, "em",2)+"data"+getParsing(document, "dt",0);
              /*  Document document = Jsoup.connect(path).get();

                Elements elements = document.select("em");
                Elements dust= document.select("dt");
                System.out.println("미세먼지"+dust);
                System.out.println(elements);

                Element targetElement = elements.get(2);

                String text = targetElement.text();

                System.out.println(text);
            */
                return text;
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

    }
    public String getParsing(Document document, String query, int point){


        Elements elements = document.select(query);
        System.out.println(elements);

        Element targetElement = elements.get(point);

        String text = targetElement.text();

        System.out.println(text);

        return text;
    }


    //초미세먼지, 습도
    // 네트워크 작업은 AsyncTask 를 사용해야 한다
    public class OtherConnection extends AsyncTask<String, String, String>{

        // 백그라운드에서 작업하게 한다
        @Override
        protected String doInBackground(String... params) {

            String path = "https://search.naver.com/search.naver?sm=top_hty&fbm=0&ie=utf8&query=%EC%8A%B5%EB%8F%84";
            // Jsoup을 이용한 날씨데이터 Pasing하기.
            try{
                Document document = Jsoup.connect(path).get();
                String text= getParsing(document, "dl[class=indicator]",0)+"data"+getParsing(document, "div[class=info_list humidity _tabContent] ul li[class=on] dl dd[class=weather_item _dotWrapper]",0);

                return text;
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

    }
}
