#!/bin/bash

rm -rf app .gradle *.lock

wget -q -P ./gradle/wrapper/ "https://services.gradle.org/distributions/gradle-8.10.2-bin.zip"
echo -e "android.useAndroidX=true\nandroid.enableJetifier=true\norg.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1024m -XX:+UseG1GC\nandroid.experimental.runInProcess=true">>gradle.properties
mkdir -p app/src/main/res/{xml,mipmap-anydpi-v26,drawable}
mkdir -p app/src/main/kotlin/com/fyan
cp *.kt app/src/main/kotlin/com/fyan/

for f in .xml .zml;do
	[ -f "$f" ]||continue
	x=false;o="";p=""
	while IFS= read -r r||[ -n "$r" ];do
		if [[ "$r" =~ ^@[^@] ]];then
			[ "$x" = true ]&&[ -n "$o" ]&&[ -n "$p" ]&&{
				if [[ "$p" == */drawable/ ]];then
					while IFS= read -r l;do
						[ -z "$l" ]&&continue
						a="${l%%:*}";d="${l#*:}";[ -z "$a" ]||[ -z "$d" ]&&continue
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
				a="${l%%:*}";d="${l#*:}";[ -z "$a" ]||[ -z "$d" ]&&continue
				t="$p$a.xml";dd=$(dirname "$t");[ ! -d "$dd" ]&&mkdir -p "$dd"
				echo '<?xml version="1.0" encoding="utf-8"?><vector xmlns:android="http://schemas.android.com/apk/res/android" android:height="24dp" android:width="24dp" android:viewportWidth="24.0" android:viewportHeight="24.0"><path android:fillColor="#FFE3E3E3" android:pathData="'"$d"'"/></vector>' > "$t"
			done<<<"$o"
		else
			d=$(dirname "$p");[ ! -d "$d" ]&&mkdir -p "$d";echo "$o">"$p"
		fi
	}
done

rm -r .xml .zml .sh