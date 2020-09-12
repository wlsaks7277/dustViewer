#include <ArduinoJson.h>
#include <ESP8266WiFi.h>
#include <FirebaseArduino.h>

// Set these to run example.
#define FIREBASE_HOST "dustviewer2.firebaseio.com"
#define FIREBASE_AUTH "5Dozz94pGJuX96qRcCgXuL15ONA7g4Kc9q1tTyHr"
#define WIFI_SSID "Iam"
#define WIFI_PASSWORD "12345678"

int dustPin =A0;
float dustVal=0;
float dustDensity=0;
float dustDensityug=0;
float voMeasured =0;
float calcVoltage =0;

int ledPower =12;
int delayTime =280;
int delayTime2 =40;
float offTime =9680;

int red =16;
int green =5;
int blue =4;

void setup() {
  Serial.begin(9600);
  pinMode(red, OUTPUT);
  pinMode(green, OUTPUT);
  pinMode(blue, OUTPUT);
  
  digitalWrite(blue,255);
  
  // connect to wifi.
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("connecting");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }
  Serial.println();
  Serial.print("connected: ");
  Serial.println(WiFi.localIP());
  
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
}

int n = 0;

void loop() {

  //digitalWrite(ledPower,LOW);
  delayMicroseconds(delayTime);

  dustVal=analogRead(dustPin);
  calcVoltage = dustVal *(5.0/1024);

  delayMicroseconds(delayTime2);

//  digitalWrite(ledPower,HIGH);
  delayMicroseconds(offTime);

  delay(3000);

  dustDensityug= (0.17 * calcVoltage -0.1)*1000;
  //-----------------------------------------------------------------
  // set value
  Firebase.setFloat("dust_in", dustDensityug);
  // Firebase.database.ref('test/').setFloat({"ug/m3": dustDensityug});
  // handle error
  if (Firebase.failed()) {
      Serial.print("setting /number failed:");
      Serial.println(Firebase.error());  
      return;
  }
  delay(1000);

  String s= Firebase.getString("door_status");
  Serial.println(s);
  if(s == "close") {
    digitalWrite(blue,0);
    digitalWrite(green,0);
    delay(2000);
    digitalWrite(red,255);
  }
  else{
    digitalWrite(blue,0);
    digitalWrite(red,0);
    delay(2000);
    digitalWrite(green,255);
  }
}