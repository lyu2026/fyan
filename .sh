#!/bin/bash

rm -rf app .gradle *.lock

mkdir -p app/src/main/res/{xml,mipmap-anydpi-v26,drawable,values,values-night,values-v35,values-night-v35}
mkdir -p app/src/main/kotlin/com/fyan
mv *.kt app/src/main/kotlin/com/fyan/ 2>/dev/null||true

# 将缓冲区内容写入目标路径: $1=路径, $2=内容
flush_block(){
	local dest="$1" buf="$2"
	[ -z "$dest" ]||[ -z "$buf" ]&&return
	if [[ "$dest" == */drawable/ ]];then
		while IFS= read -r line;do
			[ -z "$line" ]&&continue
			local name="${line%%:*}" data="${line#*:}"
			{ [ -z "$name" ]||[ -z "$data" ]; }&&continue
			local target="${dest}${name}.xml"
			mkdir -p "$(dirname "$target")"
			printf '<?xml version="1.0" encoding="utf-8"?><vector xmlns:android="http://schemas.android.com/apk/res/android" android:height="24dp" android:width="24dp" android:viewportWidth="24.0" android:viewportHeight="24.0"><path android:fillColor="#FFE3E3E3" android:pathData="%s"/></vector>\n' "$data" > "$target"
		done<<<"$buf"
	else
		mkdir -p "$(dirname "$dest")"
		printf '%s\n' "$buf" > "$dest"
	fi
}

for f in .xml .zml;do
	[ -f "$f" ]||continue
	active=false; buf=""; dest=""
	while IFS= read -r line||[ -n "$line" ];do
		if [[ "$line" =~ ^@[^@] ]];then
			"$active"&&flush_block "$dest" "$buf"
			dest="${line:1}"
			dest="${dest#"${dest%%[![:space:]]*}"}"
			dest="${dest%"${dest##*[![:space:]]}"}"
			active=true; buf=""
		elif "$active";then
			[ -n "$buf" ]&&buf+=$'\n'
			buf+="$line"
		fi
	done<"$f"
	"$active"&&flush_block "$dest" "$buf"
done

rm -f .xml .zml .sh

vc=$(TZ="Asia/Shanghai" date +%Y%-m%-d)
vn=$(TZ="Asia/Shanghai" date +%Y.%-m.%-d)
sed -i "s/versionCode = 0/versionCode = $vc/" app/build.gradle.kts
sed -i "s/versionName = \"0\"/versionName = \"$vn\"/" app/build.gradle.kts
