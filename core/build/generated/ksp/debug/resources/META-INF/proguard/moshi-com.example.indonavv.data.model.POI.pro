-keepnames class com.example.indonavv.data.model.POI
-if class com.example.indonavv.data.model.POI
-keep class com.example.indonavv.data.model.POIJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
