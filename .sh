#!/bin/bash

rm -rf app .gradle *.lock

mkdir -p app/src/main/res/{xml,mipmap-anydpi-v26,drawable,values,values-night}
mkdir -p app/src/main/kotlin/com/fyan
mv *.kt app/src/main/kotlin/com/fyan/

for f in .rz;do
 [ -f "$f" ]||continue
 x=false;o="";p=""
 while IFS= read -r r||[ -n "$r" ];do
  if [[ "$r" =~ ^@[^@] ]];then
   [ "$x" = true ]&&[ -n "$o" ]&&[ -n "$p" ]&&{
    if [[ "$p" == */drawable/ ]];then
     while IFS= read -r l;do
      [ -z "$l" ]&&continue
      a="${l%%:*}";d="${l#*:}"
      { [ -z "$a" ]||[ -z "$d" ]; }&&continue
      t="$p$a.xml";dd=$(dirname "$t");[ ! -d "$dd" ]&&mkdir -p "$dd"
      echo '<?xml version="1.0" encoding="utf-8"?><vector xmlns:android="http://schemas.android.com/apk/res/android" android:height="24dp" android:width="24dp" android:viewportWidth="24.0" android:viewportHeight="24.0"><path android:fillColor="#FFE3E3E3" android:pathData="'"$d"'"/></vector>' > "$t"
     done<<<"$o"
    else
     d=$(dirname "$p");[ ! -d "$d" ]&&mkdir -p "$d";echo "$o">"$p"
    fi;o=""
   }
   p="${r:1}";p="${p#"${p%%[![:space:]]*}"}";p="${p%"${p##*[![:space:]]}"}"
   x=true;continue
  fi
  [ "$x" = true ]&&{ [ -n "$o" ]&&o+=$'\n';o+="$r";}
 done<"$f"
 [ "$x" = true ]&&[ -n "$o" ]&&[ -n "$p" ]&&{
  if [[ "$p" == */drawable/ ]];then
   while IFS= read -r l;do
    [ -z "$l" ]&&continue
    a="${l%%:*}";d="${l#*:}"
    { [ -z "$a" ]||[ -z "$d" ]; }&&continue
    t="$p$a.xml";dd=$(dirname "$t");[ ! -d "$dd" ]&&mkdir -p "$dd"
    echo '<?xml version="1.0" encoding="utf-8"?><vector xmlns:android="http://schemas.android.com/apk/res/android" android:height="24dp" android:width="24dp" android:viewportWidth="24.0" android:viewportHeight="24.0"><path android:fillColor="#FFE3E3E3" android:pathData="'"$d"'"/></vector>' > "$t"
   done<<<"$o"
  else
   d=$(dirname "$p");[ ! -d "$d" ]&&mkdir -p "$d";echo "$o">"$p"
  fi
 }
done

rm -r .rz .sh

vc=$(TZ="Asia/Shanghai" date +%Y%m%d)
vn=$(TZ="Asia/Shanghai" date +%Y.%m.%d)
sed -i "s/versionCode=.*/versionCode=$vc/" app/build.gradle.kts
sed -i "s/versionName=\".*\"/versionName=\"$vn\"/" app/build.gradle.kts




COMPONENTS=(
 "androidx.compose:compose-bom"
 "androidx.core:core-ktx"
 "androidx.activity:activity-compose"
 "androidx.lifecycle:lifecycle-runtime-ktx"
 "androidx.navigation:navigation-compose"
 "androidx.media3:media3-ui"
 "androidx.media3:media3-exoplayer"
 "androidx.media3:media3-exoplayer-hls"
 "androidx.datastore:datastore-preferences"
 "io.coil-kt.coil3:coil-compose"
)
for component in "${COMPONENTS[@]}"; do
 group=$(echo "$component" | cut -d':' -f1)
 artifact=$(echo "$component" | cut -d':' -f2)
 group_path=$(echo "$group" | tr '.' '/')
 if [[ "$group" == androidx* ]] || [[ "$group" == com.google* ]]; then
  repo_url="https://dl.google.com/dl/android/maven2/${group_path}/${artifact}/maven-metadata.xml"
 else
  repo_url="https://repo1.maven.org/maven2/${group_path}/${artifact}/maven-metadata.xml"
 fi
 xml_data=$(curl -s "$repo_url")
 stable_version=$(echo "$xml_data" | grep -oP '<version>\K[^<]+' | grep -viE 'alpha|beta|rc|snapshot|dev|m[0-9]' | tail -n 1)
 if [ -z "$stable_version" ]; then
  echo -e "${component} -> [获取失败 / 未找到稳定版]"
 else
  echo -e "${component} -> ${stable_version}"
 fi
done
