# BIDV Money Speaker – bản hợp nhất 1.1.0

Ứng dụng Android đọc thông báo giao dịch từ ứng dụng BIDV và phát giọng nói tiếng Việt qua loa điện thoại.

## Chức năng
- Đọc cả giao dịch tiền vào và tiền ra.
- Tùy chọn độc lập 4 trường: biến động số dư/số tiền giao dịch, tên người chuyển hoặc người nhận, số dư sau giao dịch, nội dung chuyển khoản.
- Điều chỉnh âm lượng đọc.
- Nút nghe thử giọng và thử đọc mẫu đủ 4 trường.
- Chống đọc trùng cùng nội dung trong vòng 90 giây.
- Chỉ xử lý thông báo từ ứng dụng có tên/gói nhận diện BIDV.
- Không yêu cầu đăng nhập BIDV, mật khẩu, PIN hoặc OTP. Dữ liệu thông báo được xử lý trên thiết bị.

## Cài đặt và sử dụng
1. Cài APK sau khi build.
2. Mở BIDV Money Speaker.
3. Chọn **Cấp quyền đọc thông báo** và bật quyền cho BIDV Money Speaker trong Android.
4. Bật **Đọc giao dịch tự động**.
5. Chọn riêng các trường muốn đọc và điều chỉnh âm lượng.
6. Nhấn **Nghe thử giọng đọc nữ**. Nếu điện thoại không có giọng nữ tiếng Việt, cài thêm dữ liệu TTS tiếng Việt trong cài đặt Text-to-Speech của Android/Samsung.
7. Gửi một giao dịch thử nhỏ để kiểm tra thông báo BIDV thực tế.

## Build APK
Yêu cầu JDK 17, Android SDK Platform 35, Gradle 8.9 và Android Gradle Plugin 8.7.3.

```bash
gradle assembleDebug
```

APK đầu ra:
`app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions đã được cấu hình để build khi push nhánh `main`/`master` hoặc chạy thủ công tại tab Actions. Artifact có tên `BIDV-Money-Speaker-debug`.

## Giới hạn cần biết
- Ứng dụng đọc nội dung Android cung cấp trong thông báo BIDV; nếu BIDV/Android ẩn nội dung hoặc không đưa tên người chuyển, số dư hay nội dung chuyển khoản vào thông báo thì app không thể tự lấy dữ liệu đó từ tài khoản ngân hàng.
- Cấu trúc thông báo BIDV có thể khác theo phiên bản ứng dụng/tài khoản. Cần kiểm thử thông báo tiền vào và tiền ra thực tế để tinh chỉnh bộ nhận diện trường cho chính xác.
- Lựa chọn giọng nữ phụ thuộc engine TTS và giọng tiếng Việt được cài trên máy; ứng dụng ưu tiên voice tiếng Việt có tên gợi ý nữ nếu engine cung cấp, nếu không dùng giọng mặc định tiếng Việt.
- Một số máy cần cho phép ứng dụng chạy nền/tắt tối ưu pin để nhận thông báo ổn định.

## Quyền riêng tư
Ứng dụng cần quyền Notification Listener để đọc thông báo. Quyền này có thể cho phép Android hiển thị nội dung thông báo từ nhiều ứng dụng, nhưng mã nguồn hiện chỉ xử lý thông báo có nguồn nhận diện BIDV. Không gửi dữ liệu lên máy chủ.
