

echo "[DEPLOY] Bắt đầu kịch bản deploy..."


PID=$(pgrep -f "ldx-insight-backend-.*.jar")

if [ -n "$PID" ]
then
  echo "[DEPLOY] Tìm thấy process cũ, PID: $PID. Đang dừng..."

  kill $PID 
  sleep 5 
  echo "[DEPLOY] Đã dừng process cũ."
else
  echo "[DEPLOY] Không tìm thấy process cũ. Bỏ qua bước dừng."
fi


echo "[DEPLOY] Khởi động ứng dụng Spring Boot..."
nohup java -jar ldx-insight-backend-*.jar > app.log 2>&1 &

echo "[DEPLOY] Kịch bản deploy đã hoàn tất."