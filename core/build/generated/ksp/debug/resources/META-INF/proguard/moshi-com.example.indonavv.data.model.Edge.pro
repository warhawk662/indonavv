-keepnames class com.example.indonavv.data.model.Edge
-if class com.example.indonavv.data.model.Edge
-keep class com.example.indonavv.data.model.EdgeJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.example.indonavv.data.model.Edge
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.example.indonavv.data.model.Edge {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,float,float,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
