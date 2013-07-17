-injars       ../build/libs/geocoord.jar
-outjars      ../build/libs/geocoord-proguard.jar
-printmapping ../build/libs/geocoord-out.map

-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-keep public class com.geoxp.GeoXPLib {
    public protected *;
}

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-flattenpackagehierarchy

-dontwarn javax.servlet.**
-dontwarn com.vividsolutions.jts.**
-dontwarn com.fasterxml.sort.**
-dontwarn com.google.inject.**
-dontwarn com.google.inject.servlet.**
-dontwarn gnu.trove.**
-dontwarn twitter4j.**
-dontwarn com.google.gson.**
-dontwarn org.apache.http.**
-dontwarn org.apache.commons.lang.**
