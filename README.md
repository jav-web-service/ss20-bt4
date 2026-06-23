# E-Learning Platform

Hệ thống Học trực tuyến (E-Learning Platform) được thiết kế theo kiến trúc Monolithic nhằm cung cấp các khóa học kỹ năng phần mềm.
Dự án được xây dựng bằng Spring Boot, Spring Security, Spring Data JPA, và OpenFeign. CSDL sử dụng là MySQL.

## Cơ chế chặn token đã bị đăng xuất ở tầng Filter

Hệ thống áp dụng cơ chế quản lý Refresh Token và liên kết Access Token với phiên đăng nhập thông qua CSDL.
Khi đăng nhập, một `StudentToken` (Refresh Token) mới sẽ được tạo trong CSDL với `isRevoked = false`.
Đồng thời, ID của `StudentToken` này (`tokenId`) được nhúng vào trong Claims của JWT Access Token.

**Trong `JwtAuthenticationFilter`:**
1. Filter trích xuất Access Token từ Header.
2. Filter đọc `tokenId` từ Claims của Access Token.
3. Filter truy vấn database (bảng `student_tokens`) thông qua `StudentTokenRepository` để tìm token tương ứng với `tokenId` này.
4. Nếu `isRevoked` của bản ghi đó là `true`, nghĩa là phiên làm việc (hoặc toàn bộ phiên) đã bị thu hồi (đăng xuất), Filter sẽ từ chối xác thực (block access) và trả về lỗi không có quyền truy cập.

Khi người dùng gọi API **Đăng xuất**:
Hệ thống sử dụng **Java Stream API** lấy toàn bộ các `StudentToken` đang còn hiệu lực của người dùng đó từ database, duyệt qua stream và cập nhật cờ `isRevoked = true`, sau đó lưu lại.
Bằng cách này, mọi Access Token chứa `tokenId` tương ứng với các Refresh Token này đều sẽ bị chặn ngay lập tức ở tầng Filter ở những request tiếp theo.
