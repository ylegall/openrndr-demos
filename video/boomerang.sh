filename=$(basename -- "$1")
extension="${filename##*.}"
filename="${filename%.*}"

fps=$(ffprobe "$1" 2>&1 | grep -oP "\d+ (?=fps)")
echo "fps: $fps"
duration=$(ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "$1")
duration_int=${duration%.*}
echo "duration: $duration_int"
total_frames=$((duration_int * fps))
echo "total frames: $total_frames"

ffmpeg -i "$1" -filter_complex "[0]reverse[r];[0][r]concat,loop=0.5:$total_frames,setpts=N/$fps/TB" "$filename-boomerang.$extension"

