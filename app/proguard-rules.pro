# Retrofit reads a suspend function's response type from the generic signature of its Continuation
# parameter; R8 may remove a DTO only Retrofit uses and erase that signature. Keep the DTO classes
# (their names may still be obfuscated).
-keep,allowobfuscation class com.henriquesebastiao.prumo.core.network.dto.**
-keep,allowobfuscation class com.henriquesebastiao.prumo.feature.**.data.**Dto
