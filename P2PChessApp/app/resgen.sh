# run inside P2PChessApp/app
ICON_URL="https://raw.githubusercontent.com/google/material-design-icons/master/android/device/devices/materialicons/black/res/drawable-xxxhdpi/baseline_devices_black_48.png"

mkdir -p app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}

# 48-dp design spec → physical pixels for each density
declare -A dp=( [mdpi]=48 [hdpi]=72 [xhdpi]=96 [xxhdpi]=144 [xxxhdpi]=192 )

for d in "${!dp[@]}"; do
  echo "$d"
  echo "${dp[$d]}x${dp[$d]}"
  echo "convert - -resize ${dp[$d]}x${dp[$d]}" "src/main/res/mipmap-$d/ic_launcher.png"
  curl -s "$ICON_URL" \
  | convert - -resize "${dp[$d]}x${dp[$d]}" \
    "src/main/res/mipmap-$d/ic_launcher.png"
  cp "src/main/res/mipmap-$d/ic_launcher.png" \
     "src/main/res/mipmap-$d/ic_launcher_round.png"
done
