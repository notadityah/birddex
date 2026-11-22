# Project-specific R8 rules: none needed so far.
#
# The libraries that rely on reflection ship their own rules inside their AARs/JARs, and R8
# applies them automatically (listed in app/build/outputs/mapping/release/configuration.txt):
# - kotlinx.serialization (kotlinx-serialization-common.pro, kotlinx-serialization-r8.pro)
#   keeps the generated serializers of every @Serializable class (API models, nav routes);
# - Retrofit 3 (retrofit2.pro) keeps the API interfaces and the generic signatures that
#   suspend functions returning Response<T> need;
# - OkHttp, Room, Hilt/Dagger, Navigation and coroutines likewise.
#
# Release (R8) builds were checked on a device for every networking and serialization
# feature (sign-in, sightings, detect, gallery, feedback, account settings, email links).
# Add a rule here only for a class R8 can't see being used, e.g. one created by reflection.
